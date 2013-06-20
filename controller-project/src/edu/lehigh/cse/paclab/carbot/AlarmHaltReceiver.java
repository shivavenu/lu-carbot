package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This alarm causes us to issue a 'halt' command.
 * 
 * TODO: We should roll this into the callback mechanism, and be done with multiple AlarmReceiver objects.
 */
public class AlarmHaltReceiver extends BroadcastReceiver
{
    /**
     * When we receive an alarm, we put a message in the log and we stop the DTMF tone generator.
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("TestReceiver", "intent=" + intent);
        BasicBotActivityBeta._self.robotStop();
    }
}