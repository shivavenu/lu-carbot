package edu.lehigh.cse.paclab.carbot;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * This is the "controller" half of the remote-control station. We connect this activity to a wifi activity on the
 * robot, and then the phone running this activity can send commands to the robot.
 */
public class RCSenderActivity extends BasicBotActivityBeta
{
    /**
     * Track if we are connected
     */
    private boolean                    connected = false;

    /**
     * This provides a means of communicating to the network thread in a manner that avoids spinning
     */
    private ArrayBlockingQueue<String> queue     = new ArrayBlockingQueue<String>(20);

    /**
     * Reference to the ImageView where we display pictures that we receive over the net
     */
    ImageView                          myIV;

    /**
     * A handler for updating myIV from another thread
     */
    Handler                            myHandler;

    /**
     * On activity creation, we just inflate a layout, set the handler, and find the ImageView
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcsender);
        myIV = (ImageView) findViewById(R.id.ivSHOW);
        myHandler = new Handler();
    }

    /**
     * Whenever one of the buttons is pressed, we send the appropriate request over the network by putting a string in
     * the queue that the network thread is blocked on
     * 
     * @param v
     *            A reference to the button that was pressed
     */
    public void onClickImage(View v)
    {
        try {
            if (v == findViewById(R.id.cmdFWD))
                queue.put("FWD");
            if (v == findViewById(R.id.cmdREV))
                queue.put("REV");
            if (v == findViewById(R.id.cmdPTL))
                queue.put("PTL");
            if (v == findViewById(R.id.cmdPTR))
                queue.put("PTR");
            if (v == findViewById(R.id.cmdCW))
                queue.put("CW");
            if (v == findViewById(R.id.cmdCCW))
                queue.put("CCW");
            if (v == findViewById(R.id.cmdSTOP))
                queue.put("STOP");
            if (v == findViewById(R.id.cmdSNAP))
                queue.put("SNAP");
            if (v == findViewById(R.id.cmdCNCT))
                initiateConnection();
        }
        catch (InterruptedException ie) {
            // swallow the exception for now...
            longbread("Error while enqueueing: " + ie);
        }
    }

    /**
     * The client half of the communication protocol. Whenever a string arrives in the queue, we just send it and we're
     * done.
     * 
     * @param socket
     *            The open socket that the client uses to communicate with the server
     */
    void clientProtocol(Socket socket)
    {
        try {
            // set up stream for writing on the socket
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                    true);
            // main loop
            while (connected) {
                // get something from the queue, blocking if there is nothing to get
                String msg = queue.take();
                // send the message we received to the server
                out.println(msg);
                // if we sent a SNAP, wait for a reply and display the result
                if (msg.equals("SNAP")) {
                    displayIt(socket);
                }
            }
        }
        // if something goes wrong, put up some toast... note that this will drop us out of the loop
        catch (Exception e) {
            longbread("Error while sending: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Helper routine for getting a picture and displaying it
     * 
     * @param socket
     *            The socket on which the picture arrives
     */
    void displayIt(final Socket socket)
    {
        try {
            // read the data, turn it into a bitmap, and display it
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int len = dis.readInt();
            final byte[] jpeg = new byte[len];
            dis.readFully(jpeg);
            // [mfs] TODO: make this an asynctask?
            // Be sure to run GUI updates on the main thread, not on the server thread
            myHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    myIV.setImageBitmap(BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length));
                }
            });
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When the client user clicks 'connect', we create a Dialog to ask for the IP address of the server, and if we get
     * a valid response, we initiate a connection request.
     */
    private void initiateConnection()
    {
        if (connected)
            return;
        // create a dialog consisting of an EditText and two buttons
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Connect to Server");
        alert.setMessage("Enter server IP address");
        final EditText input = new EditText(this);
        // pre-fill with our address, since the prefix should be the same
        input.setText(getLocalIpAddress());
        alert.setView(input);

        // on 'OK', take the next step in starting a connection
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // if we're not connected, get the IP, make a connection thread, and go for it...
                if (!connected) {
                    String s = input.getEditableText().toString();
                    if (!s.equals("")) {
                        shortbread("Attempting to connect to " + s);
                        Thread cThread = new Thread(new ClientThread(s));
                        cThread.start();
                    }
                }
            }
        });
        // if the user clicks 'cancel', do nothing...
        alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }

    /**
     * The ClientThread Runnable Object is used to create a Thread for managing the client side of the communication
     */
    public class ClientThread implements Runnable
    {
        /**
         * Track the server IP address
         */
        String serverIpAddress;

        /**
         * Simple constructor
         * 
         * @param s
         *            the server ip address
         */
        ClientThread(String s)
        {
            serverIpAddress = s;
        }

        /**
         * The main routine is just to make a connection and then run the client protocol
         */
        public void run()
        {
            try {
                // attempt to connect to the server
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
                Socket socket = new Socket(serverAddr, WIFICONTROLPORT);
                connected = true;
                shortbread("Connected!");
                // run the client protocol, then close the socket
                clientProtocol(socket);
                socket.close();
                shortbread("connection closed");
            }
            catch (Exception e) {
                shortbread("Error during I/O: " + e);
                e.printStackTrace();
                connected = false;
            }
        }
    }

    /**
     * Provide an empty callback method, so that we are compatible with AlarmCallbackReceiver
     */
    public void callback()
    {
    }
}
