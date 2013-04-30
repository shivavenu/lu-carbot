package edu.lehigh.cse.paclab.carbot.support;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A receiver that is used during Configuration to stop the robot after an alarm
 * expires
 * 
 * [mfs] I know this is bad design, but we can eliminate the redundant receivers
 * later
 * 
 * @author mfs
 * 
 */
public class AlarmStopMovingReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Configure.self.robotStop();
    }
}
