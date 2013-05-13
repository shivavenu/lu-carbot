package edu.lehigh.cse.paclab.carbot;

import static android.media.ToneGenerator.TONE_DTMF_1;
import static android.media.ToneGenerator.TONE_DTMF_2;
import static android.media.ToneGenerator.TONE_DTMF_3;
import static android.media.ToneGenerator.TONE_DTMF_4;
import static android.media.ToneGenerator.TONE_DTMF_5;
import static android.media.ToneGenerator.TONE_DTMF_6;
import static android.media.ToneGenerator.TONE_DTMF_D;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

/**
 * This is a parent class so that all of our activities have easy access to constants, TTS, and DTMF
 */
public abstract class BasicBotActivityBeta extends Activity implements TextToSpeech.OnInitListener
{
    // constants for preference tags
    final public static String         PREFS_NAME      = "CARBOT_NAME";
    final public static String         PREFS_BYE       = "CARBOT_BYE";
    final public static String         PREFS_DIST      = "CARBOT_DIST";
    final public static String         PREFS_ROT       = "CARBOT_ROT";

    /**
     * This constant describes how long a DTMF tone must play before our system catches the sound
     */
    final public static int            DTMF_DELAY_TIME = 75;

    /**
     * Indicate the port this app uses for sending control signals between a client and server
     */
    public static final int            WIFICONTROLPORT = 9599;

    /**
     * Tag for Android debugging...
     */
    public static final String         TAG             = "Carbot Beta";

    /**
     * The object used to create DTMF tones
     */
    public static final ToneGenerator  _toneGenerator  = new ToneGenerator(AudioManager.STREAM_DTMF, 100);

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

    public static boolean isEmitting = false;
    
    /**
     * Program-wide configuration goes here
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // Keep a self reference, so that alarms can work correctly
        _self = this;
        super.onCreate(savedInstanceState);
        // configure tts and preferences
        tts = new TextToSpeech(this, this);
        prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_WRITEABLE);
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
     * Set an alarm, so that we can know when it's time to stop DTMF
     */
    public void setAlarm()
    {
        Context context = this;
        Intent intent = new Intent(context, AlarmEndToneReceiver.class);

        // remember to delete the older alarm before creating the new one
        PendingIntent pi = PendingIntent.getBroadcast(this, 1, // the request id, used for disambiguating this intent
                intent, 0); // pending intent flags

       
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + DTMF_DELAY_TIME, pi);
    }

    /**
     * Send a DTMF pulse that the robot understands as "go forward"
     */
    public void robotForward()
    {
    	if (!isEmitting){
    		isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_1);
    		setAlarm();
    	}
    }

    /**
     * Send a DTMF pulse that the robot understands as "go backward"
     */
    public void robotReverse()
    {
    	if (!isEmitting) {
    		isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_2);
    		setAlarm();
    	}
    }

    /**
     * Send a DTMF pulse that the robot understands as "rotate counterclockwise"
     */
    public void robotCounterClockwise()
    {
    	if (!isEmitting) {
    		isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_3);
    		setAlarm();
    	}
    }

    /**
     * Send a DTMF pulse that the robot understands as "rotate clockwise"
     */
    public void robotClockwise()
    {
    	if (!isEmitting) {
        	isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_4);
    		setAlarm();
    	}
    }

    /**
     * Send a DTMF pulse that the robot understands as "point turn left"
     */
    public void robotPointTurnLeft()
    {
    	if (!isEmitting) {
    		isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_5);
    		setAlarm();
    	}
    }

    /**
     * Send a DTMF pulse that the robot understands as "point turn right"
     */
    public void robotPointTurnRight()
    {
    	if (!isEmitting) {
    		isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_6);
    		setAlarm();
    	}
    }

    /**
     * Send a DTMF pulse that the robot understands as "stop"
     */
    public void robotStop()
    {
    	if (!isEmitting) {
    		isEmitting = true;
    		_toneGenerator.startTone(TONE_DTMF_D);
    		setAlarm();
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
     * Unused (for now): play a sound file?
     */
    void playCustomSound()
    {

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
}