package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.services.BTService;
import edu.lehigh.cse.paclab.carbot.services.BluetoothManager;

/**
 * An abstract class for encapsulating the Activity side of Bluetooth
 * communication. I'm punting on figuring out how to tuck a BTService in an
 * Application for multi-Activity access.
 */
public abstract class BTActivity extends Activity
{
    /**
     * if the activity suspends, then on resume we need to start the chat
     * service
     */
    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (BluetoothManager.getBtService() != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (BluetoothManager.getBtService().getState() == BTService.STATE_NONE) {
                // Start the Bluetooth chat services
                BluetoothManager.getBtService().start();
            }
        }
    }

    /** stop the service when we're done */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (!BluetoothManager.getBtAdapter().isEnabled()) {
            Intent turniton = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turniton, CarbotApplication.INTENT_BT_TURNON);
        }
        else {
            if (BluetoothManager.getBtService() == null)
                setupCommunication();
        }
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

    abstract void onStateConnected();

    abstract void onStateConnecting();

    abstract void onStateNone();

    abstract void receiveMessage(byte[] readBuf, int bytes);

    /** the service uses this to communicate with the activity */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {

            switch (msg.what) {
                case BTService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTService.STATE_CONNECTED:
                            onStateConnected();
                            break;
                        case BTService.STATE_CONNECTING:
                            onStateConnecting();
                            break;
                        case BTService.STATE_LISTEN:
                        case BTService.STATE_NONE:
                            onStateNone();
                            break;
                    }
                    break;
                case BTService.MESSAGE_WRITE:
                    break;
                case BTService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    receiveMessage(readBuf, msg.arg1);
                    break;
                case BTService.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    BluetoothManager.setDevName(msg.getData().getString("devicename"));
                    Toast.makeText(getApplicationContext(), "Connected to " + BluetoothManager.getDevName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case BTService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    /** initialize the adapters for chatting */
    private void setupCommunication()
    {

        // Initialize the BluetoothChatService to perform bluetooth connections
        BluetoothManager.setBtService(new BTService(this, mHandler));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case CarbotApplication.INTENT_BT_TURNON:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                    setupCommunication();
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
                    BluetoothManager.getBtService().connect(device);
                }
        }
    }
}
