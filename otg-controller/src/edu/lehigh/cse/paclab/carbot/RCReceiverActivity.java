package edu.lehigh.cse.paclab.carbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * This is the "robot" half of the remote-control station. We launch a wifi listener here, then take commands over wifi
 * and turn them into DTMF signals.
 * 
 * TODO: The menu for this activity is not ideal, since only one of the three buttons matters
 */
public class RCReceiverActivity extends BasicBotActivityBeta
{
    /**
     * This socket is for listening for new connections
     */
    private ServerSocket serverSocket;

    /**
     * We encapsulate all the camera behavior in a CameraMechanism object
     */
    CameraMechanism      cm = new CameraMechanism();

    /**
     * On activity creation, we just inflate a layout, announce the IP, and initialize the camera mechanism
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcreceiver);
        cm.onCreateCamera(this);
        TextView tv = (TextView) findViewById(R.id.tvIP);
        tv.setText(getLocalIpAddress());
        tv.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                startListening();
            }
        });
    }

    /**
     * Lifecycle method to run when stopping the activity. If we made a server socket, we need to close it,
     */
    @Override
    protected void onStop()
    {
        super.onStop();
        try {
            // close the server socket if we have one...
            if (serverSocket != null)
                serverSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lifecycle method to resurrect the camera on resume
     */
    @Override
    public void onResume()
    {
        super.onResume();
        cm.onResumeCamera();
    }

    /**
     * Lifecycle method to give up the camera on pause
     */
    @Override
    public void onPause()
    {
        cm.onPauseCamera();
        super.onPause();
    }

    /**
     * Whenever we receive a string over the network, we pass it here to control the robot
     * 
     * @param s
     *            The string that was received over the network
     */
    void parseMsg(String s)
    {
        if (s.equals("FWD"))
            myRobotForward();
        if (s.equals("REV"))
            myRobotReverse();
        if (s.equals("PTL"))
            myRobotPointTurnLeft();
        if (s.equals("PTR"))
            myRobotPointTurnRight();
        if (s.equals("CW"))
            myRobotClockwise();
        if (s.equals("CCW"))
            myRobotCounterClockwise();
        if (s.equals("STOP"))
            myRobotStop();
        if (s.equals("SNAP"))
            cm.snap();
    }

    /**
     * Method to create a Thread that listens for connections -- I.e., to make this phone the 'server' side of the
     * socket. Note that both sides participate in the communication.
     */
    void startListening()
    {
        Thread fst = new Thread(new ServerThread());
        fst.start();
    }

    /**
     * The server half of the communication protocol. Hopefully abstracting it into a separate function makes this code
     * easier to work with...
     * 
     * @param client
     *            The open socket that the server uses to communicate with the client
     * 
     * @return TODO: For now, just return true, which causes the caller to break out of its listening routine... we
     *         could handle this better...
     */
    boolean serverProtocol(Socket client)
    {
        try {
            // get streams for reading and writing to the socket...
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // send the output stream to the camera, so it can use it to transmit a photo
            cm.myWriter = client.getOutputStream();
            // server protocol: as long as there is data to read, read it and process it
            String line = null;
            while ((line = in.readLine()) != null) {
                parseMsg(line);
            }
        }
        catch (Exception e) {
            longbread("Error while receiving: " + e);
            e.printStackTrace();
        }
        return true;
    }

    /**
     * The ServerThread Runnable Object is used to create a Thread for managing the server side of the communication
     */
    private class ServerThread implements Runnable
    {
        public void run()
        {
            try {
                // make a socket
                serverSocket = new ServerSocket(WIFICONTROLPORT);
                shortbread("created a socket");
                // listen for new connections
                while (true) {
                    // When we get a connection, run the server protocol
                    Socket client = serverSocket.accept();
                    shortbread("Received a connection!");

                    // run the server protocol, possibly break out of the loop instead of listening for a new connection
                    if (serverProtocol(client))
                        break;
                    // TODO: update protocol to sometimes not return true?
                }
            }
            catch (Exception e) {
                shortbread("Error during I/O: " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Provide an empty callback method, so that we are compatible with AlarmCallbackReceiver
     */
    public void callback()
    {
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver)
    {
        sDriver = driver;
        final Intent intent = new Intent(context, RCReceiverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }
}