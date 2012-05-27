package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A receiver that is used when searching for the Balloon to know when to stop
 * rotating and to start looking again
 * 
 * [mfs] I know this is bad design, but we can eliminate the redundant receivers
 * later
 * 
 * @author mfs
 * 
 */
public class AlarmLookAgainReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // [mfs] This is clearly not correct!
        ConfigurationActivity.self.robotStop();
    }
}
