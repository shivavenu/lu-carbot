package edu.lehigh.cse.paclab.carbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.BasicBotActivityBeta;

public class AlarmEndToneReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("TestReceiver", "intent=" + intent);
		Toast.makeText(context, "End Tone", Toast.LENGTH_SHORT).show();
		BasicBotActivityBeta._toneGenerator.stopTone();
	}
	
}