package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;

/**
 * This is for drawing a path, and then the robot connected to the phone will perform that movement
 * 
 * [TODO] this is a bit buggy right now... I think it's partly due to the use of threads, but I'm not sure.
 * 
 * [TODO] Actually, this is incredibly buggy right now because the threads don't know to wait for the DTMF stop pulses
 * that are supposed to happen...
 */
public class DrawActivity extends BasicBotActivityBeta
{
    /**
     * A reference to the view we use to get user input... it also stores the array of points, which is bad engineering
     * but will do for now...
     */
    private DrawView wpView;

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
        wpView = (DrawView) findViewById(R.id.wpv1);

        // figure out our rotation latency
        SharedPreferences prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity",
                Activity.MODE_WORLD_WRITEABLE);
        rotatemillis = Integer.parseInt(prefs.getString(PREFS_ROT, "5000"));
        Log.e("CARBOT", rotatemillis + " = rotatemillis");

        // initialize the point for our last (current) position
        position = wpView.new Point(0, 0);
    }

    /**
     * last position of the robot when it is moving, current position of the robot when it is stopped
     */
    DrawView.Point   position;

    /**
     * Orientation/rotation of the robot
     */
    private double   orientation   = 0;

    /**
     * The index of the point, within wpView.pathPoints, where we currently are. -1 means that we're not in the midst of
     * moving.
     */
    private int      index         = -1;

    /**
     * The current mode of the robot: see documentation on callback(), and the four MODE_X constants below
     */
    private int      mode          = MODE_STOPPED;

    /**
     * enum representing when robot is at point, waiting to rotate and move to next point
     */
    static final int MODE_STOPPED  = 0;

    /**
     * enum representing when robot is rotating
     */
    static final int MODE_ROTATING = 1;

    /**
     * enum representing when robot is done rotating, ready to move
     */
    static final int MODE_ROTATED  = 2;

    /**
     * enum representing when robot is moving forward
     */
    static final int MODE_MOVING   = 3;

    /**
     * The heart of this activity is a state machine. We have, through wpView.pathPoints, an array of x,y coordinates
     * that we'd like to travel. The robot is in an initial position (0, 0), with an initial orientation. Our index
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
            case MODE_STOPPED:
                // if there is another point, then we need to:
                // - figure out how much to rotate
                // - switch mode to ROTATING
                // - start rotating
                // - request a callback at the right time
                break;
            case MODE_ROTATING:
                // from here we need to:
                // - stop the robot
                // - switch the mode to ROTATED
                // - request a callback in a fixed interval after DTMF stops (1 second?)
                break;
            case MODE_ROTATED:
                // from here we need to:
                // - figure out how long the robot needs to move for
                // - switch the mode to MOVING
                // - start moving forward
                // - request a callback at the right time
                break;
            case MODE_MOVING:
                // from here we need to:
                // - stop the robot
                // - switch the mode to STOPPED
                // - request a callback in a fixed interval after DTMF stops (1 second?)
                break;
        }
    }

    /**
     * Helper method for requesting a callback for advancing the state machine
     */
    void requestCallback(int millis)
    {
        Context context = this;
        Intent intent = new Intent(context, AlarmCallbackReceiver.class);

        // remember to delete the older alarm before creating the new one
        PendingIntent pi = PendingIntent.getBroadcast(this, 1, // the request id, used for disambiguating this intent
                intent, 0); // pending intent flags
        // set an alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, pi);
    }

    /**
     * This gets called when the user clicks 'go'. The old behavior is coded, the new behavior appears in a comment.
     * 
     * @param v
     *            A reference to the button that was clicked
     */
    public void onClickGo(View v)
    {
        // new behavior: set mode to stopped, set index to 0?, set rotation to 0, request a callback to kick off the
        // state machine

        // old behavior:
        if (index < wpView.getSize()) {
            halt = false;
            position.x = wpView.getPointX(index - 1);
            position.y = wpView.getPointY(index - 1);
            moveToPoint(index);
        }
    }

    /**
     * This gets called when the user clicks 'clear'. The old behavior is coded, the new behavior appears in a comment.
     * 
     * @param v
     *            A reference to the button that was clicked
     */
    public void onClickClear(View v)
    {
        // new behavior: stop the robot, set index to -1, request a callback
        //
        // NB: requesting a callback is a cute hack... since all callbacks have the same integer ID, this effectively
        // cancels all pending callbacks

        // old behavior:
        halt = true;
        robotStop();
        wpView.clearPoints();
        index = 2;
        orientation = 0;
    }

    // ////////////////
    // [mfs] Everything below this is deprecated and can be deleted once we get the callback stuff working

    // [mfs] should try to use magnitude scaling eventually...
    // private double position.mag = 1;

    // track if we are moving
    public boolean           moving = false;

    private volatile boolean halt   = true;

    // time in milliseconds for a 360 degree turn
    //
    // TODO: why don't we have our meter stuff in here?
    int                      rotatemillis;

    // deprecated... delete once the new stuff works
    public void moveToPoint(int i)
    {
        if (halt)
            return;
        float x = wpView.getPointX(i);
        float y = wpView.getPointY(i);

        float deltax = x - position.x;
        float deltay = y - position.y;
        double ang = (Math.atan2(deltay, deltax) * (180 / Math.PI));

        Log.v("FROM", "(" + position.x + "," + position.y + ")");
        Log.v("TO", "(" + x + "," + y + ")");
        Log.v("DELTA", "(" + deltax + "," + deltay + ")");
        Log.v("OLD ORIENTATION", orientation + "");
        Log.v("ANGLE", "(" + ang + ")");

        angle(orientation - ang);
        Log.v("DONE ROTATING", "new angle = " + orientation);

        double distance = Math.sqrt(((x - position.x) * (x - position.x)) + ((y - position.y) * (y - position.y)));
        long delay = move(distance, position.x, position.y, x, y);
        final long t_delay = delay - System.currentTimeMillis();
        Log.v("DELAY TIME FOR MOVE", "" + delay);

        index++;
        if (index < wpView.getSize()) {
            position.x = x;
            position.y = y;

            // [mfs] using threads like this is going to create a lot of system
            // pressure... we could use an alarm instead...
            Thread delayThread = new Thread(new Thread()
            {
                public void run()
                {
                    try {
                        sleep(t_delay);
                    }
                    catch (Exception e) {
                    }
                    moveToPoint(index);
                }
            });

            delayThread.start();
        }

    }

    // deprecated... delete once the new stuff works
    // [todo] This should use prefs to know distance...
    public long move(double _dis, float _old_x, float _old_y, float _new_x, float _new_y)
    {
        final double dis = _dis;
        final float old_x = _old_x;
        final float old_y = _old_y;
        final float new_x = _new_x;
        final float new_y = _new_y;
        final long start = System.currentTimeMillis();
        final long stop = start + (long) (dis * 50);
        moving = true;

        Thread updateThread = new Thread(new Runnable()
        {
            public void run()
            {
                robotForward();
                Log.i("PathActivity", "send command .1, 0");
                while (System.currentTimeMillis() < stop) {
                    if (System.currentTimeMillis() % 100 == 0) {
                        float x = old_x;
                        float y = old_y;

                        float x_dis = new_x - old_x;
                        float y_dis = new_y - old_y;

                        double percentTraveled = (double) ((System.currentTimeMillis() - start))
                                / ((double) (stop - start));
                        wpView.startPoint.x = (float) (x + (x_dis * percentTraveled));
                        // Log.v("SHOULD Be", new Float((float) (x + (x_dis * percentTraveled))).toString());
                        wpView.startPoint.y = (float) (y + (y_dis * percentTraveled));
                        wpView.postInvalidate();
                    }
                }
                robotStop();
                Log.i("WalkablePath", "0,0");
                moving = false;
            }
        });
        updateThread.start();
        return stop;
    }

    // deprecated... delete once the new stuff works
    public void angle(double ang)
    {
        // compensate for the fact that left is 0 degrees in this code, by making up 0 degrees
        ang -= 90;
        Log.v("Calling ANGLE", "ang = " + ang);

        // I think this is how long we need to wait...
        double full_circle = rotatemillis;

        long start = System.currentTimeMillis();
        long time = (long) (full_circle * (Math.abs(ang) / 360));
        long stop = start + time;
        Log.v("ROTATION TIME", "" + time);

        if (ang > 0)
            robotCounterClockwise();
        else
            robotClockwise();
        while (System.currentTimeMillis() < stop) {
        }
        robotStop();
        orientation -= ang;
        Log.i("WalkablePath", "0, 0");
    }

}