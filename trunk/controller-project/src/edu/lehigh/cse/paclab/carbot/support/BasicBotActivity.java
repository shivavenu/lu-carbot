package edu.lehigh.cse.paclab.carbot.support;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.R;

/**
 * Any activity that runs on a phone that is plugged into an Arduino will
 * inherit from this.
 * 
 * This class ensures that any descendant has a properly configured UsbManager
 * for talking to the Arduino, and also that it has the capability to easily be
 * configured for Bluetooth.
 * 
 * @author spear
 * 
 */
@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi", "NewApi", "NewApi", "NewApi", "NewApi", "NewApi" })
public abstract class BasicBotActivity extends Activity implements OnInitListener
{
    // constants for preference tags
    final public static String PREF_TAG_NAME = "KB_CONFIG_NAME";
    final public static String PREF_TAG_FAREWELL = "KB_CONFIG_FAREWELL";
    final public static String PREF_TAG_METER = "KB_CONFIG_METER";
    final public static String PREF_TAG_ROTATE = "KB_CONFIG_ROTATE";
    final public static String PREF_TAG_CAMLAG = "KB_CONFIG_CAMLAG";
    final public static String PREF_TAG_CAMSTART = "KB_CONFIG_CAMSTART";
    final public static String PREF_TAG_CAMFACE = "KB_CONFIG_CAMFACE";

    final public static String PREF_HUE_AVG = "PREF_HUE_AVG";
    final public static String PREF_HUE_STD = "PREF_HUE_STD";
    final public static String PREF_SAT_AVG = "PREF_SAT_AVG";
    final public static String PREF_SAT_STD = "PREF_SAT_STD";
    final public static String PREF_VAL_AVG = "PREF_VAL_AVG";
    final public static String PREF_VAL_STD = "PREF_VAL_STD";

    // intent constants
    public static final int INTENT_SNAP_PHOTO = 943557;
    final static private int INTENT_TURNITON = 7213;
    final static private int INTENT_CONNECT = 59847;
    public static final int INTENT_PHOTO_DONE = 66711324;

    public static final String TAG = "Carbot";

    // nasty that we're hacking it like this, but it's OK for now since we have
    // one menu for all activities.
    protected boolean chatterboxOverride = false;

    // Following code block is setting up the android to arduino communication.
    private static final String ACTION_USB_PERMISSION = "com.google.android.Demokit.action.USB_PERMISSION";
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    // this is how we interact with the Bluetooth device
    private BluetoothAdapter btAdapter = null;

    private boolean isUSBReceiverRegistered = false;

