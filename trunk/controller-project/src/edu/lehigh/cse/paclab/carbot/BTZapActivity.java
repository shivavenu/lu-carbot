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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Our goal with this activity is to send an image over bluetooth from one phone
 * to the other.
 * 
 * We're going to keep it simple, and just send /sdcard/image.jpg. Note that
 * facecapture will make that image for you.
 * 
 * TODO: there is a major bug right now: when data is sent, it is not sent as a
 * single message. Thus we are seeing that even for small files, we're actually
 * sending a bunch of messages, but we don't have anything in place for
 * transforming all those messages into one appropriately-sized message. Fixing
 * this is going to be a pain, because we basically have to implement
 * handshaking on top of the bluetooth protocol stack
 */
public class BTZapActivity extends Activity
{
    // Message types sent from the BTService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // this is how we interact with the Bluetooth device
    private BluetoothAdapter btAdapter = null;

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

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Error: no Bluetooth support", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** if the activity suspends, then on resume we need to start the chat service */
    @Override
    public synchronized void onResume() {
        super.onResume();
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BTService.STATE_NONE) {
              // Start the Bluetooth chat services
              btService.start();
            }
        }
    }

    /** stop the service when we're done */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btService != null) btService.stop();
    }

    private BTService btService = null;
    
    final static private int INTENT_TURNITON = 1;
    final static private int INTENT_CONNECT  = 2;
    @Override
    public void onStart()
    {
        super.onStart();
        if (!btAdapter.isEnabled()) {
            Intent turniton = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turniton, INTENT_TURNITON);
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
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendMessage();
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
            case R.id.item1:
                setDiscoverable();
                return true;
            case R.id.item2:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, BTFindDeviceActivity.class);
                startActivityForResult(serverIntent, INTENT_CONNECT);
                return true;
        }
        return false;
    }

    /** make Bluetooth discoverable for 300 seconds*/
    private void setDiscoverable()
    {
        if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(i);
        }
    }
    
    /**
     * Sends a message.
     */
    private void sendMessage() {
        // Check that we're actually connected before trying anything
        if (btService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "Error: No connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        File fSDCard = new File("/sdcard");
		File fImage = new File(fSDCard.toString() + "/image.jpg");
		byte[] b = new byte[(int)fImage.length()];
		Log.i("BTZap", "sent "+fImage.length()+" bytes");
		try {
			FileInputStream fis = new FileInputStream(fImage);
            fis.read(b);
            fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		btService.write(b);
    }

    /** name of the remote device */
    private String devName = null;
    
    /** the service uses this to communicate with the activity */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // get the title status field
            TextView tv = (TextView) findViewById(R.id.tvBtTitleRight);
            
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BTService.STATE_CONNECTED:
                    tv.setText("connected to " + devName);
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
            case MESSAGE_WRITE:
            	Toast.makeText(BTZapActivity.this, "Send complete", Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(devName+":  " + readMessage);
                handleIncomingMessage(readBuf, msg.arg1);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                devName = msg.getData().getString("devicename");
                Toast.makeText(getApplicationContext(), "Connected to "
                               + devName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case INTENT_TURNITON:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                    setupChat();
                }
                else {
                    Toast.makeText(this, "Bluetooth is still off", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case INTENT_CONNECT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Connected to" + data.getExtras().getString("MAC_ADDRESS"), Toast.LENGTH_SHORT).show();
                    // Get the device MAC address
                    String address = data.getExtras().getString("MAC_ADDRESS");
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    btService.connect(device);
                }
        }
    }

    private void handleIncomingMessage(byte[] readBuf, int bytes)
    {
    	Log.i("BTZap", "received " + bytes + " bytes");
        File fSDCard = new File("/sdcard");
		File fImage = new File(fSDCard.toString() + "/image.jpg");

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fImage);
			fos.write(readBuf, 0, bytes);
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
    }
}