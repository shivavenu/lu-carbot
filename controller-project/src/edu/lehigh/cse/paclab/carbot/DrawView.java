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
    private Paint            paint = new Paint();

    /**
     * The current path of points that we'll traverse
     */
    private ArrayList<Point> pathPoints;

    /**
     * The starting point
     */
    Point                    startPoint = new Point(0, 0);

    /**
     * This helps us track the first screen press
     */
    public boolean           down  = false;

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
        // create an array for the points
        pathPoints = new ArrayList<Point>();

        // make the view gray
        setBackgroundColor(Color.GRAY);

        // any touch will add a point to the array
        setOnTouchListener(new OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent m)
            {
                // this is a slightly odd interface... the first down press sets our initial point, after that, we
                // register points on release, not on down press
                switch (m.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // if we pressed down, and there are no points, we add this point and set the starting positions
                        // to this touch
                        if (pathPoints.isEmpty()) {
                            pathPoints.add(new Point(m.getX(), m.getY()));
                            startPoint.x = m.getX();
                            startPoint.y = m.getY();
                            down = true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        // swallow the first 'up', because we logged the corresponding 'down'
                        if (down) {
                            down = false;
                            break;
                        }
                        // add this point
                        pathPoints.add(new Point(m.getX(), m.getY()));
                        break;
                }

                // force a redraw
                v.invalidate();
                return true;
            }
        });
    }

    /**
     * This is what runs when we need to draw the image
     */
    public void onDraw(Canvas canvas)
    {
        // pick a color and line width
        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(3);

        // handle the first line
        if (pathPoints.size() >= 2) {
            Point start = pathPoints.get(0);
            Point end = pathPoints.get(1);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawPoint(start.x, start.y, paint);
            canvas.drawPoint(end.x, end.y, paint);
            paint.setColor(Color.CYAN);
            paint.setStrokeWidth(3);
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }

        // handle subsequent lines
        for (int i = 2; i < pathPoints.size(); i++) {
            Point start = pathPoints.get(i - 1);
            Point end = pathPoints.get(i);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawPoint(start.x, start.y, paint);
            canvas.drawPoint(end.x, end.y, paint);
            paint.setColor(Color.CYAN);
            paint.setStrokeWidth(3);
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }

        // draw the start point
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(6);
        canvas.drawPoint(startPoint.x, startPoint.y, paint);
        // Log.v("X", new Float(currentX).toString());
    }

    /**
     * getter for a point's x coordinate
     * 
     * @param index
     * @return
     */
    public float getPointX(int index)
    {
        return pathPoints.get(index).x;
    }

    /**
     * getter for a point's y coordinate
     * 
     * @param index
     * @return
     */
    public float getPointY(int index)
    {
        return pathPoints.get(index).y;
    }

    /**
     * Clear the array of points, then redraw
     */
    public void clearPoints()
    {
        pathPoints.clear();
        invalidate();
    }

    /**
     * Get the number of points
     * 
     * @return
     */
    public int getSize()
    {
        return pathPoints.size();
    }

    /**
     * Helper class to store x,y coordinates
     */
    public class Point
    {
        public float x;
        public float y;

        public Point(float x, float y)
        {
            this.x = x;
            this.y = y;
        }
    }
}