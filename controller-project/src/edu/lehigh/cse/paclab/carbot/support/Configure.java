package edu.lehigh.cse.paclab.carbot.support;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import edu.lehigh.cse.paclab.carbot.R;

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
public class Configure extends BasicBotActivity
{
    SharedPreferences prefs;
    int alarmNum = 0;

    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
    }

    public static Configure self;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        self = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration);
        prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_WRITEABLE);
        EditText et;

        et = (EditText) findViewById(R.id.etConfigureName);
        et.setText(prefs.getString(PREF_TAG_NAME, "KIN-derbot"));
        et = (EditText) findViewById(R.id.etConfigureFarewell);
        et.setText(prefs.getString(PREF_TAG_FAREWELL, "Thank you for letting me come to your class.  I hope you have a great summer!"));
        et = (EditText) findViewById(R.id.etConfigureMeter);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_METER, "5000")));
        et = (EditText) findViewById(R.id.etConfigureRotate);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_ROTATE, "5000")));
        et = (EditText) findViewById(R.id.etConfigureCameraLag);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_CAMLAG, "5000")));
        et = (EditText) findViewById(R.id.etConfigureCameraStartup);
        et.setText("" + Integer.parseInt(prefs.getString(PREF_TAG_CAMSTART, "5000")));
    }

    public void onConfigureClick(View v)
    {
        if (v == findViewById(R.id.btnConfigureNameTest)) {
            EditText et = (EditText) findViewById(R.id.etConfigureName);
            Speak("Hello, my name is " + et.getText().toString());
        }
        if (v == findViewById(R.id.btnConfigureNameSave)) {
            EditText et = (EditText) findViewById(R.id.etConfigureName);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_NAME, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureFarewellTest)) {
            EditText et = (EditText) findViewById(R.id.etConfigureFarewell);
            Speak(et.getText().toString());
        }
        if (v == findViewById(R.id.btnConfigureFarewellSave)) {
            EditText et = (EditText) findViewById(R.id.etConfigureFarewell);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_FAREWELL, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureMeterTest)) {
            EditText et = (EditText) findViewById(R.id.etConfigureMeter);
            int time = Integer.parseInt(et.getText().toString());
            // set a timer for when to stop
            Intent intent = new Intent(this, AlarmStopMovingReceiver.class);
            intent.putExtra("AlarmID", alarmNum);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (time), pendingIntent);
            robotForward();
        }
        if (v == findViewById(R.id.btnConfigureMeterSave)) {
            EditText et = (EditText) findViewById(R.id.etConfigureMeter);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_METER, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureRotateTest)) {
            EditText et = (EditText) findViewById(R.id.etConfigureRotate);
            int time = Integer.parseInt(et.getText().toString());
            // set a timer for when to stop
            Intent intent = new Intent(this, AlarmStopMovingReceiver.class);
            intent.putExtra("AlarmID", alarmNum);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (time), pendingIntent);
            robotClockwise();
        }
        if (v == findViewById(R.id.btnConfigureRotateSave)) {
            EditText et = (EditText) findViewById(R.id.etConfigureRotate);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_ROTATE, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureCameraLagSave)) {
            EditText et = (EditText) findViewById(R.id.etConfigureCameraLag);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_CAMLAG, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureCameraStartupSave)) {
            EditText et = (EditText) findViewById(R.id.etConfigureCameraStartup);
            Editor e = prefs.edit();
            e.putString(PREF_TAG_CAMSTART, et.getText().toString());
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureFrontCamera)) {
            Editor e = prefs.edit();
            e.putString(PREF_TAG_CAMFACE, "FRONT");
            e.commit();
        }
        if (v == findViewById(R.id.btnConfigureRearCamera)) {
            Editor e = prefs.edit();
            e.putString(PREF_TAG_CAMFACE, "REAR");
            e.commit();
        }
    }
}
