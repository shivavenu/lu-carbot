package edu.lehigh.cse.paclab.carbot.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import edu.lehigh.cse.paclab.carbot.R;

/**
 * A simple demonstration of socket-based communication on Android
 * 
 * The behavior of this app is simple: we run it on two phones that are on the same network. On one phone, use the menu
 * to indicate that it should 'listen' for connections -- this makes that phone the 'server'. On the other phone, use
 * the menu to 'connect', and provide the 'server' IP address -- this makes the second phone the 'client'. Once the
 * connection is established, type text in the client and it will appear on the server screen, and be send back to the
 * client, where it appears on the client screen as well.
 * 
 * It would be trivial to turn this into a 'chat' program, but that's not our goal. In fact, having the EditText and
 * Button on the server might even be confusing, but we have them solely for the sake of keeping this app as simple as
 * possible (e.g., one Activity).
 * 
 * Since this Activity demonstrates bidirectional socket-based communication over WiFi, it should be trivial to extend
 * it to our needs in CarBot.
 * 
 * References:
 * 
 * http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
 */
public class WifiTest extends Activity
{
    /**
     * Indicate the port this app uses
     */
    static final int                   WIFIPORT        = 9599;

    /**
     * It's useful to have a reference to the 'role' TextView that the client and server update
     */
    TextView                           tvRole;

    /**
     * Since we have threads, and those threads may need to post something back to the UI, we need a handler:
     */
    private Handler                    handler         = new Handler();

    /**
     * When we're in Server mode, this socket is for listening for new connections
     */
    private ServerSocket               serverSocket;

    /**
     * When we're in Client mode, this is the IP of the server
     */
    private String                     serverIpAddress = "";

    /**
     * When we're in Client mode, track if we have a connection
     */
    private boolean                    connected       = false;

    /**
     * This is the means of communicating between the gui thread and the client network thread, so that the network
     * thread doesn't have to spin.
     */
    private ArrayBlockingQueue<String> queue           = new ArrayBlockingQueue<String>(20);

    /**
     * Standard method for creating an activity. We update a few TextViews to report this phone's IP address and
     * indicate it isn't in 'client' or 'server' mode yet
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifitest);
        // set the IP textview to this phone's IP address
        ((TextView) findViewById(R.id.tvIP)).setText(getLocalIpAddress());

        // initialize the tvRole status field
        tvRole = (TextView) findViewById(R.id.tvRole);
    }

    /**
     * Mandatory method for setting up the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.listen_connect, menu);
        return true;
    }

    /**
     * Dispatch method for dealing with menu events
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // we just dispatch to a helper method, so that this code stays readable
        switch (item.getItemId()) {
            case R.id.menu_listen:
                startListening();
                return true;
            case R.id.menu_connect:
                initiateConnection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
     * Method to create a Thread that listens for connections -- I.e., to make this phone the 'server' side of the
     * socket. Note that both sides participate in the communication.
     */
    void startListening()
    {
        // make a server thread, and start it
        Thread fst = new Thread(new ServerThread());
        fst.start();
        tvRole.setText("Mode: Server::Listening");
    }

