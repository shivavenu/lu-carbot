package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A receiver that is used after snapping a photo to ensure enough time has
 * passed for the sdcard to be updated with the new image
 * 
 * [mfs] I know this is bad design, but we can eliminate the redundant receivers
 * later
 * 
 * @author spear
 * 
 */
public class AlarmSnapPhotoReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        SnapPhoto.self.onAlarm();
    }
}
