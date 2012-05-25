package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.lehigh.cse.paclab.carbot.services.ArduinoManager;
import edu.lehigh.cse.paclab.carbot.services.BluetoothManager;
import edu.lehigh.cse.paclab.carbot.services.TTSManager;

public class RoundTwoDemos extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // draw the screen
        setContentView(R.layout.roundtwolayout);

        // configure the TTS Service
        TTSManager.configure(this);

        // initialize the Bluetooth singleton
        BluetoothManager.initialize(TTSManager.getCachedApplicationContext());

        // initialize the Arduino singleton
        ArduinoManager.initialize(TTSManager.getCachedApplicationContext());
        
        // configure Bluetooth
        BluetoothManager.configure(this);
        
        ArduinoManager.configure(this);
    }

    /**
     * For now, all we do is launch the demos, but soon we'll do real things
     * instead...
     * 
     * @param v
     *            The button that was pressed
     */
    public void launchActivity(View v)
    {
        if (v == findViewById(R.id.btnLaunchBallLearn)) {
            startActivity(new Intent(this, edu.lehigh.cse.paclab.carbot.BallLearnActivity.class));
        }
        if (v == findViewById(R.id.btnLaunchBallFind)) {
            startActivity(new Intent(this, edu.lehigh.cse.paclab.carbot.BallFindActivity.class));
        }
        if (v == findViewById(R.id.btnLaunchBTRemoteControl)) {
            startActivity(new Intent(this, edu.lehigh.cse.paclab.carbot.BTRemoteControl.class));
        }
        if (v == findViewById(R.id.btnLaunchBTBotDriver)) {
            startActivity(new Intent(this, edu.lehigh.cse.paclab.carbot.BTBotDriver.class));
        }
        if (v == findViewById(R.id.btnDrive)) {
            startActivity(new Intent(this, edu.lehigh.cse.paclab.carbot.DriveTheBotActivity.class));
        }
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
        BluetoothManager.shutdown();
        ArduinoManager.shutdown();
        super.onStop();
    }

    /** Draw our menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.carbotmenu, menu);
        return true;
    }

    /** This runs when a menu item is clicked */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menuConnectArduino1:
                ArduinoManager.config();
                return true;
            case R.id.menuConnectArduino2:
                ArduinoManager.sendCommand("INIT");
                return true;
        }
        return false;
    }

}
