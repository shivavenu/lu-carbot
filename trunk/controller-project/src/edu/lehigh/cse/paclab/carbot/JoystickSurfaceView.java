package edu.lehigh.cse.paclab.carbot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class JoystickSurfaceView extends SurfaceView implements SurfaceHolder.Callback
{
    /**
     * Simple point type for storing x,y float coordinates
     * 
     * @author spear
     * 
     */
    public class Point
    {
        float x;
        float y;

        Point(float _x, float _y)
        {
            x = _x;
            y = _y;
        }
    }

    /**
     * Probably not optimal... this is a way to keep drawing on the screen. I'm
     * almost certain it can be done more simply...
     */
    class JoystickHandler extends Thread
    {
        private SurfaceHolder _surfaceHolder;
        private JoystickSurfaceView _MySurfaceView;
        private boolean _run = false;

        public JoystickHandler(SurfaceHolder surfaceHolder, JoystickSurfaceView MySurfaceView)
        {
            _surfaceHolder = surfaceHolder;
            _MySurfaceView = MySurfaceView;
        }

        public void setRunning(boolean run)
        {
            _run = run;
        }

        @Override
        public void run()
        {
            Canvas c;
            while (_run) {
                c = null;
                try {
                    c = _surfaceHolder.lockCanvas(null);
                    synchronized (_surfaceHolder) {
                        _MySurfaceView.onDraw(c);
                    }
                }
                finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        _surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
    }

    private JoystickHandler jHandler;
    
    // track the position of the joystick
    private Point joystickPosition;
    
    // velocities
    private float linear = 0, omega = 0;
    
    // the movement that we wish to see in the robot
    public Point move = new Point(linear, omega);
    
    // picture of joystick
    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.joystick);
    
    /**
     * Grumble... extending SurfaceView means we need multiple constructors...
     */
    public JoystickSurfaceView(Context context)
    {
        super(context);
        init();
    }

    /**
     * Grumble... extending SurfaceView means we need multiple constructors...
     */
    public JoystickSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    /**
     * Grumble... extending SurfaceView means we need multiple constructors...
     */
    public JoystickSurfaceView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Set up the picture of the joystick and its position, and attach a callback
     * 
     * NB: if we have a callback, why do we need the thread?
     */
    private void init()
    {
        // gross hard-coding of the x,y position of the center...
        joystickPosition = new Point(210, 210);
        b = Bitmap.createScaledBitmap(b, 100, 100, true);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    /**
     * This actually draws the joystick in the appropriate place on the screen
     */
    @Override
    public void onDraw(Canvas canvas)
    {
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(b, joystickPosition.x - (b.getWidth() / 2), joystickPosition.y - (b.getHeight() / 2), null);
    }

    /**
     * On a touch, this figures out where to put the joystick
     */
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                joystickPosition = circleMove(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                joystickPosition = circleMove(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_UP:
                joystickPosition.x = this.getWidth() / 2;
                joystickPosition.y = this.getHeight() / 2;
                return true;
        }
        return false;
    }

    /**
     * Empty method required by our current extends/imports configuration
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        jHandler = new JoystickHandler(getHolder(), this);
        jHandler.setRunning(true);
        jHandler.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // simply copied from sample application LunarLander:
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        jHandler.setRunning(false);
        // _thread.suspend();
        while (retry) {
            try {
                jHandler.join();
                retry = false;
            }
            catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    /**
     * This is going to create a ton of garbage... we can do this more simply...
     */
    private Point circleMove(float x, float y)
    {
        float centerX = this.getWidth() / 2;
        float centerY = this.getHeight() / 2;

        float circleRadiusY = this.getWidth() * 0.35f;
        float circleRadiusX = this.getHeight() * 0.35f;

        float rotate = rotateFromPointToPoint(new Point(x, y), new Point(centerX, centerY));

        float finalX = (centerX + circleRadiusX * (float) Math.cos(rotate * Math.PI / 180));
        float finalY = (centerY + circleRadiusY * (float) Math.sin(rotate * Math.PI / 180));

        if (finalX >= centerX) { // to right of center
            if (x < finalX) {
                finalX = x;
            } // is touch less than circle radius?
        }
        else {
            if (x > finalX) {
                finalX = x;
            }// is touch greater than circle radius?
        }
        if (finalY >= centerY) {// above center
            if (y < finalY) {
                finalY = y;
            } // is touch lower than circle radious
        }
        else {
            if (y > finalY) {
                finalY = y;
            } // above circle?
        }

        omega = -(float) ((finalX - centerX) / circleRadiusX * (Math.PI / 4));
        linear = -(float) (finalY - centerY) / circleRadiusY * .4f;

        move = new Point(linear, omega);
        return (new Point(finalX, finalY));
    }

    private float rotateFromPointToPoint(final Point pFromPoint, final Point pToPoint)
    {
        float k1 = (float) (pToPoint.y - pFromPoint.y);
        float k2 = (float) (pToPoint.x - pFromPoint.x);

        float angle = (float) Math.atan2(k1, k2);
        float rotation = (float) Math.toDegrees(angle);

        return rotation + 180;
    }
}
