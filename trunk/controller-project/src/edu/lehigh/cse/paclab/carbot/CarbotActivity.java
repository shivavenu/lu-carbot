package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * This is the main activity for now. Its only job is to let us launch the other
 * activities that we have made, so that we can test the varios components
 */
public class CarbotActivity extends Activity
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // draw the screen
        setContentView(R.layout.mainlayout);
    }

    /**
     * Every button in the activity is wired to this, and the button that was
     * clicked is passed as the parameter. We can then compare the parameter to
     * the known buttons to know which activity to launch
     * 
     * @param v
     *            The button that was pressed
     */
    public void launchActivity(View v)
    {
        if (v == findViewById(R.id.btnFaceCapture)) {
            startActivity(new Intent(this, FaceCaptureActivity.class));
        }
        else if (v == findViewById(R.id.btnTalkBack)) {
            startActivity(new Intent(this, TalkBackActivity.class));
        }
        else if (v == findViewById(R.id.btnVoiceRecognizer)) {
            startActivity(new Intent(this, VoiceRecognizerActivity.class));
        }
        else if (v == findViewById(R.id.btnBTChat)) {
            startActivity(new Intent(this, BlueToothActivity.class));
        }
        else if (v == findViewById(R.id.btnTouchDraw)) {
            startActivity(new Intent(this, TouchDrawActivity.class));
        }
        else if (v == findViewById(R.id.btnOnOffControl)) {
            startActivity(new Intent(this, OnOffControllerActivity.class));
        }
        else if (v == findViewById(R.id.btnCameraPreview)) {
            startActivity(new Intent(this, CameraPreviewActivity.class));
        }
        else if (v == findViewById(R.id.btnRCSender)) {
            startActivity(new Intent(this, RCSenderActivity.class));
        }
        else if (v == findViewById(R.id.btnRCReceiver)) {
            startActivity(new Intent(this, RCReceiverActivity.class));
        }
        else if (v == findViewById(R.id.btnBTZap)) {
            startActivity(new Intent(this, BTZapActivity.class));
        }
    }
}
