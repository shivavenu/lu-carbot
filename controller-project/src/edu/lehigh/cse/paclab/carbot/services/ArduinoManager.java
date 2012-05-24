package edu.lehigh.cse.paclab.carbot.services;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

public class ArduinoManager
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
    }

    private static Activity cachedActivity = null;

    /**
     */
    public static void configure(Activity callingActivity)
    {
        cachedActivity = callingActivity;
    }

    public static void shutdown()
    {
        cachedApplicationContext.unbindService(conn);
    }

    static Messenger messenger = null;

    static private Handler handler = new Handler()
    {
        public void handleMessage(Message message)
        {
            Bundle data = message.getData();
            if (message.arg1 == Activity.RESULT_OK && data != null) {
                String text = data.getString(ArduinoService.RETURNVAL);
                Toast.makeText(cachedActivity, text, Toast.LENGTH_LONG).show();
            }
        }
    };
    
    private static ServiceConnection conn = new ServiceConnection()
    {

        public void onServiceConnected(ComponentName className, IBinder binder)
        {
            messenger = new Messenger(binder);
        }

        public void onServiceDisconnected(ComponentName className)
        {
            messenger = null;
        }
    };

    static public void config()
    {
        Intent intent = null;
        intent = new Intent(cachedActivity, ArduinoService.class);
        // Create a new Messenger for the communication back
        // From the Service to the Activity
        Messenger messenger = new Messenger(handler);
        intent.putExtra("MESSENGER", messenger);
        cachedApplicationContext.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    static public void sendCommand(String CMD)
    {
        Message msg = Message.obtain();

        try {
            Bundle bundle = new Bundle();
            bundle.putString("CMD", CMD);
            msg.setData(bundle);
            messenger.send(msg);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}