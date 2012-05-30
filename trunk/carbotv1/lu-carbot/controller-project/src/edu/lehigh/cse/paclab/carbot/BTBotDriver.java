package edu.lehigh.cse.paclab.carbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.services.BTService;
import edu.lehigh.cse.paclab.carbot.services.BluetoothManager;

/**
 * An activity for controlling a robot based on commands received from BlueTooth
 * 
 * This is designed for use with BTRemoteControl, but the communication is
 * neutral, so anything really could do the job. When a message is received over
 * Bluetooth, it is parsed, and the corresponding command is sent to the Arduino
 * to control the robot's motion.
 */
public class BTBotDriver extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // declare desire for a custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        // inflate the layout
        setContentView(R.layout.btzaplayout);
        // attach the custom title to our "title" layout
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.bttitle);
        // set the text for the RHS
        TextView tv = (TextView) findViewById(R.id.tvBtTitleRight);
        tv.setText("Not Connected");
    }

    /**
     * if the activity suspends, then on resume we need to start the chat
     * service
     */
    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (btService.getState() == BTService.STATE_NONE) {
                // Start the Bluetooth chat services
                btService.start();
            }
        }
    }

    /** stop the service when we're done */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (btService != null)
            btService.stop();
    }

    private BTService btService = null;

    @Override
    public void onStart()
    {
        super.onStart();
        if (!BluetoothManager.getBtAdapter().isEnabled()) {
            Intent turniton = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turniton, CarbotApplication.INTENT_BT_TURNON);
        }
        else {
            if (btService == null)
                setupChat();
        }
    }

    /** initialize the adapters for chatting */
    private void setupChat()
    {
        // Initialize the send button with a listener that for click events
        Button mSendButton = (Button) findViewById(R.id.btnBTZap);
        mSendButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                sendBigMessage();
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        btService = new BTService(this, mHandler);
    }

    /** Draw our menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.remotecontrolmenu, menu);
        return true;
    }

    /** This runs when a menu item is clicked */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menuBTDiscoverable:
                BluetoothManager.setDiscoverable();
                return true;
            case R.id.menuBTFindDevice:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, BTFindDeviceActivity.class);
                startActivityForResult(serverIntent, CarbotApplication.INTENT_BT_CONNECT);
                return true;
        }
        return false;
    }

    // now we shall try to set up a 2-stage communication
    // snd -1: size
    // rcv -1: send ack
    // snd i: byte[512*i]...upto 512 more bytes
    // rcv i: send ack

    // the size of the data being sent
    int sendSize;

    // the round that we are on
    int sendIter = -1;

    // are we sending or receiving?
    boolean sending = false;

    // the data
    byte data[];

    private void sendBigMessage()
    {
        // Check that we're actually connected before trying anything
        if (btService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "Error: No connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        // move FSM to sending mode
        sending = true;
        sendIter = -1;

        // get the data to send (NB: this will change)
        // 1 - find the file
        File fSDCard = new File("/sdcard");
        File fImage = new File(fSDCard.toString() + "/image.jpg");
        // 2 - get its length, make a buffer
        sendSize = (int) fImage.length();
        data = new byte[sendSize];
        // 3 - get the data
        try {
            FileInputStream fis = new FileInputStream(fImage);
            fis.read(data);
            fis.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            sending = false;
            return;
        }
        catch (IOException e1) {
            e1.printStackTrace();
            sending = false;
            return;
        }
        // start the send/receive handshaking
        sendMessage();
    }

    /**
     * Sends a message.
     */
    private void sendMessage()
    {
        if (sending == true) {
            // first iteration gets the size and populates the buffer
            // then it sends the size
            // then it waits for an ack
            if (sendIter == -1) {
                btService.write(("" + sendSize).getBytes());
                sendIter++;
            }
            else {
                // compute size of next transmission
                int remain = sendSize - 512 * sendIter;
                int min = remain > 512 ? 512 : remain;
                // send packet
                btService.write(data, 512 * sendIter, min);
                // advance to next packet
                sendIter++;
            }
        }
        else {
            // we are receiving a message, so we are only supposed to send an
            // ACK
            btService.write("ACK".getBytes());
        }
    }

    /**
     * Receive a message
     */
    private void receiveMessage(byte[] readBuf, int bytes)
    {
        // if we are sending, then this is an ack
        if (sending == true) {
            // we will just ignore the message, as crazy as that seems...

            // so then all we need to do is advance the FSM
            if (sendIter * 512 >= sendSize) {
                // we've sent the whole file. we're done
                sending = false;
            }
            else {
                // send more data
                sendMessage();
            }
        }
        else {
            // if we haven't started receiving yet, we've got some work to do
            if (sendIter == -1) {
                try {
                    // total size can be determined by the payload of this message
                    sendSize = Integer.parseInt(new String(readBuf, 0, bytes));
                // get room for the data
                data = new byte[sendSize];
                // advance to next state
                sendIter++;
                }
                catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    return;
                }
            }
            else {
                // figure out how much data is in this packet
                int remain = sendSize - 512 * sendIter;
                int min = remain > 512 ? 512 : remain;
                int start = 512 * sendIter;
                // copy data into our nice big packet
                for (int i = 0; i < min; ++i)
                    data[start + i] = readBuf[i];
                // advance to next state
                sendIter++;
            }
            // ack the message
            sendMessage();

            // are we all done?
            if (sendIter * 512 >= sendSize) {
                // clear the counter
                sendIter = -1;
                
                // create a file and dump the byte stream into it
                File fSDCard = new File("/sdcard");
                File fImage = new File(fSDCard.toString() + "/image.jpg");

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
                ImageView iv = (ImageView)findViewById(R.id.imgZapImage);
                iv.setImageURI(Uri.fromFile(fImage)); 

            }
        }
    }

 
    /** the service uses this to communicate with the activity */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            // get the title status field
            TextView tv = (TextView) findViewById(R.id.tvBtTitleRight);

            switch (msg.what) {
                case BTService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTService.STATE_CONNECTED:
                            tv.setText("connected to " + BluetoothManager.getDevName());
                            break;
                        case BTService.STATE_CONNECTING:
                            tv.setText("Connecting...");
                            break;
                        case BTService.STATE_LISTEN:
                        case BTService.STATE_NONE:
                            tv.setText("not connected");
                            break;
                    }
                    break;
                case BTService.MESSAGE_WRITE:
                    //Toast.makeText(BTZapActivity.this, "Send complete", Toast.LENGTH_SHORT).show();
                    break;
                case BTService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    // String readMessage = new String(readBuf, 0, msg.arg1);
                    // mConversationArrayAdapter.add(devName+":  " +
                    // readMessage);
                    receiveMessage(readBuf, msg.arg1);
                    break;
                case BTService.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    BluetoothManager.setDevName(msg.getData().getString("devicename"));
                    Toast.makeText(getApplicationContext(), "Connected to " + BluetoothManager.getDevName(), Toast.LENGTH_SHORT).show();
                    break;
                case BTService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case CarbotApplication.INTENT_BT_TURNON:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                    setupChat();
                }
                else {
                    Toast.makeText(this, "Bluetooth is still off", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case CarbotApplication.INTENT_BT_CONNECT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Connected to" + data.getExtras().getString("MAC_ADDRESS"), Toast.LENGTH_SHORT)
                            .show();
                    // Get the device MAC address
                    String address = data.getExtras().getString("MAC_ADDRESS");
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = BluetoothManager.getBtAdapter().getRemoteDevice(address);
                    // Attempt to connect to the device
                    btService.connect(device);
                }
        }
    }
}