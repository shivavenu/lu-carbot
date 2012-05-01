package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

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
                    return true;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(eventX, eventY);
                    break;
                case MotionEvent.ACTION_UP:
                    // nothing to do
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
}
