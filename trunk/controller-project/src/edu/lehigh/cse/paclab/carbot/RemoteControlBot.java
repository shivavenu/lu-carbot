package edu.lehigh.cse.paclab.carbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.support.BTService;
import edu.lehigh.cse.paclab.carbot.support.BasicBotActivity;
import edu.lehigh.cse.paclab.carbot.support.SnapPhoto;

/**
 * An activity for controlling a robot remotely
 * 
 * This is almost there. Remaining tasks:
 * 
 * 1 - hardening: you need to not rotate the phone, and connect bluetooth before
 * connecting usb
 * 
 * 2 - auto-snap picture: right now, the robot phone must be touched a few times
 * 
 * 3 - Orthogonality: right now both sides of the system use this activity,
 * instead of the botphone using something simpler
 * 
 */
public class RemoteControlBot extends BasicBotActivity
{
    TextView tvStatus;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remotecontrolbot);
        initBTStatus();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.i("CARBOT", "in OAR");
        switch (requestCode) {
            case INTENT_SNAP_PHOTO:
                Log.i("CARBOT", "in OAR-2");
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("CARBOT", "in OAR-3");
                    sendBigMessage();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // now we shall try to set up a 2-stage communication
    // snd -1: size
    // rcv -1: send ack
    // snd i: byte[512*i]...upto 512 more bytes
    // rcv i: send ack

    // the round that we are on
    int sendIter = -1;

    // are we sending or receiving?
    boolean sending = false;

    // the data
    byte data[];

    // the size of the data being sent
    int sendSize;

    // the message, if we are sending a short message
    String shortmessage = "";

    // send a simple message
    void sendCMD(String msg)
    {
        // Check that we're actually connected before trying anything
        if (btService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "Error: No connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        sending = true;
        sendIter = -1;
        shortmessage = msg;
        sendSize = -1;
        data = null;
        sendMessage();
    }

    protected void sendBigMessage()
    {
        Log.i("CARBOT", "Sending big message");
        // Check that we're actually connected before trying anything
        if (btService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "Error: No connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        // move FSM to sending mode
        sending = true;
        sendIter = -1;
        shortmessage = "";

        // get the data to send (NB: this will change)
        // 1 - find the file
        File fImage = SnapPhoto.getOutputMediaFile();
        // 2 - get its length, make a buffer
        sendSize = (int) fImage.length();
        Log.i("CARBOT", "File Size is " + sendSize);
        data = new byte[sendSize];
        // 3 - get the data
        try {
            FileInputStream fis = new FileInputStream(fImage);
            fis.read(data);
            fis.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            sending = false;
            return;
        }
        // start the send/receive handshaking
        sendMessage();
    }

    /**
     * Sends a message.
     */
    protected void sendMessage()
    {
        // case 1: we are sending a non-data message
        if (sending == true && data == null) {
            Log.i("CARBOT", "sending " + shortmessage);
            // send the actual message as a string
            btService.write(shortmessage.getBytes());
            return;
        }
        // case 2: we are sending the first chunk of a data message
        if (sending == true && sendIter == -1) {
            Log.i("CARBOT", "sending startbig size = " + sendSize);
            // send the size of the message as a string, advance to next
            btService.write(("" + sendSize).getBytes());
            sendIter++;
            return;
        }
        // case 3: we are sending the 'sendIter'th chunk of a data message
        if (sending == true) {
            // figure out which chunk to send (no more than 512 bytes!)
            int remain = sendSize - 512 * sendIter;
            int min = remain > 512 ? 512 : remain;
            // send bytes via offset/size variant of write command, advance to
            // next
            Log.i("CARBOT", "sending " + min + " bytes");
            btService.write(data, 512 * sendIter, min);
            sendIter++;
            return;
        }
    }

    void ack()
    {
        // Send an ack
        Log.i("CARBOT", "sending ack");
        btService.write("ACK".getBytes());
    }

    void sendDone()
    {
        sending = false;
        sendIter = -1;
        shortmessage = "";
        sendSize = -1;
        data = null;
    }

    /**
     * Receive a message
     */
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
        // case 1: we are the sender of a non-data message... this is an ACK
        if (sending == true && data == null) {
            // ignore the message, as it must be an ACK
            Log.i("CARBOT", "case 1 received " + new String(readBuf, 0, bytes));
            // the communication is done
            sendDone();
            return;
        }
        // case 2: we are the sender of a data message... this is an ACK
        if (sending == true && data != null) {
            // ignore the message, as it must be an ACK
            Log.i("CARBOT", "case 2 received " + new String(readBuf, 0, bytes));

            // do we need to send more data?
            if (sendIter * 512 >= sendSize) {
                sendDone();
            }
            else {
                sendMessage();
            }
            return;
        }
        // case 3: we are the receiver of a new message
        if (sendIter == -1) {
            String msg = new String(readBuf, 0, bytes);
            Log.i("CARBOT", "RECEIVED:::" + msg);
            TextView tv = (TextView) findViewById(R.id.tvRemoteControlBotMessage);
            // check for known non-int messages
            if (msg.equals("FWD")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Forward");
                ack();
                robotForward();
                sendDone();
                return;
            }
            if (msg.equals("REV")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Reverse");
                ack();
                robotReverse();
                sendDone();
                return;
            }
            if (msg.equals("STOP")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Stop");
                ack();
                robotStop();
                sendDone();
                return;
            }
            if (msg.equals("PTR")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Right");
                ack();
                robotStop();
                robotPointTurnRight();
                sendDone();
                return;
            }
            if (msg.equals("PTL")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Left");
                ack();
                robotStop();
                robotPointTurnLeft();
                sendDone();
                return;
            }
            if (msg.equals("ROT+")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Clockwise");
                ack();
                robotClockwise();
                sendDone();
                return;
            }
            if (msg.equals("ROT-")) {
                // it's forward: update the TV, send an ACK
                tv.setText("Counterclockwise");
                ack();
                robotCounterclockwise();
                sendDone();
                return;
            }
            if (msg.equals("SNAP")) {
                ack();
                sendDone();
                // time to take a photo...
                Log.i("CARBOT", "Starting intent to take picture");
                Intent i = new Intent(this, SnapPhoto.class);
                startActivityForResult(i, INTENT_SNAP_PHOTO);
                return;
            }
            // other known messages would be handled here, or better yet, have a
            // function handle them!

            // ...

            // if we are here, then we think we've been sent an Integer, which
            // means we are starting a new data communication
            try {
                // total size can be determined by the payload of this
                // message
                sendSize = Integer.parseInt(new String(readBuf, 0, bytes));
                // get room for the data, send an ack
                data = new byte[sendSize];
                ack();
                // advance to next state
                sendIter++;
            }
            catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            return;
        }
        // case 4: we are receiving the 'sendIter'th packet of data

        // figure out how much data is in this packet
        int remain = sendSize - 512 * sendIter;
        int min = remain > 512 ? 512 : remain;

        // figure out offset where data will go
        int start = 512 * sendIter;

        // copy data into buffer
        for (int i = 0; i < min; ++i)
            data[start + i] = readBuf[i];

        // advance to next state
        sendIter++;

        // ack the message
        ack();

        // are we all done?
        if (sendIter * 512 >= sendSize) {
            // clear the counter
            sendIter = -1;

            // create a file and dump the byte stream into it (hard-code for
            // Droid I)
            File fImage = SnapPhoto.getOutputMediaFile();

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(fImage);
                fos.write(data, 0, sendSize);
                fos.close();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
            // if that worked, then update the imageView
            ImageView iv = null; // (ImageView) findViewById(R.id.ivBTRCImage);
            iv.setImageURI(null);
            iv.invalidate();
            iv.setImageURI(Uri.fromFile(fImage));
            iv.invalidate();
        }
    }

}