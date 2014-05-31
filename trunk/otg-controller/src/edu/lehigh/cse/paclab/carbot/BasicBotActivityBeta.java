package edu.lehigh.cse.paclab.carbot;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

/**
 * This is a parent class so that all of our activities have easy access to constants and TTS
 */
public abstract class BasicBotActivityBeta extends Activity implements TextToSpeech.OnInitListener
{
    // constants for preference tags
    final public static String         PREFS_NAME              = "CARBOT_NAME";
    final public static String         PREFS_BYE               = "CARBOT_BYE";
    final public static String         PREFS_DIST              = "CARBOT_DIST";
    final public static String         PREFS_ROT               = "CARBOT_ROT";
    final public static String         PREFS_MODE              = "CARBOT_MODE";

    /**
     * Indicate the port this app uses for sending control signals between a client and server
     * 
     * TODO: move to preferences?
     */
    public static final int            WIFICONTROLPORT         = 9599;

    /**
     * Tag for debugging...
     */
    public static final String         TAG                     = "Carbot";

    /**
     * For accessing the preferences storage of the activity
     */
    SharedPreferences                  prefs;

    /**
     * A self reference, for alarms
     */
    public static BasicBotActivityBeta _self;

    /**
     * The text to speech interface
     */
    TextToSpeech                       tts;


    /**
     * When 'back' is pressed, shut off the USBManager support and terminate the app
     */
    @Override
    public void onBackPressed()
    {
        finish();
    }

    /**
     * Program-wide configuration goes here
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Keep a self reference, so that alarms can work correctly
        _self = this;

        // configure tts and preferences
        tts = new TextToSpeech(this, this);
        prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_WRITEABLE);

        // Don't let the app sleep
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * When the activity goes away, we need to clear out TTS
     */
    @Override
    public void onDestroy()
    {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    /**
     * When TTS is initialized, we need this
     */
    @Override
    public void onInit(int status)
    {
        // if we were successful initialization, set the language to US
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Trouble with language pack");
            }
        }
        else {
            Log.e(TAG, "TTS Initialization error");
        }
    }

    /**
     * Simple mechanism for using text-to-speech
     */
    void speak(String s)
    {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * This method returns the phone's IP addresses
     * 
     * @return A string representation of the phone's IP addresses
     */
    static String getLocalIpAddress()
    {
        String ans = "";
        try {
            // get all network interfaces, and create a string of all their addresses
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                Enumeration<InetAddress> addrList = ni.getInetAddresses();
                while (addrList.hasMoreElements()) {
                    InetAddress addr = addrList.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        ans += addr.getHostAddress().toString() + ";";
                    }
                }
            }
        }
        catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return ans;
    }

    /**
     * A wrapper for Toast that handles problems that stem from trying to Toast from a thread that isn't the UI thread.
     * This variant prints a short toast.
     * 
     * @param s
     *            The message to display
     */
    void shortbread(final String s)
    {
        BasicBotActivityBeta.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(BasicBotActivityBeta.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * A wrapper for Toast that handles problems that stem from trying to Toast from a thread that isn't the UI thread.
     * This variant prints a long toast.
     * 
     * @param s
     *            The message to display
     */
    void longbread(final String s)
    {
        BasicBotActivityBeta.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(BasicBotActivityBeta.this, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This function gives us the ability to have an AlarmReceiver that can do all sorts of arbitrary stuff
     */
    abstract public void callback();

    // /// OTG Hacks

    /**
     * Driver instance, passed in statically via {@link #show(Context, UsbSerialDriver)}.
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using arguments passed in with the
     * {@link #startActivity(Intent)} intent. We can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    protected static UsbSerialDriver                  sDriver   = null;

    private final ExecutorService                   mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager                mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener()
                                                              {

                                                                  @Override
                                                                  public void onRunError(Exception e)
                                                                  {
                                                                      // Log.d(TAG, "Runner stopped.");
                                                                  }

                                                                  @Override
                                                                  public void onNewData(final byte[] data)
                                                                  {
                                                                      // we don't expect data to come back anymore...
                                                                  }
                                                              };

    byte[]                                          data      = new byte[1];

    @Override
    protected void onPause()
    {
        super.onPause();
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            }
            catch (IOException e) {
                // Ignore.
            }
            sDriver = null;
        }
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Log.d(TAG, "Resumed, sDriver=" + sDriver);
        if (sDriver == null) {
            Toast.makeText(this, "No serial device.", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            }
            catch (IOException e) {
                // Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    sDriver.close();
                }
                catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            Toast.makeText(this, "Serial device: " + sDriver.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
        }
        onDeviceStateChange();
    }

    private void stopIoManager()
    {
        if (mSerialIoManager != null) {
            // Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager()
    {
        if (sDriver != null) {
            // Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange()
    {
        stopIoManager();
        startIoManager();
    }


    /**
     * Send a byte to the Arduino that instructs it to stop
     */
    public void myRobotStop()
    {
        otgSendCommand((byte) 0);
    }

    /**
     * Send a byte to the Arduino that instructs it to go forward
     */
    public void myRobotForward()
    {
        otgSendCommand((byte) 1);
    }

    /**
     * Send a byte to the Arduino that instructs it to go backward
     */
    public void myRobotReverse()
    {
        otgSendCommand((byte) 2);
    }

    /**
     * Send a byte to the Arduino that instructs it to go clockwise
     */
    public void myRobotClockwise()
    {
        otgSendCommand((byte) 3);
    }

    /**
     * Send a byte to the Arduino that instructs it to go counterclockwise
     */
    public void myRobotCounterClockwise()
    {
        otgSendCommand((byte) 4);
    }

    /**
     * Send a byte to the Arduino that instructs it to do a point turn right
     */
    public void myRobotPointTurnRight()
    {
        otgSendCommand((byte) 6);
    }

    /**
     * Send a byte to the Arduino that instructs it to do a pont turn left
     */
    public void myRobotPointTurnLeft()
    {
        otgSendCommand((byte) 5);
    }

    /**
     * Custom sendCommand for interacting with OTG
     */
    void otgSendCommand(byte b)
    {
        data[0] = b;
        mSerialIoManager.writeAsync(data);
    }
}