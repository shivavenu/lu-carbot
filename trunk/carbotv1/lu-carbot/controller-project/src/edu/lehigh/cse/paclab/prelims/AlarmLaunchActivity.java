package edu.lehigh.cse.paclab.prelims;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.R;

/**
 * A simple example of how to launch alarms. Note that this goes hand-in-hand
 * with the AlarmReceiver class. The two work in tandem.
 */
public class AlarmLaunchActivity extends Activity
{
    int alarmNum = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarmlayout);
    }

    public void onLaunchAlarm(View v)
    {
        EditText text = (EditText) findViewById(R.id.etSeconds);
        int i = Integer.parseInt(text.getText().toString());
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("AlarmID", alarmNum);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (i * 1000), pendingIntent);
        Toast.makeText(this, "Alarm set in " + i + " seconds", Toast.LENGTH_SHORT).show();
    }
}
