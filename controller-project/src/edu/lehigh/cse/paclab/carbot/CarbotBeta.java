package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;

/**
 * This is the new framework for Carbot. Eventually we will deprecate and then remove everything else.
 */
public class CarbotBeta extends Activity
{
    /**
     * The main activity is just a menu, so we don't have much to do here... just call super and then draw the screen
     */
	PowerManager.WakeLock wl;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
    	wl.acquire();
    	Log.e("Carbot", "wakelock acquired");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.carbot_beta);
    }
    
    public void onPause(Bundle savedInstanceState){
    	Log.e("Carbot", "wakelock released");
    	wl.release();
    }

    /**
     * On any button press, we simply launch a new activity
     * 
     * @param v
     *            A reference to the button that was pressed.
     */
    public void launchActivity(View v)
    {
        if (v == findViewById(R.id.btnTetheredBot))
            startActivity(new Intent(this, TetheredBotBeta.class));
        if (v == findViewById(R.id.btnColorDetection))
            startActivity(new Intent(this, ColorDetectionActivity.class));
        if (v == findViewById(R.id.btnRCReceiver))
            startActivity(new Intent(this, RCReceiverActivity.class));
        if (v == findViewById(R.id.btnRCSender))
            startActivity(new Intent(this, RCSenderActivity.class));
        if (v == findViewById(R.id.btnConfig))
            startActivity(new Intent(this, Configure.class));
        if (v == findViewById(R.id.btnDraw))
            startActivity(new Intent(this, DrawActivity.class));
        if (v == findViewById(R.id.btnTalking))
            startActivity(new Intent(this, Chatterbox.class));
    }
}