package edu.lehigh.cse.paclab.prelims;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmStopMovingReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
    	// we should just stop the robot here
    	// stopRobot();

    	// for the time being, we'll print a message
        int i = intent.getExtras().getInt("AlarmID");
        Toast.makeText(context, "Alarm #" + i + " expired", Toast.LENGTH_SHORT).show();
        
		// the right way to handle this is to send an intent that the caller can
		// catch. When it catches the intent, it knows the alarm expired, and it
		// can do whatever it wants.
        Intent rv = new Intent("ALARM_TRIGGER");
        context.sendBroadcast(rv);
    }
}
