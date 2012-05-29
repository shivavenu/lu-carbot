package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import edu.lehigh.cse.paclab.carbot.support.AlarmStopMovingReceiver;
import edu.lehigh.cse.paclab.carbot.support.BasicBotActivity;

/**
 * For configuring the robot.
 * 
 * What do we configure?
 * 
 * -- give it a name
 * 
 * -- configure the time for a 360 degree rotation
 * 
 * -- configure the time to travel one meter
 * 
 * -- configure the shutter lag/save time
 * 
 * -- configure the camera startup time lag
 * 
 * @author spear
 * 
 */
public class ConfigurationActivity extends BasicBotActivity
{
    SharedPreferences prefs;
    int alarmNum = 0;
    
    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
    }

    public static ConfigurationActivity self;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        self = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configurationlayout);
        prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_WRITEABLE);
        EditText et;

        et = (EditText) findViewById(R.id.etKinderConfigName);
        et.setText(prefs.getString(PREF_TAG_NAME, "KIN-derbot"));
        et = (EditText) findViewById(R.id.etKinderConfigMeter);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_METER, "5000")));
        et = (EditText) findViewById(R.id.etKinderConfigRotate);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_ROTATE, "5000")));
        et = (EditText) findViewById(R.id.etKinderConfigCameraLag);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_CAMLAG, "5000")));
        et = (EditText) findViewById(R.id.etKinderConfigCameraStartup);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_CAMSTART, "5000")));
    }

    public void onKinderConfigClick(View v)
    {
        if (v == findViewById(R.id.btnKinderConfigNameTest)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigName);
            Speak("Hello, my name is " + et.getText().toString());
        }
        if (v == findViewById(R.id.btnKinderConfigNameSave)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigName);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_NAME, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnKinderConfigMeterTest)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigMeter);
            int time = Integer.parseInt(et.getText().toString());
            // set a timer for when to stop
            Intent intent = new Intent(this, AlarmStopMovingReceiver.class);
            intent.putExtra("AlarmID", alarmNum);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (time), pendingIntent);
            robotForward();
        }
        if (v == findViewById(R.id.btnKinderConfigMeterSave)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigMeter);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_METER, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnKinderConfigRotateTest)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigRotate);
            int time = Integer.parseInt(et.getText().toString());
            // set a timer for when to stop
            Intent intent = new Intent(this, AlarmStopMovingReceiver.class);
            intent.putExtra("AlarmID", alarmNum);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (time), pendingIntent);
            robotClockwise();
        }
        if (v == findViewById(R.id.btnKinderConfigRotateSave)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigRotate);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_ROTATE, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnKinderConfigCameraLagSave)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigCameraLag);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_CAMLAG, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnKinderConfigCameraStartupSave)) {
            EditText et = (EditText) findViewById(R.id.etKinderConfigCameraStartup);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_CAMSTART, et.getText().toString());
            e.commit();
        }
    }
}

