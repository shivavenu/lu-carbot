package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BlueToothActivity extends Activity
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
        setContentView(R.layout.bluetoothlayout);
        // attach the custom title to our "title" layout
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.bttitle);
        // set the text for the RHS
        TextView tv = (TextView) findViewById(R.id.textView2);
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

    // this is a member so we can access it easily
    private ArrayAdapter<String> mConversationArrayAdapter;
    
    /** initialize the adapters for chatting */
    private void setupChat() 
    {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView mConversationView = (ListView) findViewById(R.id.listView1);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the send button with a listener that for click events
        Button mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                EditText et = (EditText) findViewById(R.id.editText1);
                String message = et.getText().toString();
                sendMessage(message);
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
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (btService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "Error: No connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            btService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            EditText et = (EditText) findViewById(R.id.editText1);
            et.setText("");
        }
    }

    /** name of the remote device */
    private String devName = null;
    
    /** the service uses this to communicate with the activity */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // get the title status field
            TextView tv = (TextView) findViewById(R.id.textView2);
            
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BTService.STATE_CONNECTED:
                    tv.setText("connected to " + devName);
                    mConversationArrayAdapter.clear();
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
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(devName+":  " + readMessage);
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

}