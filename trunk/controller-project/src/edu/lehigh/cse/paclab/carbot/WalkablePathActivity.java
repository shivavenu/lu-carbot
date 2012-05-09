package edu.lehigh.cse.paclab.carbot;

import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class WalkablePathActivity extends Activity
{
    private WalkablePathView view;
    private int index = 1;
    private float current_x;
    private float current_y;
    private double current_orientation = 0;
    private double current_mag = 1;
    public boolean moving = false;
    public CountDownLatch c;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);

        view = new WalkablePathView(this);

        Button b = new Button(this);
        b.setText("Go!");
        b.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if (index < view.getSize()) {
                    current_x = view.getPointX(index - 1);
                    current_y = view.getPointY(index - 1);
                    moveToPoint(index);
                }
            }
        });

        Button clear = new Button(this);
        clear.setText("Cl");
        clear.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                view.clearPoints();
                index = 1;
                current_orientation = 0;
            }
        });

        layout.addView(b);
        layout.addView(clear);
        layout.addView(view);
        this.setContentView(layout);
    }

    public void moveToPoint(int i)
    {
        // if(moving == false)
        {
            float x = view.getPointX(i);
            float y = view.getPointY(i);

            float deltax = x - current_x;
            float deltay = y - current_y;
            double ang = (Math.atan2(deltay, deltax) * (180 / Math.PI));

            Log.v("CURRENT ORIENTATION BEFORE", new Double(current_orientation).toString());
            Log.v("ANGLE", new Double(ang).toString());
            angle(current_orientation - ang);
            Log.v("CURRENT ORIENTATION AFTER", new Double(current_orientation).toString());

            double distance = Math.sqrt(((x - current_x) * (x - current_x)) + ((y - current_y) * (y - current_y)));
            long delay = move(distance, current_x, current_y, x, y);
            final long t_delay = delay - System.currentTimeMillis();

            index++;
            if (index < view.getSize()) {
                current_x = x;
                current_y = y;

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
                Log.i("PathActivity", "send command .1, 0");
                while (System.currentTimeMillis() < stop) {
                    if (System.currentTimeMillis() % 100 == 0) {
                        float x = old_x;
                        float y = old_y;

                        float x_dis = new_x - old_x;
                        float y_dis = new_y - old_y;

                        double percentTraveled = (double) ((System.currentTimeMillis() - start))
                                / ((double) (stop - start));
                        view.currentX = (float) (x + (x_dis * percentTraveled));
                        Log.v("SHOULD Be", new Float((float) (x + (x_dis * percentTraveled))).toString());
                        view.currentY = (float) (y + (y_dis * percentTraveled));
                        view.postInvalidate();
                    }
                }
                Log.i("WalkablePath", "0,0");
                moving = false;
            }
        });
        updateThread.start();
        return stop;
    }

    public void angle(double ang)
    {
        double full_circle = 3800;

        long start = System.currentTimeMillis();
        long stop = start + (long) (full_circle * (Math.abs(ang) / 360));

        if (ang != 0) {
            if (ang < 0)
                Log.i("WalkablePath", "0, -Math.PI / 2");
            else
                Log.i("WalkablePath", "0, Math.PI / 2");
        }

        while (System.currentTimeMillis() < stop) {
        }
        current_orientation -= ang;
        Log.i("WalkablePath", "0, 0");
    }
}