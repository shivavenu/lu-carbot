package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * A receiver... this one receives alarms and simply makes toast when they
 * arrive
 */
public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        int i = intent.getExtras().getInt("AlarmID");
        Toast.makeText(context, "Alarm #" + i + " expired", Toast.LENGTH_SHORT).show();
    }

}
