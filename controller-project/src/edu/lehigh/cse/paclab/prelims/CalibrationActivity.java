package edu.lehigh.cse.paclab.prelims;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.R;

public class CalibrationActivity extends Activity {

	int alarmNum = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// start by calling parent method
		super.onCreate(savedInstanceState);

		// draw the screen
		setContentView(R.layout.calibrationlayout);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String fwdVal = prefs.getString("FWD", "1");
        String ccwVal = prefs.getString("CCW", "1");
        
        EditText et = (EditText)findViewById(R.id.etFWDTime);
        et.setText(fwdVal);
        et = (EditText)findViewById(R.id.etCCWTime);
        et.setText(ccwVal);

        registerReceiver(alarmreply, new IntentFilter("ALARM_TRIGGER"));
	
	}

	public void onCalibrateFWD(View v) 
	{
		// get the time
		EditText et = (EditText)findViewById(R.id.etFWDTime);
		float time = Float.parseFloat(et.getText().toString());

		// save it as the new calibrated time
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        Editor e = prefs.edit();
        e.putString("FWD", ""+time);
        e.commit();
        
        // start the robot
		Message msg = Message.obtain();
		try {
			Bundle bundle = new Bundle();
			bundle.putString("CMD", "FWD");
			msg.setData(bundle);
			messenger.send(msg);
		} catch (RemoteException eee) {
			eee.printStackTrace();
		}
        
        // set a timer for when to stop
        Intent intent = new Intent(this, AlarmStopMovingReceiver.class);
        intent.putExtra("AlarmID", alarmNum);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long)(time * 1000), pendingIntent);
        
        // toast will be unnecessary once robot starts moving
        Toast.makeText(this, "Alarm set in " + time + " seconds", Toast.LENGTH_SHORT).show();
	}

	public void onCalibrateCCW(View v) 
	{
		// get the time
		EditText et = (EditText)findViewById(R.id.etCCWTime);
		float time = Float.parseFloat(et.getText().toString());

		// save it as the new calibrated time
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        Editor e = prefs.edit();
        e.putString("CCW", ""+time);
        e.commit();

        // start the robot
        // instructRobot(CCW);
        
        // set a timer for when to stop
        Intent intent = new Intent(this, AlarmStopMovingReceiver.class);
        intent.putExtra("AlarmID", alarmNum);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long)(time * 1000), pendingIntent);
        
        // toast will be unnecessary once robot starts moving
        Toast.makeText(this, "Alarm set in " + time + " seconds", Toast.LENGTH_SHORT).show();
	}

	Messenger messenger = null;

	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			Bundle data = message.getData();
			if (message.arg1 == RESULT_OK && data != null) {
				String text = data.getString(ArduinoService.RETURNVAL);
				Toast.makeText(CalibrationActivity.this, text, Toast.LENGTH_LONG).show();
			}
		}
	};
	private ServiceConnection conn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			messenger = new Messenger(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			messenger = null;
		}
	};

	protected void onResume() {
		super.onResume();
		Toast.makeText(this, "OnResume called", Toast.LENGTH_SHORT).show();
		Intent intent = null;
		intent = new Intent(this, ArduinoService.class);
		// Create a new Messenger for the communication back
		// From the Service to the Activity
		Messenger messenger = new Messenger(handler);
		intent.putExtra("MESSENGER", messenger);
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(conn);
	}

	public void onClickHack(View view) {
		String action = "";
		if (view == findViewById(R.id.btnHackFwd))
			action = "FWD";
		if (view == findViewById(R.id.btnHackConfig))
			action = "INIT";
		if (view == findViewById(R.id.btnHackRev))
			action = "REV";
		if (view == findViewById(R.id.btnHackStop))
			action = "STOP";
		
		Message msg = Message.obtain();

		try {
			Bundle bundle = new Bundle();
			bundle.putString("CMD", action);
			msg.setData(bundle);
			messenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private BroadcastReceiver alarmreply = new BroadcastReceiver(){
		public void onReceive(Context c, Intent i)
		{
			Message msg = Message.obtain();
			try {
				Bundle bundle = new Bundle();
				bundle.putString("CMD", "STOP");
				msg.setData(bundle);
				messenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
}