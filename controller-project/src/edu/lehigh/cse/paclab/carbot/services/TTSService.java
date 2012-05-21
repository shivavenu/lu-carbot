package edu.lehigh.cse.paclab.carbot.services;

import java.util.Locale;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.widget.Toast;

/**
 * TTSService is a Singleton class for managing the Text-To-Speech service
 * 
 */
public class TTSService
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
     * Configure the State singleton by setting the application context
     * 
     * @param appContext
     *            A reference to the Application
     */
    public static void initialize(Application appContext)
    {
        cachedApplicationContext = appContext;
    }

    /**
     * Constant to indicate interactions between the Application and the
     * text-to-speech service
     */
    private static final int CHECK_TTS = 99873;

    /**
     * The Text To Speech service.
     * 
     * Use this object to talk
     */
    private static TextToSpeech mTTS;

    /**
     * Track whether TTS is configured
     */
    private static boolean ttsConfigured = false;

    /**
     * This callback runs when we configure the TTS, to indicate that we are
     * initialized
     * 
     * [TODO] We should actually look at the status!
     */
    private static OnInitListener ttsListener = new OnInitListener()
    {
        @Override
        public void onInit(int status)
        {
            mTTS.speak("Hello, I am car-bot", TextToSpeech.QUEUE_FLUSH, null);
            ttsConfigured = true;
        }
    };

    /**
     * Configure TTS. Note that tts cannot be configured without an activity,
     * and thus we can't do this configuration from the initialize() method.
     * 
     * @param callingActivity
     *            The activity that configured the TTS. It will catch TTS
     *            intents and feed them back to State.
     */
    public static void configure(Activity callingActivity)
    {
        // check if text-to-speech is supported, via an intent:
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        callingActivity.startActivityForResult(checkIntent, CHECK_TTS);
        // NB: the intent will be sent to callingActivity, which should use the
        // below handleStateIntent method to allow State to filter out TTS
        // intents

        // [TODO] should have code in here for tracking if TTS is in the midst
        // of configuration, so we don't config twice...
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
    public static boolean handleIntent(int requestCode, int resultCode, Intent data)
    {
        // check if this is a TTS Intent
        if (requestCode == CHECK_TTS) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTTS = new TextToSpeech(getCachedApplicationContext(), ttsListener);
            }
            else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                getCachedApplicationContext().startActivity(installIntent);
            }
            // set the locale
            if (mTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                mTTS.setLanguage(Locale.US);
            }
            return true;
        }

        // punt on this intent, leave it up to the caller to handle
        return false;
    }

    /**
     * Shut down TTS.
     * 
     * [TODO] Not called yet, should be called when the application terminates
     */
    public static void shutdownTTS()
    {
        if (mTTS != null) {
            mTTS.speak("Goodbye", TextToSpeech.QUEUE_FLUSH, null);
            mTTS.stop();
            mTTS.shutdown();
            ttsConfigured = false;
        }
    }

    /**
     * Attempt to use TTS to communicate. If the TTS is not available, use Toast
     * instead
     * 
     * @param text
     */
    public static void sayIt(String text)
    {
        if (ttsConfigured)
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        else
            Toast.makeText(getCachedApplicationContext(), "TTS Not Available: " + text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Getter for testing if TTS is available
     * 
     * [TODO] Is this useful?
     * 
     * @return True iff the TTS is ready to use
     */
    public static boolean isTtsConfigured()
    {
        return ttsConfigured;
    }
}
