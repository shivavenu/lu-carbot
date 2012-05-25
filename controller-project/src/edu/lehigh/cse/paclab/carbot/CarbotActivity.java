package edu.lehigh.cse.paclab.carbot;

/**
 * The main TODO we have to cover before we can build out the bot controllers regards the service.  In particular, we need to:
 * 
 * 1 - make sure we can access it from multiple activities without recreating it
 * 
 * 2 - get the configuration correct... right now we need to configure twice, for some reason
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import edu.lehigh.cse.paclab.carbot.services.TTSManager;

/**
 * This is the main activity for now. Its only job is to let us launch the other
 * activities that we have made, so that we can test the various components
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

        // configure the TTS Service
        TTSManager.configure(this);
    }

    /**
     * Whenever an intent comes to this Activity, it is handled in this code
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // filter TTS events.. TTS events only occur during TTS configuration,
        // and TTS configuration should *only* happen from this code
        if (TTSManager.handleIntent(requestCode, resultCode, data))
            return;

        // otherwise handle the intent according to the behaviors of this
        // specific Activity:

        // ...
    }

    /**
     * We explicitly close the app, we should shut down any services that we
     * started...
     */
    @Override
    public void onStop()
    {
        TTSManager.shutdown();
        super.onStop();
    }

    public void launchActivity(View v)
    {
        if (v == findViewById(R.id.btnLaunchDemos)) {
            TTSManager.sayIt("I can talk!"); // just to show how we can use the
                                             // service from this Activity...
            startActivity(new Intent(this, edu.lehigh.cse.paclab.prelims.DemosActivity.class));
        }
        if (v == findViewById(R.id.btnLaunchR2Demos)) {
            TTSManager.sayIt("I can talk!"); // just to show how we can use the
                                             // service from this Activity...
            startActivity(new Intent(this, RoundTwoDemos.class));
        }
    }

    /*
     * Plan from here:
     * 
     * - There will be many different behaviors that we want the robot to
     * support
     * 
     * - Simple control via buttons - This will require two phones, one to talk
     * to the other.
     * 
     * - We will want the buttons to make the phone move, and we will also want
     * a 'snap picture' button
     * 
     * - Key challenge here: is our BlueTooth code hardened, or else is WiFi
     * going to work?
     * 
     * - Simple control by talking
     * 
     * - Basically the same as the previous project
     * 
     * - Speech recognition for kids is really hard
     * 
     * - Controlling phone needs to have internet access for SpeechToText API to
     * work
     * 
     * - Control by drawing on the screen
     * 
     * - This will require one phone mounted nicely on the robot
     * 
     * - Which drawing mechanism do we prefer here?
     * 
     * - Calibration is going to be very important
     * 
     * - Need to harden this wrt cancelling motion and resetting
     * 
     * - Game mode: find the ball
     * 
     * - This will require one phone
     * 
     * - First requirement is to teach the phone which ball to look for. I think
     * the histograms will help.
     * 
     * - Second is to integrate vision code with robot control so that we can
     * move around until we find the ball
     * 
     * - Stopping is going to be a challenge
     * 
     * - Key underlying features
     * 
     * - TextToSpeech
     * 
     * - We want the robot to talk as much as possible
     * 
     * - Hardened ArduinoService
     * 
     * - So that we can register with a robot once
     * 
     * - See
     * http://stackoverflow.com/questions/2621395/more-efficient-way-of-updating
     * -ui-from-service-than-intents/2622473#2622473, or else use an AIDL to
     * create the connection to the service
     * (http://www.helloandroid.com/tutorials/musicdroid-audio-player-part-ii)
     * 
     * - Hardened Phone2Phone communication
     * 
     * - Bluetooth or Wifi... doesn't matter
     * 
     * - Key thing is that we need a connection that won't drop
     * 
     * - Calibration support
     * 
     * - Need to calibrate the Arduino controller so that we get speed and
     * angles right.
     * 
     * - Need to verify that the alarmservice resolution is appropriate...
     * otherwise we'll need to have threads in spinloops
     * 
     * - Need to calibrate the camera to the ball being searched, based on
     * lighting
     * 
     * 
     * - Update on histogramming from John Spletzer
     * 
     * I wouldn't do histograms, but you can do what you want. I think the
     * easiest thing would be to:
     * 
     * 1. Take a patch that you know will contain portions of the ball
     * 
     * 2. Calculate a mean & std dev for each channel (H-S-V) from the patch
     * 
     * 3. Establish min & max values for each channel (e.g., mean +/- 3 sigma).
     * This is your color model.
     * 
     * 4. Over each pixel in the new image do:
     * 
     * Ball = H_val>H_min && H_val<H_max && S_val>S_min && S_val<S_max &&
     * V_val>V_min && V_val<V_max;
     * 
     * There are ways to speed this up using LUTs, etc., but that's another
     * story. I'll warn you though that since the camera you are using will
     * likely dynamically adjust brightness, exposure, gain, shutter, etc., that
     * a simple technique may not give great results regardless. However, I'd
     * start here.
     */
}