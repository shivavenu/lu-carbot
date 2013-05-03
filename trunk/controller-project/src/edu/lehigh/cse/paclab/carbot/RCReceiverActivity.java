package edu.lehigh.cse.paclab.carbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * This is the "robot" half of the remote-control station. We launch a wifi listener here, then take commands over wifi
 * and turn them into DTMF signals.
 * 
 * [TODO] Snap Photo is incomplete. Currently we have support for the receiver to take pictures, but it does so through
 * manual intervention instead of via remote control
 * 
 * The layout for this activity is not useful right now. Longer-term, we are going to need to have a camera on the
 * screen for taking pictures and sending them back to the remote controller, which might motivate having the IP address
 * on screen, too
 */
public class RCReceiverActivity extends BasicBotActivityBeta
{
    /**
     * This socket is for listening for new connections
     */
    private ServerSocket serverSocket;

    CameraMechanism      cm = new CameraMechanism();

    /**
     * On activity creation, we just inflate a layout and initialize the camera mechanism
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcreceiver);
        cm.onCreateCamera(this, findViewById(R.id.captureFront));
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
                Toast.makeText(this, "You should run that on the remote control", Toast.LENGTH_LONG).show();
                return true;
            case R.id.menu_report:
                Toast.makeText(this, getLocalIpAddress(), Toast.LENGTH_LONG).show();
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
     * Whenever we receive a string over the network, we pass it here to control the robot
     * 
     * @param s
     *            The string that was received over the network
     */
    void parseMsg(String s)
    {
        if (s.equals("FWD"))
            robotForward();
        if (s.equals("REV"))
            robotReverse();
        if (s.equals("PTL"))
            robotPointTurnLeft();
        if (s.equals("PTR"))
            robotPointTurnRight();
        if (s.equals("CW"))
            robotClockwise();
        if (s.equals("CCW"))
            robotCounterClockwise();
        if (s.equals("STOP"))
            robotStop();
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
     * @return For now, just return true, which causes the caller to break out of its listening routine... we could
     *         handle this better...
     */
    boolean serverProtocol(Socket client)
    {
        try {
            // get streams for reading and writing to the socket... the write direction is not yet implemented...
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),
                    true);
            // server protocol: as long as there is data to read, read it and send it back with a prefix attached
            String line = null;
            while ((line = in.readLine()) != null) {
                // display the message locally
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
                // listen for new connections
                while (true) {
                    // When we get a connection, update the UI
                    Socket client = serverSocket.accept();
                    shortbread("Received a connection!");

                    // run the server protocol, possibly break out of the loop instead of listening for a new connection
                    if (serverProtocol(client))
                        break;
                    // [mfs] TODO: update protocol to sometimes not return true?
                }
            }
            catch (Exception e) {
                shortbread("Error during I/O: " + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        cm.onResumeCamera();
    }

    @Override
    public void onPause()
    {
        cm.onPauseCamera();
        super.onPause();
    }
}
