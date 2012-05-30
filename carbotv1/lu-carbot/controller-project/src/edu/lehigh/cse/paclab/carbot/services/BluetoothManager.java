package edu.lehigh.cse.paclab.carbot.services;

import java.util.Set;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.CarbotApplication;

/**
 * BluetoothManager is a Singleton class for managing the Bluetooth service
 * 
 */
public class BluetoothManager
{
    /**
     * Reference to the application, useful for Toast, making Intents, and
     * starting Activities
     */
    private static Application cachedApplicationContext;

    /**
     * Getter for the application context
     * 
     * @return the application context of this State object
     */
    public static Application getCachedApplicationContext()
    {
        return cachedApplicationContext;
    }

    /**
     * Configure the BluetoothManager singleton by setting the application
     * context
     * 
     * @param appContext
     *            A reference to the Application
     */
    public static void initialize(Application appContext)
    {
        cachedApplicationContext = appContext;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null)
            Toast.makeText(appContext, "Error: no Bluetooth support", Toast.LENGTH_SHORT).show();
    }

    /**
     * Track whether BluetoothManager is fully configured
     */
    private static boolean btConfigured = false;

    private static Activity cachedActivity = null;

    /**
     * Configure BluetoothManager. Note that tts cannot be configured without an
     * activity, and thus we can't do this configuration from the initialize()
     * method.
     * 
     * @param callingActivity
     *            The activity that configured the TTS. It will catch TTS
     *            intents and feed them back to State.
     */
    public static void configure(Activity callingActivity)
    {
        cachedActivity = callingActivity;
        if (!BluetoothManager.getBtAdapter().isEnabled()) {
            Intent turniton = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cachedActivity.startActivityForResult(turniton, CarbotApplication.INTENT_BT_TURNON);
        }

        // NB: the intent will be sent to callingActivity, which should use the
        // below handleStateIntent method to allow State to filter out TTS
        // intents
    }

     /**
     * Shut down Bluetooth
     * 
     * [TODO] Not called yet, should be called when the application terminates
     */
    public static void shutdown()
    {
        // cancel any in-progress discovery
        if (btAdapter != null)
            if (btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();
        // kill the BT service
        if (BluetoothManager.getBtService() != null)
            BluetoothManager.getBtService().stop();

    }

    /**
     * Getter for testing if Bluetooth is available
     * 
     * @return True iff the Bluetooth is ready to use
     */
    public static boolean isBtConfigured()
    {
        return btConfigured;
    }

    /**
     * This is the Bluetooth adapter object, through which we interact with the
     * Bluetooth hardware
     */
    private static BluetoothAdapter btAdapter = null;

    public static BluetoothAdapter getBtAdapter()
    {
        return btAdapter;
    }

    /**
     * Kick off discovery of devices
     */
    public static void doDiscovery()
    {
        // stop existing discovery
        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();
        // start discovering again
        btAdapter.startDiscovery();
    }

    public static void cancelDiscovery()
    {
        btAdapter.cancelDiscovery();
    }

    /**
     * Get the list of bonded devices
     * 
     * @return A list of bonded devices
     */
    public static Set<BluetoothDevice> getBondedDevices()
    {
        return btAdapter.getBondedDevices();
    }

    /** make Bluetooth discoverable for 300 seconds */
    public static void setDiscoverable()
    {
        if (BluetoothManager.getBtAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            cachedActivity.startActivity(i);
        }
    }

    public static String getDevName()
    {
        return devName;
    }

    public static void setDevName(String name)
    {
        devName = name;
    }

    public static BTService getBtService()
    {
        return btService;
    }

    public static void setBtService(BTService bts)
    {
        btService = bts;
    }

    /** name of the remote device */
    private static String devName = null;
    
    private static BTService btService = null;
}
