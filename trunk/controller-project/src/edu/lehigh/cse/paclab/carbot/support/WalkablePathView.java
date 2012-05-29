package edu.lehigh.cse.paclab.carbot.support;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class WalkablePathView extends View
{
    private Paint paint = new Paint();
    private ArrayList<Point> pathPoints;
    public float currentX;
    public float currentY;
    public boolean changed = false;

    public WalkablePathView(Context context, AttributeSet as)
    {
        super(context, as);
        init();
    }

    public WalkablePathView(Context context)
    {
        super(context);
        init();
    }
    
    private void init()
    {
        pathPoints = new ArrayList<Point>();

        this.setBackgroundColor(Color.GRAY);

        this.setOnTouchListener(new OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent m)
            {
                switch (m.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (pathPoints.isEmpty()) {
                            pathPoints.add(new Point(m.getX(), m.getY()));
                            currentX = m.getX();
                            currentY = m.getY();
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        pathPoints.add(new Point(m.getX(), m.getY()));
                        break;
                }

                v.invalidate();

                return true;
            }
        });
    }

    public void onDraw(Canvas canvas)
    {
        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(3);

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

        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(6);
        canvas.drawPoint(currentX, currentY, paint);
        //Log.v("X", new Float(currentX).toString());
    }

    public float getPointX(int index)
    {
        return pathPoints.get(index).x;
    }

    public float getPointY(int index)
    {
        return pathPoints.get(index).y;
    }

    public void clearPoints()
    {
        pathPoints.clear();
        this.invalidate();
    }

    public int getSize()
    {
        return pathPoints.size();
    }

    public synchronized void inv()
    {
        if (changed)
            this.invalidate();
        changed = false;
    }

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