    PowerManager.WakeLock wl;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "Call to BasicBotActivity::onCreate");

        // Arduino Support

        // Looks for input
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        // Creates new IntentFilter to indicate future communication with a
        // particular entity
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        isUSBReceiverRegistered = true;

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

        // jellybean edits for text-to-speech...
        //
        // Warning: this won't install a language pack if one isn't available,
        // which means you might have really funky failures if you don't have
        // languages installed. It's not a problem for Nexus7, so I'm leaving
        // this for now
        mTts = new TextToSpeech(this, this);
        if (TextToSpeech.LANG_AVAILABLE == mTts.isLanguageAvailable(Locale.US)) {
            mTts.setLanguage(Locale.US);
        }
        else {
            Toast.makeText(this,
                    "Unable to set a language pack for Text-To-Speech... Bad things are likely to happen from here.",
                    Toast.LENGTH_LONG).show();
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Error: no Bluetooth support", Toast.LENGTH_SHORT).show();
            finish();
        }

        // grab a wake lock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        wl.acquire();

        // set up custom window title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    }

    protected TextView tvStatus;

    protected void initBTStatus()
    {
        // save the status field so we can update it easily
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.bttitle);
        tvStatus = (TextView) findViewById(R.id.tvBtTitleRight);
        tvStatus.setText("not connected");
    }

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

    /**
     * If the application has stopped and then has resumed, the application will
     * check to see if the input and output stream of data is still active then
     * checks to see if an accessory is present, if so, opens communication; if
     * not, the application will request permission for communication.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        if (mInputStream != null && mOutputStream != null) {
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            }
            else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        }
        else {
            Log.d(TAG, "mAccessory is null");
        }

        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (btService.getState() == BTService.STATE_NONE) {
                // Start the Bluetooth chat services
                btService.start();
            }
        }
    }

    /**
     * More watchdogs...
     */
    @Override
    public void onBackPressed()
    {
        // Arduino
        closeAccessory();
        if (isUSBReceiverRegistered)
            unregisterReceiver(mUsbReceiver);

        // TTS
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }

        // Bluetooth
        if (btService != null)
            btService.stop();

        // give up the wakelock
        wl.release();

        // finish the activity
        finish();
    }

    /**
     * Required for TextToSpeech.OnInitListener
     */
    @Override
    public void onInit(int status)
    {
    }

    /**
     * BroadcastReceiver is the object responsible for establishing
     * communication with any sort of entity sending information to the
     * application, in this case, the application is receiving information from
     * an arduino. The BroadcastReceiver sees if the entity sending information
     * is a supported usb accessory, in which case opens full communication with
     * the device beyond the handshake which is initiated upon application
     * launch.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    }
                    else {
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            }
            else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.i("CARBOT", "resultCode = " + resultCode);
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
                    Toast.makeText(this, "Connecting to " + data.getExtras().getString("MAC_ADDRESS"),
                            Toast.LENGTH_SHORT).show();
                    // Get the device MAC address
                    String address = data.getExtras().getString("MAC_ADDRESS");
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    btService.connect(device);
                }
        }
    }

    /**
     * In the event of the application being terminated or paused closeAccessory
     * is called to end the data input stream.
     */
    protected void closeAccessory()
    {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        }
        catch (IOException e) {
        }
        finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    /**
     * Called upon the construction of the BroadcastReceiver assuming the
     * BroadcastReceiver has found an accessory to interact with. openAccessory
     * is also called in the onResume method. Opens up a data output and input
     * stream for communication with an accessory.
     * 
     * @param accessory
     */
    protected void openAccessory(UsbAccessory accessory)
    {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
        }
        else {
            Log.d(TAG, "accessory open fail");
        }
    }

    /**
     * Called anytime information is to be sent out from the application over
     * the output stream. An array of bytes is created with the first value
     * holding the "address" of the hardware being communicated with. In this
     * case, 1 means the forward, 2 means reverse, etc. In our system, arduino
     * handles the task to be carried out by the hardware, so, arduino only
     * needs to know whether or not to carry out a particular action indicated
     * by the action reference number.
     * 
     * @param target
     */
    public void sendCommand(byte target)
    {
        byte[] buffer = new byte[1];
        buffer[0] = target;

        Log.e(TAG, "Message sent" + buffer[0]);
        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
            }
            catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    public void robotStop()
    {
        sendCommand((byte) 0);
    }

    public void robotForward()
    {
        sendCommand((byte) 1);
    }

    public void robotReverse()
    {
        sendCommand((byte) 2);
    }

    public void robotClockwise()
    {
        sendCommand((byte) 3);
    }

    public void robotCounterclockwise()
    {
        sendCommand((byte) 4);
    }

    public void robotPointTurnRight()
    {
        sendCommand((byte) 6);
    }

    public void robotPointTurnLeft()
    {
        sendCommand((byte) 5);
    }

    private TextToSpeech mTts;

    public void Speak(String s)
    {
        mTts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }

    protected BTService btService = null;

    /** initialize the adapters for chatting */
    private void setupChat()
    {
        // Initialize the BluetoothChatService to perform bluetooth connections
        btService = new BTService(this, mHandler);
    }

    /** Draw our menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth, menu);
        return true;
    }

    /** This runs when a menu item is clicked */
    /**
     * NOTE: OPENCV does not like this method, resolve later...
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	 	
        /*switch (item.getItemId()) {
            case R.id.menuBTDiscoverable:
                setDiscoverable();
                return true;
            case R.id.menuBTFindDevice:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, BTFindDeviceActivity.class);
                startActivityForResult(serverIntent, INTENT_CONNECT);
                return true;
            case R.id.menuChatterboxOverride:
                chatterboxOverride = !chatterboxOverride;
                return true;
        }*/
        return false;
    }

    /** make Bluetooth discoverable for 300 seconds */
    private void setDiscoverable()
    {
        if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(i);
        }
    }

    /** name of the remote device */
    private String devName = null;

    abstract protected void receiveMessage(byte[] readBuf, int bytes);

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
                            if (tv != null)
                                tv.setText("connected to " + devName);
                            break;
                        case BTService.STATE_CONNECTING:
                            if (tv != null)
                                tv.setText("Connecting...");
                            break;
                        case BTService.STATE_LISTEN:
                        case BTService.STATE_NONE:
                            if (tv != null)
                                tv.setText("not connected");
                            break;
                    }
                    break;
                case BTService.MESSAGE_WRITE:
                    // Toast.makeText(BTZapActivity.this, "Send complete",
                    // Toast.LENGTH_SHORT).show();
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
                    devName = msg.getData().getString("devicename");
                    Toast.makeText(getApplicationContext(), "Connected to " + devName, Toast.LENGTH_SHORT).show();
                    break;
                case BTService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };
}