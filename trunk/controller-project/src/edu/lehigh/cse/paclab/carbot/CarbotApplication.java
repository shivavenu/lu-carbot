package edu.lehigh.cse.paclab.carbot;

import android.app.Application;
import edu.lehigh.cse.paclab.carbot.services.ArduinoManager;
import edu.lehigh.cse.paclab.carbot.services.BluetoothManager;
import edu.lehigh.cse.paclab.carbot.services.TTSManager;
import edu.lehigh.cse.paclab.carbot.services.VisualMemoryManager;

/**
 * Override the Application class, so that we can be sure that our State
 * singleton is initialized.
 * 
 * In more detail, every Android app has an Application object, which persists
 * even as individual activities are created and destroyed. We would like to be
 * able to use the Application as a hook, so that we can save things like the
 * reference to the Bluetooth Service and the reference to the ArduinoController
 * Service... doing so allows us to access those services from any Activity in
 * the application, without having to do any work in the individual activities.
 * 
 * We override the Application object here, and update onCreate() to initialize
 * our "State" class. In the Manifest, we also declare that our app uses
 * CarbotApplication, instead of the default Application. Then we implement
 * State as a Singleton, and that's enough to ensure that every Activity has
 * access to every Service that we create.
 */
public class CarbotApplication extends Application
{
    /**
     * Constant to indicate interactions between the Application and the
     * text-to-speech service
     */
    public static final int INTENT_TTS_CHECK = 99873;
    public static final int INTENT_BT_TURNON = 72418;
    public static final int INTENT_BT_CONNECT = 445452;
    public static final int INTENT_SNAP_PHOTO = 943557;

    /**
     * Initialize the application
     * 
     * We override the method to provide a hook for initializing the singleton
     * initialization
     */
    @Override
    public void onCreate()
    {
        super.onCreate();

        // initialize the TTS singleton
        TTSManager.initialize(this);

        // initialize the VisualMemory singleton
        VisualMemoryManager.initialize(this);

        // initialize the Bluetooth singleton
        BluetoothManager.initialize(this);

        // initialize the Arduino singleton
        ArduinoManager.initialize(this);
    }
}
