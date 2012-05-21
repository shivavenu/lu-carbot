package edu.lehigh.cse.paclab.carbot;

import android.app.Application;
import android.content.Intent;
import edu.lehigh.cse.paclab.carbot.services.TTSService;

/**
 * State is a singleton class, which we use to store application-wide global
 * variables. In particular, State holds references to the Arduino and Bluetooth
 * Service objects, so that every activity can access them.
 * 
 * NB: this is a Singleton pattern, so every method and field should be static
 * 
 * [TODO] I'm not sure we actually need this... TTSService must be initialized
 * explicitly from an activity, and the ArduinoService and BluetoothService will
 * both probably both be encapsulated in their own classes too. Thus the only
 * real benefits of this code are that it provides the cachedApplicationContext
 * and it decreases the amount of intent filtering required by each Activity.
 */
public class State
{
    /**
     * Reference to the application, useful for Toast, making Intents, and
     * starting Activities
     */
    public static Application cachedApplicationContext;

    /**
     * Track whether the default State fields are configured or not.
     */
    private static boolean configured = false;

    /**
     * Return true iff the State singleton is configured.
     * 
     * @return the configuration state of the object
     */
    public static boolean isConfigured()
    {
        return configured;
    }

    /**
     * Configure the State singleton
     * 
     * For now, this is just a stub
     * 
     * @param appContext
     *            A reference to the Application
     */
    public static void initialize(Application appContext)
    {
        cachedApplicationContext = appContext;
        configured = true;
    }

    /**
     * Many Activities could end up receiving intents that are supposed to go to
     * State. This method is used by State to filter out those intents and
     * handle them.
     * 
     * @param requestCode
     *            The code for the Intent
     * @param resultCode
     *            The return value
     * @param data
     *            Any extras
     * @return True if the Intent was handled by this code, False if it should
     *         be handled by the caller
     */
    static public boolean handleStateIntent(int requestCode, int resultCode, Intent data)
    {
        // check if this is a TTS Intent
        if (TTSService.handleStateIntent(requestCode, resultCode, data))
            return true;

        // punt on this intent, leave it up to the caller to handle
        return false;
    }
    
    /**
     * Private constructor to enforce Singleton pattern
     */
    private State()
    {
    }
}
