package edu.lehigh.cse.paclab.carbot;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Simple demo to show how to draw with touch. This is from
 * http://www.vogella.com/articles/AndroidTouch/article.html
 */
public class TouchDrawActivity extends Activity
{
    /**
     * Helper class: this is a view that we can paint on
     */
    public class DrawableView extends View
    {
        private Paint paint = new Paint();
        private Path path = new Path();

        public DrawableView(Context context, AttributeSet attrs)
        {
            super(context, attrs);

            paint.setAntiAlias(true);
            paint.setStrokeWidth(6f);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    setOrigin(eventX, eventY);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(eventX, eventY);
                    extendPath(eventX, eventY);
                    break;
                case MotionEvent.ACTION_UP:
                    // nothing to do
                	moveRobot();
                    break;
                default:
                    return false;
            }

            // Schedules a repaint.
            invalidate();
            return true;
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(new DrawableView(this, null));
    }
    
    /**
     * Encapsulate points in a 2-d coordinate system
     */
	private class Point 
	{
		public double x;
		public double y;

		Point(float _x, float _y) 
		{
			x = _x;
			y = _y;
		}
		
		@Override
		public String toString() 
		{
			return "(" + x + ", " + y + ")";
		}
	}

	ArrayList<Point> points;
	
	void setOrigin(float x, float y) 
	{
		points = new ArrayList<Point>();
		points.add(new Point(x,y));
	}

	void extendPath(float x, float y) 
	{
		points.add(new Point(x,y));
	}
	
	void moveRobot() 
	{
		Point origin = new Point(0,0);
		Point last = new Point(0,0);
		Point next = new Point(0,0);
		String msg = "";
		int segment = 0;
		boolean started = false;
		for (Point p : points) {
			if (!started) {
				origin.x = p.x;
				origin.y = p.y;
				msg = "Origin = " + origin;
				Log.i("MoveRobot", msg);
				started = true;
			}
			else {
				++segment;
				msg = "Added segment " + segment;
				Log.i("MoveRobot", msg);
				msg = "New Point = " + p.toString();
				Log.i("MoveRobot", msg);
				next.x = p.x - origin.x;
				next.y = p.y - origin.y;
				msg = "Transposed Point = " + next.toString();
				Log.i("MoveRobot", msg);
				
				msg = "move from " + last.toString() + " to " + next.toString();
				Log.i("MoveRobot", msg);
				
				// do trig here to figure out rotation
				double rotation = 0.0; // obviously missing some past info about the old direction
				msg = "need to rotate " + rotation + "radians (real answer TBD)";
				Log.i("MoveRobot", msg);
				
				// now compute distance to travel
				double distance = Math.sqrt((last.x - next.x)
						* (last.x - next.x) + (last.y - next.y)
						* (last.y - next.y));
				msg = "need to move " + distance + "units";
				Log.i("MoveRobot", msg);
				// set up for next point
				last.x = next.x;
				last.y = next.y;

			}
		}
	}
}
