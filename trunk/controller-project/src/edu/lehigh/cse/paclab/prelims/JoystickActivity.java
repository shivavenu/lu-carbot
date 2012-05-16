package edu.lehigh.cse.paclab.prelims;

import java.util.Timer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import edu.lehigh.cse.paclab.carbot.R;

public class JoystickActivity extends Activity
{
    public static int x, y;
    static JoystickSurfaceView v;
    Timer timer = new Timer();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joysticklayout);

        // wire the joystick so that it sends messages
        v = (JoystickSurfaceView) findViewById(R.id.jssv1);
        v.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent m)
            {
                switch (m.getAction()) {
                    case MotionEvent.ACTION_UP:
                        sendPosition(0, 0);
                        return false;
                }
                sendMovement();
                return false;
            }
        });
    }

    /**
     * Dummy function... need to send something to the robot...
     */
    private void sendPosition(float x, float y)
    {
        Log.i("Joystick", "Set this position as 0,0");
    }

    /**
     * Dummy function... need to send something to the Robot...
     */
    private void sendMovement()
    {
        Log.v("Joystick", "X: " + v.move.x + " Y: " + v.move.y);
    }
}