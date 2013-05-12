package edu.lehigh.cse.paclab.carbot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * An activity for saving configuration information, specifically the text that the robot should say as its name and
 * farewell message, and the time it takes the robot to move in standard ways (distance, rotation)
 */
public class Configure extends BasicBotActivityBeta
{
    /**
     * A counter for disambiguating alarms
     */
    int               alarmNum = 0;

    /**
     * Lifecycle method to create this activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // set the layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_beta);

        // update all text boxes to show our current values
        EditText et;
        et = (EditText) findViewById(R.id.etCfgName);
        et.setText(prefs.getString(PREFS_NAME, "KIN-derbot"));
        et = (EditText) findViewById(R.id.etCfgBye);
        et.setText(prefs.getString(PREFS_BYE,
                "Thank you for letting me come to your class.  I hope you have a great summer!"));
        et = (EditText) findViewById(R.id.etCfgDist);
        et.setText("" + Integer.parseInt(prefs.getString(PREFS_DIST, "5000")));
        et = (EditText) findViewById(R.id.etCfgRot);
        et.setText("" + Integer.parseInt(prefs.getString(PREFS_ROT, "5000")));
    }

    /**
     * All buttons in the layout are set up to call this when they are clicked
     * 
     * @param v
     *            The View (Button) that was clicked
     */
    public void onConfigClick(View v)
    {
        // test a name by speaking it
        if (v == findViewById(R.id.btnCfgNameTest)) {
            EditText et = (EditText) findViewById(R.id.etCfgName);
            speak("Hello, my name is " + et.getText().toString());
        }
        // save the name
        if (v == findViewById(R.id.btnCfgNameSave)) {
            EditText et = (EditText) findViewById(R.id.etCfgName);
            Editor e = prefs.edit();
            e.putString(PREFS_NAME, et.getText().toString());
            e.commit();
        }
        // test a farewell message by speaking it
        if (v == findViewById(R.id.btnCfgByeTest)) {
            EditText et = (EditText) findViewById(R.id.etCfgBye);
            speak(et.getText().toString());
        }
        // save the farewell message
        if (v == findViewById(R.id.btnCfgByeSave)) {
            EditText et = (EditText) findViewById(R.id.etCfgBye);
            Editor e = prefs.edit();
            e.putString(PREFS_BYE, et.getText().toString());
            e.commit();
        }
        // test the distance value by starting the robot, and stopping it in the specified time
        if (v == findViewById(R.id.btnCfgDistTest)) {
            // figure out the requested stop time
            EditText et = (EditText) findViewById(R.id.etCfgDist);
            long time = Integer.parseInt(et.getText().toString());
            // ask for an alarm to stop the robot at that time
            requestStop(time);
            // start moving
            robotForward();
        }
        // save the distance time
        if (v == findViewById(R.id.btnCfgDistSave)) {
            EditText et = (EditText) findViewById(R.id.etCfgDist);
            Editor e = prefs.edit();
            e.putString(PREFS_DIST, et.getText().toString());
            e.commit();
        }
        // test the rotation value by rotating the robot, and stopping it in the specified time
        if (v == findViewById(R.id.btnCfgRotTest)) {
            // figure out stop time
            EditText et = (EditText) findViewById(R.id.etCfgRot);
            long time = Integer.parseInt(et.getText().toString());
            // ask for an alarm to stop the robot at that time
            requestStop(time);
            // start spinning
            robotClockwise();
        }
        // save the rotation time
        if (v == findViewById(R.id.btnCfgRotTest)) {
            EditText et = (EditText) findViewById(R.id.etCfgRot);
            Editor e = prefs.edit();
            e.putString(PREFS_ROT, et.getText().toString());
            e.commit();
        }
    }

    /**
     * Set up an alarm to stop the robot
     * 
     * @param when
     *            The number of milliseconds in the future at which time the robot should stop
     */
    private void requestStop(long when)
    {
        Intent intent = new Intent(this, AlarmHaltReceiver.class);
        intent.putExtra("AlarmID", alarmNum);
        PendingIntent pi = PendingIntent.getBroadcast(this, alarmNum++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + when, pi);
    }

    /**
     * Provide an empty callback method, so that we are compatible with AlarmCallbackReceiver
     */
    public void callback()
    {
    }
}