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
    public class Vector2
    {
        float x;
        float y;

        Vector2(float _x, float _y)
        {
            x = _x;
            y = _y;
        }
    }

    private TutorialThread _thread;
    private Vector2 v = new Vector2(this.getWidth() / 2, this.getHeight() / 2);
    private float linear = 0, omega = 0;
    public Vector2 move = new Vector2(linear, omega);
    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.joystick);
    // b=Bitmap.createScaledBitmap(b, 126, 126, true);
    public boolean touched = false;

    public JoystickSurfaceView(Context context)
    {
        super(context);
        init();

    }

    public JoystickSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        init();
    }

    public JoystickSurfaceView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
        init();
    }

    private void init()
    {
        b = Bitmap.createScaledBitmap(b, 100, 100, true);
        getHolder().addCallback(this);
        // _thread = new TutorialThread(getHolder(), this);
        setFocusable(true);

    }

    @Override
    public void onDraw(Canvas canvas)
    {

        canvas.drawColor(Color.RED);
        canvas.drawBitmap(b, v.x - (b.getWidth() / 2), v.y - (b.getHeight() / 2), null);
        // /canvas.

    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v = circleMove(e.getX(), e.getY());
                // x=(int) e.getX();
                // y=(int) e.getY();
                touched = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                v = circleMove(e.getX(), e.getY());
                // x=(int) e.getX();
                // y=(int) e.getY();
                touched = true;
                return true;
            case MotionEvent.ACTION_UP:
                v.x = this.getWidth() / 2;
                v.y = this.getHeight() / 2;
                touched = true;
                return true;
        }
        return false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        // if (!_thread.isAlive()) {
        _thread = new TutorialThread(getHolder(), this);
        _thread.setRunning(true);
        _thread.start();
        // }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // simply copied from sample application LunarLander:
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        _thread.setRunning(false);
        // _thread.suspend();
        while (retry) {
            try {
                _thread.join();
                retry = false;
            }
            catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    private Vector2 circleMove(float x, float y)
    {

        float centerX = this.getWidth() / 2;
        float centerY = this.getHeight() / 2;

        float circleRadiusY = this.getWidth() * 0.35f;
        float circleRadiusX = this.getHeight() * 0.35f;
        // System.out.println(this.getHeight());
        float rotate = rotateFromPointToPoint(new Vector2(x, y), new Vector2(centerX, centerY));

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
        // System.out.println(rotate + " " + finalX + " " + finalY);

        omega = -(float) ((finalX - centerX) / circleRadiusX * (Math.PI / 4));
        linear = -(float) (finalY - centerY) / circleRadiusY * .4f;

        move = new Vector2(linear, omega);
        return (new Vector2(finalX, finalY));
    }

    private float rotateFromPointToPoint(final Vector2 pFromPoint, final Vector2 pToPoint)
    {
        // System.out.println("rotateVectors" + " " + pToPoint + " " +
        // pFromPoint);

        float k1 = (float) (pToPoint.y - pFromPoint.y);
        float k2 = (float) (pToPoint.x - pFromPoint.x);

        // float tan = k1 / k2;

        float angle = (float) Math.atan2(k1, k2);
        float rotation = (float) Math.toDegrees(angle);

        // System.out.println("currRot: "+Bullet.bulletVelocityX + " " +
        // Bullet.bulletVelocityY);
        return rotation + 180;
    }

    public Vector2 returnTouch()
    {
        if (touched) {
            return v;
        }
        else {
            return null;
        }

    }
}

class TutorialThread extends Thread
{
    private SurfaceHolder _surfaceHolder;
    private JoystickSurfaceView _MySurfaceView;
    private boolean _run = false;

    public TutorialThread(SurfaceHolder surfaceHolder, JoystickSurfaceView MySurfaceView)
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
