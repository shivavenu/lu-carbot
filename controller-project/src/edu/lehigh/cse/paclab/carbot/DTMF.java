package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;
import android.media.ToneGenerator;
import android.media.AudioManager;

import static android.media.ToneGenerator.TONE_DTMF_0;
import static android.media.ToneGenerator.TONE_DTMF_1;
import static android.media.ToneGenerator.TONE_DTMF_2;
import static android.media.ToneGenerator.TONE_DTMF_3;
import static android.media.ToneGenerator.TONE_DTMF_4;
import static android.media.ToneGenerator.TONE_DTMF_5;

public class DTMF extends Activity{
private static final String TAG = "DTMF";
	
	ToggleButton forward;
	ToggleButton reverse;
	ToggleButton c_clockwise;
	ToggleButton clockwise;
	ToggleButton pointTurnLeft;
	ToggleButton pointTurnRight;
	
	AudioManager audioManager;
	
	static final ToneGenerator _toneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, 100);
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dtmf);
        
        
        forward = (ToggleButton)findViewById(R.id.forward);
		reverse = (ToggleButton)findViewById(R.id.reverse);
		c_clockwise = (ToggleButton)findViewById(R.id.c_clockwise);
		clockwise = (ToggleButton)findViewById(R.id.clockwise);
		pointTurnLeft = (ToggleButton)findViewById(R.id.pointTurnLeft);
		pointTurnRight = (ToggleButton)findViewById(R.id.pointTurnRight);
		
    }
    
    public void onPause() {
		super.onPause();
	}
    
    public void onDestroy() {
		super.onDestroy();
	}
    
    public void forward(View v){
    	 
		if(!forward.isChecked()){
			_toneGenerator.stopTone();
			Log.e(TAG, "forward: not emitting");
		}
		else{
			_toneGenerator.startTone(TONE_DTMF_0);
			Log.e(TAG, "forward: emitting");
		}
	}
    
    public void reverse(View v){
   	 
		if(!reverse.isChecked()){
			_toneGenerator.stopTone();
			Log.e(TAG, "reverse: not emitting");
		}
		else{
			_toneGenerator.startTone(TONE_DTMF_1);
			Log.e(TAG, "reverse: emitting");
		}
	}
    
    public void c_clockwise(View v){
      	 
		if(!c_clockwise.isChecked()){
			_toneGenerator.stopTone();
			Log.e(TAG, "c_clockwise: not emitting");
		}
		else{
			_toneGenerator.startTone(TONE_DTMF_2);
			Log.e(TAG, "c_clockwise: emitting");
		}
	}
    
    public void clockwise(View v){
      	 
		if(!clockwise.isChecked()){
			_toneGenerator.stopTone();
			Log.e(TAG, "clockwise: not emitting");
		}
		else{
			_toneGenerator.startTone(TONE_DTMF_3);
			Log.e(TAG, "clockwise: emitting");
		}
	}
    
    public void pointTurnLeft(View v){
      	 
		if(!pointTurnLeft.isChecked()){
			_toneGenerator.stopTone();
			Log.e(TAG, "pointTurnLeft: not emitting");
		}
		else{
			_toneGenerator.startTone(TONE_DTMF_4);
			Log.e(TAG, "pointTurnLeft: emitting");
		}
	}
    
    public void pointTurnRight(View v){
      	 
		if(!pointTurnRight.isChecked()){
			_toneGenerator.stopTone();
			Log.e(TAG, "pointTurnRight: not emitting");
		}
		else{
			_toneGenerator.startTone(TONE_DTMF_5);
			Log.e(TAG, "pointTurnRight: emitting");
		}
	}

}
