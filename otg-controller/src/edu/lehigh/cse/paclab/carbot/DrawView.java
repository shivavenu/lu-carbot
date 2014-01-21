package edu.lehigh.cse.paclab.carbot;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * This is a canvas that we can use to allow the user to draw on the screen to create an array of points, which then can
 * be used to drive the robot
 */
public class DrawView extends View
{
    /**
     * Object for drawing lines and points on the screen
     */
    private Paint           paint = new Paint();

    /**
     * The current path of points that we'll traverse
     */
    public ArrayList<Point> points = new ArrayList<Point>();

    /**
     * On construction, call our init() function
     */
    public DrawView(Context context, AttributeSet as)
    {
        super(context, as);
        init();
    }

    /**
     * On construction, call our init() function
     */
    public DrawView(Context context)
    {
        super(context);
        init();
    }

    /**
     * Initialization: create our array of points, and set UI components
     */
    private void init()
    {
        // make the view BLACK
        setBackgroundColor(Color.BLACK);

        // any touch will add a point to the array
        setOnTouchListener(new OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent m)
            {
                // on down press, add the point
                if (m.getAction() == MotionEvent.ACTION_DOWN) 
                    points.add(new Point(m.getX(), m.getY()));
                // force a redraw
                v.invalidate();
                return true;
            }
        });
    }

    /**
     * This is what runs when we need to (re)draw the View
     */
    public void onDraw(Canvas canvas)
    {
        // draw every point we have as a red box
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        for (int i = 0; i < points.size(); i++) {
            Point start = points.get(i);
            canvas.drawPoint(start.x, start.y, paint);   
        }

        // draw cyan lines between points
        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(3);
        for (int i = 1; i < points.size(); i++) {
            Point start = points.get(i - 1);
            Point end = points.get(i);
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }
    }

    /**
     * Clear the array of points, then redraw a blank canvas
     */
    public void clearPoints()
    {
        points.clear();
        invalidate();
    }

    /**
     * Helper class to store x,y coordinates
     */
    public class Point
    {
        public float x;
        public float y;

        public Point(float _x, float _y)
        {
            x = _x;
            y = _y;
        }
    }
}