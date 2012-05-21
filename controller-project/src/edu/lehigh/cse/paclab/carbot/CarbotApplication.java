package edu.lehigh.cse.paclab.carbot;

import android.app.Application;

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
     * Initialize the application
     * 
     * We override the method to provide a hook for initializing the State singleton
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        
        // initialize the State singleton
        State.initialize(this);
    }
    
}
