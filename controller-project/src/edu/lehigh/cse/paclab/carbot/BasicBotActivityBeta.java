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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * TODO: (1) Create the abstract methods for handling Standard Socket as well as any member or static variables (2) Fill
 * in methods for TTS (3) Use Wi-fi socket for remote (4) Remove "connected" pop-up, use either toast or background
 * icon.
 * 
 * @author ArmonShariati
 * 
 */
public abstract class BasicBotActivityBeta extends Activity
{
    /**
     * Indicate the port this app uses for sending control signals between a client and server
     */
    public static final int           WIFICONTROLPORT = 9599;

    public static final String        TAG             = "Carbot Beta";

    /**
     * Unused, but will eventually help us with Text-to-speech stuff
     */
    static public final int           CHECK_TTS       = 99873;

    public static final ToneGenerator _toneGenerator  = new ToneGenerator(AudioManager.STREAM_DTMF, 100);

    private AlarmManager              am;

    // AudioManager audioManager;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "Call to BasicBotActivityBeta::onCreate");
    }

    public void setAlarm()
    {

        Context context = this;
        Intent intent = new Intent(context, AlarmEndToneReceiver.class);

        // remember to delete the older alarm before creating the new one
        PendingIntent pi = PendingIntent.getBroadcast(this, 1, // the request id, used for disambiguating this intent
                intent, 0); // pending intent flags

        am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pi);

    }

    public void robotForward()
    {
        _toneGenerator.startTone(TONE_DTMF_1);
        setAlarm();
    }

    public void robotReverse()
    {
        _toneGenerator.startTone(TONE_DTMF_2);
        setAlarm();
    }

    public void robotCounterClockwise()
    {
        _toneGenerator.startTone(TONE_DTMF_3);
        setAlarm();
    }

    public void robotClockwise()
    {
        _toneGenerator.startTone(TONE_DTMF_4);
        setAlarm();
    }

    public void robotPointTurnLeft()
    {
        _toneGenerator.startTone(TONE_DTMF_5);
        setAlarm();
    }

    public void robotPointTurnRight()
    {
        _toneGenerator.startTone(TONE_DTMF_6);
        setAlarm();
    }

    public void robotStop()
    {
        _toneGenerator.startTone(TONE_DTMF_D);
        setAlarm();
    }

    void initTTS()
    {

    }

    void closeTTS()
    {

    }

    void speak(String s)
    {

    }

    void playCustomSound()
    {

    }

    // required for OnInitListener... what should I use this for?
    void onInit(int status)
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
}
