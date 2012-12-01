package edu.lehigh.cse.paclab.carbot.support;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import static android.media.ToneGenerator.TONE_DTMF_D;
import static android.media.ToneGenerator.TONE_DTMF_1;
import static android.media.ToneGenerator.TONE_DTMF_2;
import static android.media.ToneGenerator.TONE_DTMF_3;
import static android.media.ToneGenerator.TONE_DTMF_4;
import static android.media.ToneGenerator.TONE_DTMF_5;
import static android.media.ToneGenerator.TONE_DTMF_6;

/**
 * TODO:
 * (1) Create the abstract methods for handling Standard Socket as well as any member or static variables
 * (2) Fill in methods for TTS
 * (3) Use Wi-fi socket for remote
 * (4) Remove "connected" pop-up, use either toast or background icon.
 * @author ArmonShariati
 *
 */
public abstract class BasicBotActivityBeta extends Activity {
	public static final String TAG = "Carbot Beta";
	
	static public final int CHECK_TTS = 99873;
	
	static final ToneGenerator _toneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, 100);
	
	private AlarmManager am;
	
	//AudioManager audioManager;
	
	public void onCreate(Bundle savedInstanceState)
	{
		 super.onCreate(savedInstanceState);
	     Log.e(TAG, "Call to BasicBotActivityBeta::onCreate");
	}
	
	public void setAlarm(){
		
		Context context = this;
		Intent intent = new Intent(context, AlarmEndToneReceiver.class);
		
		//remember to delete the older alarm before creating the new one
		PendingIntent pi = PendingIntent.getBroadcast(this,
				1, //the request id, used for disambiguating this intent
				intent,
				0); //pending intent flags
		
		am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pi);
				
	}
	
	public void robotForward() {
		_toneGenerator.startTone(TONE_DTMF_1);
		Toast.makeText(this, "Forward", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "forward: emitting");
	}

	public void robotReverse() {
		_toneGenerator.startTone(TONE_DTMF_2);
		Toast.makeText(this, "Reverse", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "reverse: emitting");
	}

	public void robotCounterClockwise() {
		_toneGenerator.startTone(TONE_DTMF_3);
		Toast.makeText(this, "CCW", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "c_clockwise: emitting");
	}

	public void robotClockwise() {
		_toneGenerator.startTone(TONE_DTMF_4);
		Toast.makeText(this, "CW", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "clockwise: emitting");
	}

	public void robotPointTurnLeft() {
		_toneGenerator.startTone(TONE_DTMF_5);
		Toast.makeText(this, "Left", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "pointTurnLeft: emitting");
	}

	public void robotPointTurnRight() {
		_toneGenerator.startTone(TONE_DTMF_6);
		Toast.makeText(this, "Right", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "pointTurnRight: emitting");
	}
	
	public void robotStop() {
		_toneGenerator.startTone(TONE_DTMF_D);
		Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
		setAlarm();
		Log.e(TAG, "stopBot: emitting");
	}
	
	void initTTS() {
		
	}
	
	void closeTTS() {
	
	}
	
	void speak(String s) {
		
	}
	
	void playCustomSound() {

	}
	//required for OnInitListener... what should I use this for?
	void onInit(int status) {
	}
}
