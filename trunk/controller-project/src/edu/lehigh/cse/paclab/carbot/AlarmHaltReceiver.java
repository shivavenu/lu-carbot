package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Whenever we play a DTMF sound, we are responsible for shutting it off at some point. The way we do this is by setting
 * an alarm that is caught by this receiver, which then stops the DTMF tone generator.
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