    /**
     * Wrapper to simplify posting messages from a network thread back to the UI thread via our Handler
     * 
     * @param message
     *            The message to display
     */
    void hdlrMsg(final String message)
    {
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                tvRole.setText(message);
            }
        });
    }

    /**
     * The server half of the communication protocol. Hopefully abstracting it into a separate function makes this code
     * easier to work with...
     * 
     * @param client
     *            The open socket that the server uses to communicate with the client
     * @return For now, just return true, which causes the caller to break out of its listening routine... we could
     *         handle this better...
     */
    boolean serverProtocol(Socket client)
    {
        try {
            // get streams for reading and writing to the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),
                    true);
            // server protocol: as long as there is data to read, read it and send it back with a prefix attached
            String line = null;
            while ((line = in.readLine()) != null) {
                // display the message locally
                hdlrMsg(line);
                // send the message back
                out.println("received " + line);
                // NB: we'd need to use handler if we wanted to do anything fancy here...
                // handler.post(new Runnable() { @Override public void run() { } });
            }
        }
        catch (Exception e) {
            hdlrMsg("Mode: Server::Error reset required");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * The client half of the communication protocol. Hopefully abstracting it into a separate function makes this code
     * easier to work with...
     * 
     * @param socket
     *            The open socket that the client uses to communicate with the server
     */
    void clientProtocol(Socket socket)
    {
        try {
            // set up streams for reading/writing on the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                    true);
            // main loop
            while (connected) {
                // get something from the queue, blocking if there is nothing to get
                String msg = queue.take();

                // [mfs] could have a way to break out of the loop here if the message is 'exitnow' or something like
                // that

                // send the message we received to the server
                out.println(msg);
                // wait for a reply from the server, then display it
                String s = in.readLine();
                hdlrMsg(s);
            }
        }
        catch (Exception e) {
            Log.e("ClientActivity", "S: Error", e);
        }
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
                serverSocket = new ServerSocket(WIFIPORT);
                // listen for new connections
                while (true) {
                    // When we get a connection, update the UI
                    Socket client = serverSocket.accept();
                    hdlrMsg("Mode: Server::Connected");

                    // run the server protocol, possibly break out of the loop instead of listening for a new connection
                    if (serverProtocol(client))
                        break;
                    // [mfs] TODO: update protocol to sometimes not return true?
                }
            }
            catch (Exception e) {
                hdlrMsg("Mode: Server::Error");
                e.printStackTrace();
            }
        }
    }

    /**
     * When the client user clicks 'connect', we create a Dialog to ask for the IP address of the server, and if we get
     * a valid response, we initiate a connection request.
     */
    private void initiateConnection()
    {
        // create a dialog consisting of an EditText and two buttons
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Connect to Server");
        alert.setMessage("Enter server IP address");
        final EditText input = new EditText(this);
        alert.setView(input);

        // on 'OK', take the next step in starting a connection
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // if we're not connected, get the IP, make a connection thread, and go for it...
                if (!connected) {
                    String s = input.getEditableText().toString();
                    serverIpAddress = s;
                    if (!serverIpAddress.equals("")) {
                        hdlrMsg("Connecting to " + s);
                        Thread cThread = new Thread(new ClientThread());
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
     * This method determines the phone's IP addresses
     * 
     * @return A string representation of the phone's IP addresses
     */
    static String getLocalIpAddress()
    {
        String ans = "";
        try {
            // get all network interfaces, and create a string of all their addresses
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                Enumeration<InetAddress> addrList = ni.getInetAddresses();
                while (addrList.hasMoreElements()) {
                    InetAddress addr = addrList.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        ans += addr.getHostAddress().toString() + ";";
                    }
                }
            }
        }
        catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return ans;
    }

    /**
     * The ClientThread Runnable Object is used to create a Thread for managing the client side of the communication
     */
    public class ClientThread implements Runnable
    {
        public void run()
        {
            try {
                // attempt to connect to the server
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
                Socket socket = new Socket(serverAddr, WIFIPORT);
                connected = true;
                hdlrMsg("Connected");
                // run the client protocol, then close the socket
                clientProtocol(socket);
                socket.close();
                Log.d("ClientActivity", "C: Closed.");
            }
            catch (Exception e) {
                Log.e("ClientActivity", "C: Error", e);
                connected = false;
            }
        }
    }

    /**
     * This is a UI-related method. When we click the 'send' button, this runs. Its behavior is to grab the text in
     * etInput and dump it into the queue so that the network thread can use it
     * 
     * @param v
     *            A reference to the button that was clicked
     */
    public void sendMsg(View v)
    {
        String msg = ((EditText) findViewById(R.id.etInput)).getEditableText().toString();
        try {
            queue.put(msg);
        }
        catch (InterruptedException ie) {
            // swallow the exception for now...
            Log.e("ClientActivity", "S: Error", ie);
        }
    }
}
