package edu.lehigh.cse.paclab.carbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import edu.lehigh.cse.paclab.carbot.support.AlarmDanceReceiver;
import edu.lehigh.cse.paclab.carbot.support.BasicBotActivity;

/**
 * This is going to be a silly speech-to-text game. We're going to let the kids
 * talk to the robot, and see if it can understand their words at all :)
 * 
 * [TODO] It would be nice to use Bluetooth as a remote control, for launching
 * Speech-to-Text intents...
 */
public class Chatterbox extends BasicBotActivity
{
    /**
     * Unique code for getting the voice request back
     */
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 789437;

    public static Chatterbox self;

    /**
     * There is no complex initialization for this Activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatterbox);
        initBTStatus();
        initTheDance();
        self = this;
        mp = new MediaPlayer();
        try {
            final AssetFileDescriptor assetFileDescritor = this.getAssets().openFd("tune.mp3");
            mp.setDataSource(assetFileDescritor.getFileDescriptor(), assetFileDescritor.getStartOffset(),
                    assetFileDescritor.getLength());
            mp.prepare();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // [mfs] This disables headphone communication... not exactly what we
        // want, but enough of a demo to make things straightforward...
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_CALL);
        am.setSpeakerphoneOn(true);
    }

    /**
     * Listening entails launching an intent to get instructions for the robot.
     */
    public void listen(View v)
    {
        if ((mp != null) && (mp.isPlaying()))
            mp.stop();

        // set up an intent to ask for speech-to-text, and connect it back to
        // this Activity
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());

        // Give text to display, and a hint about the language model
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell the robot what to do!");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // How many results to return? They will be sorted by confidence
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        // Start the activity
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    private MediaPlayer mp;

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // go through the results, dump them all to a TextView
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String res = matches.get(0);
            if (res.contains("dance") || chatterboxOverride) {
                Speak("Did you say dance?  I love to dance!");
                nextDance();
            }
            else {
                Speak("It sounded like you said " + res + ".  I don't know what that means");
            }

        }
        else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            Speak("I didn't understand that... please try again");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
        // [TODO] if we have BT, we need to do stuff here...

    }

    private class TimedAction
    {
        public int atMilliseconds;
        public char action;

        public TimedAction(int i, char c)
        {
            atMilliseconds = i;
            action = c;
        }

        @Override
        public String toString()
        {
            return "(" + atMilliseconds + ", " + action + ")";
        }
    }

    ArrayList<TimedAction> theDance;

    private void initTheDance()
    {
        // for random actions
        final char[] actions = { 'n', 's', 'f', 'l', 'r', 'b', '+', '-' };
        Random r = new Random();

        // we're at step 0
        dancestep = 0;

        // set up arraylist, start music, then pause briefly
        theDance = new ArrayList<TimedAction>();
        // first we wait, so phone can stop talking
        theDance.add(new TimedAction(8000, 'n'));
        // then we start music
        theDance.add(new TimedAction(3650, 'p')); // nothing during
                                                  // "who let the dogs out"
        theDance.add(new TimedAction(700, '+')); // who
        theDance.add(new TimedAction(450, '-')); // who
        theDance.add(new TimedAction(390, '+')); // who
        theDance.add(new TimedAction(300, '-')); // who
        for (int i = 0; i < 2; ++i) {
            theDance.add(new TimedAction(1800, 's')); // stop for
                                                      // "who let the dogs out"
            theDance.add(new TimedAction(700, '+')); // who
            theDance.add(new TimedAction(450, '-')); // who
            theDance.add(new TimedAction(390, '+')); // who
            theDance.add(new TimedAction(300, '-')); // who
        }

        // now the verse
        for (int i = 0; i < 36; ++i) {
            int a = Math.abs(r.nextInt()) % 8;
            theDance.add(new TimedAction(490, actions[a]));
        }

        for (int i = 0; i < 4; ++i) {
            theDance.add(new TimedAction(1800, 's')); // stop for
                                                      // "who let the dogs out"
            theDance.add(new TimedAction(700, '+')); // who
            theDance.add(new TimedAction(450, '-')); // who
            theDance.add(new TimedAction(390, '+')); // who
            theDance.add(new TimedAction(300, '-')); // who
        }

        // now quit
        theDance.add(new TimedAction(2000, 'd'));
        theDance.add(new TimedAction(2000, 'x'));
    }

    int dancestep = 0;

    public void nextDance()
    {
        // find the 'dancestep' item in 'theDance' and do it
        if (theDance.size() <= dancestep) {
            robotStop();
            return;
        }
        TimedAction ta = theDance.get(dancestep);
        switch (ta.action) {
            case 'n':
                // no-op;
                break;
            case 'p':
                // play music;
                if (mp != null)
                    mp.start();
                break;
            case 's':
                // stop
                robotStop();
                break;
            case 'f':
                // forward
                robotForward();
                break;
            case 'l':
                // left
                robotStop();
                robotPointTurnLeft();
                break;
            case 'r':
                // right
                robotStop();
                robotPointTurnRight();
                break;
            case 'b':
                // backward
                robotReverse();
                break;
            case '+':
                // rotate clockwise
                robotClockwise();
                break;
            case '-':
                // rotate counterclockwise
                robotCounterclockwise();
                break;
            case 'd':
                // all done
                robotStop();
                Speak("Whew, I'm getting tired");
                mp.stop();
                break;
            case 'x':
                SharedPreferences prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity",
                        Activity.MODE_WORLD_WRITEABLE);
                String s = prefs.getString(PREF_TAG_FAREWELL,
                        "Thank you for letting me come to your class.  I hope you have a great summer!");
                Speak(s);
                return;
        }

        // now set a timer for the next dance step
        // set a timer for when to do the next move
        Intent intent = new Intent(this, AlarmDanceReceiver.class);
        intent.putExtra("AlarmID", dancestep);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), dancestep++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (ta.atMilliseconds),
                pendingIntent);
    }
}
