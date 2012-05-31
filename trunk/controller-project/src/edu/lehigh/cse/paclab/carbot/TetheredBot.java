package edu.lehigh.cse.paclab.carbot;

import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import edu.lehigh.cse.paclab.carbot.support.BasicBotActivity;

/**
 * Simplest control mechanism: this phone is plugged into the Robot, and we
 * control the robot via buttons on the phone.
 * 
 * Note that in this case, we don't need any special Bluetooth stuff
 */
public class TetheredBot extends BasicBotActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // pick tablet or phone layout
        // Note: tablet is 800x1232, phone is 480x800
        Display display = getWindowManager().getDefaultDisplay();  
        int width = display.getWidth();
        int height = display.getHeight();
        if (width > 700 || height > 900)
            setContentView(R.layout.tetheredbot_tablet);
        else
            setContentView(R.layout.tetheredbot);
        Log.v("CARBOT", "width, height = " + width + " " + height); 

        
        initBTStatus();
    }

    public void onClickImage(View v)
    {
        if (v == findViewById(R.id.ivTetherForward)) {
            robotForward();
        }
        if (v == findViewById(R.id.ivTetherReverse)) {
            robotReverse();
        }
        // if we are going forward, a simple PTL doesn't suffice
        if (v == findViewById(R.id.ivTetherLeft)) {
            robotStop();
            robotPointTurnLeft();
        }
        // if we are going forward, a simple PTR doesn't suffice
        if (v == findViewById(R.id.ivTetherRight)) {
            robotStop();
            robotPointTurnRight();
        }
        if (v == findViewById(R.id.ivTetherRotPos)) {
            robotClockwise();
        }
        if (v == findViewById(R.id.ivTetherRotNeg)) {
            this.robotCounterclockwise();
        }
        if (v == findViewById(R.id.ivTetherStop)) {
            robotStop();
        }
    }

    /**
     * NB: required but unused in this app... we don't use bluetooth!
     */
    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
    }
}