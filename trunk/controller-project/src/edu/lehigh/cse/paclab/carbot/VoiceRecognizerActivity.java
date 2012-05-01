package edu.lehigh.cse.paclab.carbot;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.TextView;

/**
 * Simple demo to show how we can listen the the user and translate their words
 * into text
 */
public class VoiceRecognizerActivity extends Activity
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
        setContentView(R.layout.voicerecognizerlayout);
    }

    /**
     * Listening entails launching an intent to get instructions for the robot.
     */
    public void listen(View v)
    {
        // set up an intent to ask for speech-to-text, and connect it back to this Activity
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());

        // Give text to display, and a hint about the language model
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell the robot what to do!");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        // How many results to return?  They will be sorted by confidence
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
            String result = "";
            for (String s : matches)
                result = result + s + "\n";
            TextView tv = (TextView) findViewById(R.id.tvRecognizerOutput);
            tv.setText(result);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
