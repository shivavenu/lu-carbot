package edu.lehigh.cse.paclab.carbot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;

/**
 * This is for drawing a path, and then the robot connected to the phone will perform that movement
 */
public class DrawActivity extends BasicBotActivityBeta
{
    /**
     * A reference to the view we use to get user input... it also stores the array of points, which is bad engineering
     * but will do for now...
     */
    private DrawView dv;

    /**
     * time in milliseconds for a 360 degree turn
     */
    int              rotatemillis;

    /**
     * Current orientation/rotation of the robot
     */
    private double   orientation = 0;

    /**
     * The index of the point, within dv.pathPoints, where we currently are. -1 means that we're not in the midst of
     * moving.
     */
    private int      index       = -1;

    /**
     * Used by the FSM to know what the robot is doing, and what it needs to be doing next
     */
    enum MODE {
        STOPPED, // robot is at point[index], waiting to rotate and move to next point
        ROTATING, // robot is in motion, rotating
        ROTATED, // robot is done rotating, is ready to move
        MOVING; // robot is in motion, moving forward
    }

    /**
     * The current mode of the robot
     */
    private MODE mode = MODE.STOPPED;

    /**
     * On creation, we inflate a layout, register our view, and figure out our motor parameters
     */
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
            setContentView(R.layout.drawtocontrolbot_tablet);
        else
            setContentView(R.layout.drawtocontrolbot);
        Log.v("CARBOT", "width, height = " + width + " " + height);

        // find the drawable part of the screen
        dv = (DrawView) findViewById(R.id.wpv1);

        // figure out our rotation latency
        rotatemillis = Integer.parseInt(prefs.getString(PREFS_ROT, "5000"));

