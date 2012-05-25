package edu.lehigh.cse.paclab.kinderbot;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;
import edu.lehigh.cse.paclab.carbot.R;
import edu.lehigh.cse.paclab.kinderbot.support.BasicBotActivity;

/**
 * Simplest control mechanism: this phone is plugged into the Robot, and we
 * control the robot via buttons on the phone
 */
public class DriveTheBotActivity extends BasicBotActivity
{

    ToggleButton forward;
    ToggleButton reverse;
    ToggleButton clockwise;
    ToggleButton counterClockwise;
    ToggleButton pTurnLeft;
    ToggleButton pTurnRight;



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drivethebotlayout);

        forward = (ToggleButton) findViewById(R.id.button1);
        reverse = (ToggleButton) findViewById(R.id.button2);
        clockwise = (ToggleButton) findViewById(R.id.button3);
        counterClockwise = (ToggleButton) findViewById(R.id.button4);
        pTurnRight = (ToggleButton) findViewById(R.id.button5);
        pTurnLeft = (ToggleButton) findViewById(R.id.button6);
    }

    /**
     * The last few methods dictate what command will be sent based on what
     * button is activated by the user
     */
    public void forward(View V)
    {
        if (!forward.isChecked())
            sendCommand((byte) 0);
        else
            sendCommand((byte) 1);
    }

    public void reverse(View V)
    {
        if (!reverse.isChecked())
            sendCommand((byte) 0);
        else
            sendCommand((byte) 2);
    }

    public void clockwise(View V)
    {
        if (!clockwise.isChecked())
            sendCommand((byte) 0);
        else
            sendCommand((byte) 3);
    }

    public void cclockwise(View V)
    {
        if (!counterClockwise.isChecked())
            sendCommand((byte) 0);
        else
            sendCommand((byte) 4);
    }

    public void pointTurnR(View V)
    {
        if (!pTurnRight.isChecked())
            sendCommand((byte) 0);
        else
            sendCommand((byte) 5);
    }

    public void pointTurnL(View V)
    {
        if (!pTurnLeft.isChecked())
            sendCommand((byte) 0);
        else
            sendCommand((byte) 6);
    }

    /**
     * NB: required but unused in this app... we don't use bluetooth!
     */
    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
    }
}