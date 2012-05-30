package edu.lehigh.cse.paclab.carbot;

import android.os.Bundle;
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
        setContentView(R.layout.tetheredbot);
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