package edu.lehigh.cse.paclab.carbot.support;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import edu.lehigh.cse.paclab.carbot.R;

/**
 * This is for drawing a path, and then the robot connected to the phone will
 * perform that movement
 * 
 * [TODO] this is a bit buggy right now... I think it's partly due to the use of
 * threads, but I'm not sure.
 * 
 * @author spear
 * 
 */
public class DrawToControlBot extends BasicBotActivity
{
    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
        // empty for now, but we need to do this if we are going to draw on a
        // remote phone
    }

    private WalkablePathView wpView;
    private int index = 2;
    private float current_x;
    private float current_y;
    private double current_orientation = 0;

    // [mfs] should try to use magnitude scaling eventually...
    // private double current_mag = 1;

    // track if we are moving
    public boolean moving = false;

    private volatile boolean halt = true;
    
    // time in milliseconds for a 360 degree turn
    int rotatemillis;

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
        
        wpView = (WalkablePathView) findViewById(R.id.wpv1);

        SharedPreferences prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity",
                Activity.MODE_WORLD_READABLE);
        rotatemillis = Integer.parseInt(prefs.getString(PREF_TAG_ROTATE, "5000"));

        Log.e("CARBOT", rotatemillis + " = rotatemillis");
        initBTStatus();
    }

    public void onClickGo(View v)
    {
        if (index < wpView.getSize()) {
            halt = false;
            current_x = wpView.getPointX(index - 1);
            current_y = wpView.getPointY(index - 1);
            moveToPoint(index);
        }
    }

    public void onClickClear(View v)
    {
        halt = true;
        robotStop();
        wpView.clearPoints();
        index = 2;
        current_orientation = 0;
    }

    public void moveToPoint(int i)
    {
        if (halt)
            return;
        float x = wpView.getPointX(i);
        float y = wpView.getPointY(i);

        float deltax = x - current_x;
        float deltay = y - current_y;
        double ang = (Math.atan2(deltay, deltax) * (180 / Math.PI));

        Log.v("FROM", "("+current_x+","+current_y+")");
        Log.v("TO", "("+x+","+y+")");
        Log.v("DELTA", "("+deltax+","+deltay+")");
        Log.v("OLD ORIENTATION", current_orientation+"");
        Log.v("ANGLE", "("+ang+")");
        
        angle(current_orientation - ang);
        Log.v("DONE ROTATING", "new angle = " + current_orientation);

        double distance = Math.sqrt(((x - current_x) * (x - current_x)) + ((y - current_y) * (y - current_y)));
        long delay = move(distance, current_x, current_y, x, y);
        final long t_delay = delay - System.currentTimeMillis();
        Log.v("DELAY TIME FOR MOVE", "" + delay);

        index++;
        if (index < wpView.getSize()) {
            current_x = x;
            current_y = y;

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
                        wpView.currentX = (float) (x + (x_dis * percentTraveled));
                        //Log.v("SHOULD Be", new Float((float) (x + (x_dis * percentTraveled))).toString());
                        wpView.currentY = (float) (y + (y_dis * percentTraveled));
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
        Log.v("ROTATION TIME", ""+time);

        if (ang > 0)
            robotCounterclockwise();
        else
            robotClockwise();
        while (System.currentTimeMillis() < stop) {
        }
        robotStop();
        current_orientation -= ang;
        Log.i("WalkablePath", "0, 0");
    }
}