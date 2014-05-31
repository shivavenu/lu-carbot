package edu.lehigh.cse.paclab.carbot;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

/**
 * This is the interface to TetheredBot. There are on-screen buttons for controlling the robot
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
            myRobotForward();
        if (v == findViewById(R.id.ivTetherReverse))
            myRobotReverse();
        if (v == findViewById(R.id.ivTetherLeft))
            myRobotPointTurnLeft();
        if (v == findViewById(R.id.ivTetherRight))
            myRobotPointTurnRight();
        if (v == findViewById(R.id.ivTetherRotPos))
            myRobotClockwise();
        if (v == findViewById(R.id.ivTetherRotNeg))
            myRobotCounterClockwise();
        if (v == findViewById(R.id.ivTetherStop))
            myRobotStop();
    }

    /**
     * Provide an empty callback method, so that we are compatible with AlarmCallbackReceiver
     */
    public void callback()
    {
    }

    /**
     * Starts the activity, using the supplied driver instance.
     * 
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver)
    {
        sDriver = driver;
        final Intent intent = new Intent(context, TetheredBotBeta.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }
}