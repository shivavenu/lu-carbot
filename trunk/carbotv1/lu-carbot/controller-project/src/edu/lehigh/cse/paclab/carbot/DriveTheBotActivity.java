package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import edu.lehigh.cse.paclab.carbot.R;
import edu.lehigh.cse.paclab.carbot.services.ArduinoManager;

public class DriveTheBotActivity extends Activity
{
    int alarmNum = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drivethebotlayout);

        forward = (ToggleButton) findViewById(R.id.button1);
        reverse = (ToggleButton) findViewById(R.id.button2);
        clockwise = (ToggleButton) findViewById(R.id.button3);
        counterClockwise = (ToggleButton) findViewById(R.id.button4);
        pTurnRight = (ToggleButton) findViewById(R.id.button5);
        pTurnLeft = (ToggleButton) findViewById(R.id.button6);
    }

    ToggleButton forward;
    ToggleButton reverse;
    ToggleButton clockwise;
    ToggleButton counterClockwise;
    ToggleButton pTurnLeft;
    ToggleButton pTurnRight;

    public void forward(View V)
    {
        if (!forward.isChecked())
            ArduinoManager.sendCommand("STOP");
        else
            ArduinoManager.sendCommand("FWD");
    }

    public void reverse(View V)
    {
        if (!reverse.isChecked())
            ArduinoManager.sendCommand("STOP");
        else
            ArduinoManager.sendCommand("REV");
    }

    public void clockwise(View V)
    {
        if (!clockwise.isChecked())
            ArduinoManager.sendCommand("STOP");
        else
            ArduinoManager.sendCommand("CW");
    }

    public void cclockwise(View V)
    {
        if (!counterClockwise.isChecked())
            ArduinoManager.sendCommand("STOP");
        else
            ArduinoManager.sendCommand("CCW");
    }

    public void pointTurnR(View V)
    {
        if (!pTurnRight.isChecked())
            ArduinoManager.sendCommand("STOP");
        else
            ArduinoManager.sendCommand("PTR");
    }

    public void pointTurnL(View V)
    {
        if (!pTurnLeft.isChecked())
            ArduinoManager.sendCommand("STOP");
        else
            ArduinoManager.sendCommand("INIT");
    }

}