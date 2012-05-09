package edu.lehigh.cse.paclab.carbot;

import java.util.Timer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

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
        // lcmMessenger l = new lcmMessenger();
        v = (JoystickSurfaceView) findViewById(R.id.jssv1);


        v.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent m)
            {
                switch (m.getAction()) {
                    case MotionEvent.ACTION_UP:
                        Log.i("Joystick", "Set this position as 0,0");
                        return false;
                }
                sendMovement();

                return false;
            }
        });

    }

    private void sendMovement()
    {
        Log.v("Joystick", "X: " + v.move.x + " Y: " + v.move.y);
    }
}