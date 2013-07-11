package edu.lehigh.cse.paclab.carbot;

import android.os.Bundle;
import android.view.View;

/**
 * This is the interface to TetheredBot.  There are on-screen buttons for controlling the robot
 */
public class TetheredBotBeta extends BasicBotActivityBeta
{
    /**
     * On activity creation, we just inflate a menu
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // TODO: manage screen size better
        
        // Use this code for managing screens of different size...this uses a more recent API than the previous
        // TetheredBot
        //
        // Display display = getWindowManager().getDefaultDisplay(); Point size = new Point(); display.getSize(size);
        // int width = size.x; int height = size.y;
        setContentView(R.layout.tetheredbot_beta);
    }

    /**
     * Whenever one of the buttons is pressed, we issue the appropriate command to move the robot
     * 
     * @param v
     *            A reference to the button that was pressed
     */
    public void onClickImage(View v)
    {
        if (v == findViewById(R.id.ivTetherForward))
            robotForward();
        if (v == findViewById(R.id.ivTetherReverse))
            robotReverse();
        if (v == findViewById(R.id.ivTetherLeft))
            robotPointTurnLeft();
        if (v == findViewById(R.id.ivTetherRight))
            robotPointTurnRight();
        if (v == findViewById(R.id.ivTetherRotPos))
            robotClockwise();
        if (v == findViewById(R.id.ivTetherRotNeg))
            robotCounterClockwise();
        if (v == findViewById(R.id.ivTetherStop))
            robotStop();
    }

    /**
     * Provide an empty callback method, so that we are compatible with AlarmCallbackReceiver
     */
    public void callback()
    {
    }
}