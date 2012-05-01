package edu.lehigh.cse.paclab.carbot;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class TalkBackActivity extends Activity implements OnInitListener
{
    private final int CHECK_TTS = 99873;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.talkbacklayout);

        // check if text-to-speech is supported, via an intent:
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, CHECK_TTS);

    }

    private TextToSpeech mTts;

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CHECK_TTS) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(this, this);
            }
            else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
            if (mTts.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_COUNTRY_AVAILABLE)
                mTts.setLanguage(Locale.US);
        }
    }

    @Override
    public void onDestroy()
    {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status)
    {
        Toast.makeText(this, "TTS onInit returned " + status, Toast.LENGTH_SHORT).show();
        mTts.speak("Hey there, dude!", TextToSpeech.QUEUE_FLUSH, null);
    }

    public void sayIt(View v)
    {
        EditText et = (EditText) findViewById(R.id.etWordsToSay);
        mTts.speak(et.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
    }

}