package edu.lehigh.cse.paclab.prelims;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import edu.lehigh.cse.paclab.carbot.R;
import edu.lehigh.cse.paclab.carbot.services.TTSManager;

/**
 * This is the main activity for now. Its only job is to let us launch the other
 * activities that we have made, so that we can test the varios components
 */
public class DemosActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // draw the screen
        setContentView(R.layout.demoslayout);
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
        // show how we can use Services from multiple activities... note that
        // this will cancel any previous TTS
        TTSManager.sayIt("You clicked a button in DemoActivity");

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
        else if (v == findViewById(R.id.btnStreamCapture)) {
            startActivity(new Intent(this, StreamCaptureActivity.class));
        }
        else if (v == findViewById(R.id.btnAlarmActivityLaunch)) {
            startActivity(new Intent(this, AlarmLaunchActivity.class));
        }
        else if (v == findViewById(R.id.btnLaunchWifiServer)) {
            startActivity(new Intent(this, WifiServerActivity.class));
        }
        else if (v == findViewById(R.id.btnLaunchWifiClient)) {
            startActivity(new Intent(this, WifiClientActivity.class));
        }
        else if (v == findViewById(R.id.btnLaunchJoystick)) {
            startActivity(new Intent(this, JoystickActivity.class));
        }
        else if (v == findViewById(R.id.btnLaunchWalkablePath)) {
            startActivity(new Intent(this, WalkablePathActivity.class));
        }
        else if (v == findViewById(R.id.btnDriveTheBot)) {
            startActivity(new Intent(this, DriveTheBotActivity.class));
        }
        else if (v == findViewById(R.id.btnCalibrate)) {
            startActivity(new Intent(this, CalibrationActivity.class));
        }
        else if (v == findViewById(R.id.btnVision)) {
            startActivity(new Intent(this, VisionActivity.class));
        }
    }
}
