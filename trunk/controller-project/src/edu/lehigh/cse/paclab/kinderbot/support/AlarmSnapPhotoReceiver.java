package edu.lehigh.cse.paclab.kinderbot.support;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmSnapPhotoReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        SnapPhoto.self.onAlarm();
    }
}
