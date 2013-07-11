package edu.lehigh.cse.paclab.carbot;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;

/**
 * This is going to be a silly speech-to-text game. We're going to let the kids talk to the robot, and see if it can
 * understand their words at all :)
 * 
 * TODO: Are we using this at all?
 */
public class Chatterbox extends BasicBotActivityBeta
{
    /**
     * Unique code for getting the voice request back
     */
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 789437;

    /**
     * There is no complex initialization for this Activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatterbox);

        // [mfs] This disables headphone communication... not exactly what we
        // want, but enough of a demo to make things straightforward...
        //AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //am.setMode(AudioManager.MODE_IN_CALL);
        //am.setSpeakerphoneOn(true);
    }

    /**
     * Listening entails launching an intent to get instructions for the robot.
     */
    public void listen(View v)
    {
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
            if (res.contains("dance")) {
                speak("Did you say dance?  That robot over there likes to dance... should I play some music for it?");
            }
            else if (res.contains("your name")) {
                speak("My name is " + prefs.getString(PREFS_NAME, "Mrs. Robot"));
            }
            else if (res.contains("do you like first grade")) {
                speak("I love first grade.  Reading and Writing and Math are some of my favorites!");
            }
            else if (res.contains("do you like kindergarten")) {
                speak("I think kindergarten is great. I especially love kid writing!");
            }
            else if (res.contains("what do you know")) {
                speak("I don't know very much.  Robots aren't as smart as kids.");
            }
            else {
                speak("It sounded like you said " + res + ".  I don't know what that means");
            }
        }
        else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            speak("I didn't understand that... please try again");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void callback()
    {
    }
}
