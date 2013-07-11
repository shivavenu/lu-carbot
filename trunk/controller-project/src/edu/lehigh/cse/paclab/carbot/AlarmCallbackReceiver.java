package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * In many of our codes, we start the robot moving in a certain way, and then we need to shut it off after a little
 * while. Sometimes we need to just stop, sometimes we need to advance a state machine. This receiver forwards to the
 * Activity, so that the correct action can be taken after the right amount of time has passed.
 */
public class AlarmCallbackReceiver extends BroadcastReceiver
{
    /**
     * When we receive an alarm, we just forward to the callback mechanism
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("TestReceiver", "intent=" + intent);
        BasicBotActivityBeta._self.callback();
    }
}