        // TODO: why don't we use the parameter for PREFS_DIST, too?
    }

    /**
     * The heart of this activity is a state machine. We have, through dv.pathPoints, an array of x,y coordinates that
     * we'd like to travel. The robot is in an initial position (0, 0), with an initial orientation. Our index
     * represents the current point that we are at. Lastly, we have a mode, to represent 4 states of rotation and motion
     * that are possible. The processing of the state machine is handled via chained callbacks, according to this
     * mechanism.
     */
    public void callback()
    {
        // Do nothing if index is -1
        if (index == -1)
            return;

        switch (mode) {
            case STOPPED: {
                // this is the base state. We have a point to go to, so we need to rotate and then request motion

                // 1 - make sure we have a point to go to... important if there is only one point
                if ((index + 1) >= dv.points.size())
                    return;

                // 2 - switch the mode to ROTATING
                mode = MODE.ROTATING;

                // 3 - figure out how much we need to rotate, and translate it into a time to rotate

                // get the vector for the change in position we wish to achieve
                float deltaX = dv.points.get(index + 1).x - dv.points.get(index).x;
                float deltaY = dv.points.get(index + 1).y - dv.points.get(index).y;

                // figure out angle of the distance vector in a standard x y plane
                double ang = (Math.atan2(deltaY, deltaX) * (180 / Math.PI));

                Log.v("FROM", "(" + dv.points.get(index).x + "," + dv.points.get(index).y + ")");
                Log.v("TO", "(" + dv.points.get(index + 1).x + "," + dv.points.get(index + 1).y + ")");
                Log.v("DELTA", "(" + deltaX + "," + deltaY + ")");
                Log.v("OLD ORIENTATION", orientation + "");
                Log.v("ANGLE", "(" + ang + ")");

                // The angle is relative to an orientation of straight up... adjust for current robot orientation, then
                // update /orientation/
                ang = orientation - ang;
                orientation = ang;
                // Translate coordinate system... left isn't 0 degrees: up is 0 degrees
                ang -= 90;

                // figure out how long we need to rotate... remember that rotatemillis is the time to do a full circle
                double time_to_rotate = rotatemillis * (Math.abs(ang) / 360);

                // DTMF_DELAY_TIME represents the time a DTMF tone must play for our hardware to detect it. If our
                // rotation is too quick, we'll not
                // have shut off the signal yet. Our solution is to add a full rotation in that situation. It's gross,
                // but it should work.
                if (time_to_rotate < DTMF_DELAY_TIME)
                    time_to_rotate += rotatemillis;
                Log.v("ROTATION TIME", "" + time_to_rotate);

                // 4 - start rotating the robot
                if (ang > 0)
                    robotCounterClockwise();
                else
                    robotClockwise();

                // 5 - request callback to stop rotation and move to next stage of FSM
                requestCallback((int) time_to_rotate);

                break;
            }
            case ROTATING: {
                // rotation is done... stop the robot, move to next state, resume FSM
                robotStop();
                mode = MODE.ROTATED;
                requestCallback(DTMF_DELAY_TIME + 100);
                break;
            }
            case ROTATED: {
                // The robot is ready to start moving forward. The challenge is to figure out when it should stop

                // 1 - switch the mode to MOVING
                mode = MODE.MOVING;

                // 2 - Figure out how long the robot needs to move forward

                // get the vector for the change in position we wish to achieve
                float deltaX = dv.points.get(index + 1).x - dv.points.get(index).x;
                float deltaY = dv.points.get(index + 1).y - dv.points.get(index).y;

                Log.v("FROM", "(" + dv.points.get(index).x + "," + dv.points.get(index).y + ")");
                Log.v("TO", "(" + dv.points.get(index + 1).x + "," + dv.points.get(index + 1).y + ")");
                Log.v("DELTA", "(" + deltaX + "," + deltaY + ")");

                // figure out the actual distance
                double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

                // for now, we just scale the pixel distance by 50 to get the time to move... this is a gross hack,
                // should be done more cleanly
                int time_to_go = (int) (50 * distance);
                Log.v("TRAVEL_TIME", "" + time_to_go);

                // 3 - Start the robot, unless we wouldn't be able to stop in time, in which case we skip this movement
                if (time_to_go > DTMF_DELAY_TIME)
                    robotForward();

                // 4 - request a callback that fires when the robot should be at the destination
                requestCallback(time_to_go);

                break;
            }
            case MOVING: {
                // Motion is done... stop the robot, advance index, resume FSM at new index
                robotStop();
                mode = MODE.STOPPED;
                index++;
                requestCallback(DTMF_DELAY_TIME + 100);
                break;
            }
        }
    }

    /**
     * Helper method for requesting a callback for advancing the state machine
     */
    void requestCallback(int millis)
    {
        Context context = this;
        Intent intent = new Intent(context, AlarmCallbackReceiver.class);
        // NB: using same request ID for all PIs in this whole project is a feature
        PendingIntent pi = PendingIntent.getBroadcast(this, 1, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, pi);
    }

    /**
     * This gets called when the user clicks 'go', and it starts the robot along the path that was drawn
     * 
     * @param v
     *            A reference to the button that was clicked
     */
    public void onClickGo(View v)
    {
        // Stop the robot, then indicate we are at position 0 with 0 rotation and we want to start the FSM
        robotStop();
        index = 0;
        orientation = 0;
        mode = MODE.STOPPED;
        requestCallback(2 * DTMF_DELAY_TIME); // make sure the 'stop' tone has ended before we start the FSM...
    }

    /**
     * This gets called when the user clicks 'clear'. it stops the robot, resets the FSM, and clears the array of points
     * 
     * @param v
     *            A reference to the button that was clicked
     */
    public void onClickClear(View v)
    {
        // stop the robot, invalidate the FSM, and clear the set of points
        //
        // NB: since all callbacks have the same integer ID, requesting a callback cancels all pending callbacks
        robotStop();
        index = -1;
        mode = MODE.STOPPED;
        requestCallback(1);
        dv.clearPoints();
    }
}