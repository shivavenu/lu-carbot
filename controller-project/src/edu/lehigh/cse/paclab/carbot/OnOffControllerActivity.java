package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Our plan is for the arduino to expose 6 primitives: TurnLeft, TurnRight,
 * Forward, Reverse, Clockwise, and CounterClockwise. This is a rudimentary
 * framework for a controller. In reality, we would want the system to send
 * messages to the arduino when buttons are pressed
 */
public class OnOffControllerActivity extends Activity 
{
	/**
	 * For storing all the buttons on the view, so we can easily un-toggle them...
	 */
	ToggleButton btns[] = new ToggleButton[6];
	String messages[] = {"Stopped", "Forward", "Reverse", "Left", "Right", "Clockwise", "CounterClockwise"};
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		// start by calling parent method
		super.onCreate(savedInstanceState);

		// draw the screen
		setContentView(R.layout.onoffcontrollayout);
		
		// find all the buttons
		btns[0] = (ToggleButton)findViewById(R.id.tbForward);
		btns[1] = (ToggleButton)findViewById(R.id.tbReverse);
		btns[2] = (ToggleButton)findViewById(R.id.tbLeft);
		btns[3] = (ToggleButton)findViewById(R.id.tbRight);
		btns[4] = (ToggleButton)findViewById(R.id.tbCW);
		btns[5] = (ToggleButton)findViewById(R.id.tbCCW);
	}

	// constants... we could use an Enum, but this is pretty straightforward
	static final int CMD_NONE = 0;
	static final int CMD_FWD = 1;
	static final int CMD_REV = 2;
	static final int CMD_LFT = 3;
	static final int CMD_RT = 4;
	static final int CMD_CW = 5;
	static final int CMD_CCW = 6;

	/**
	 * track the current command given to the robot.  Note that lifecycle could be an issue here.
	 */
	int currCmd = CMD_NONE;

	/**
	 * When a button is pressed, we translate its ID to a direction int, and
	 * then pass the event on
	 * 
	 * @param v
	 *            The button that was pressed
	 */
	public void onToggleButtonClick(View v)
	{
		if (v == findViewById(R.id.tbForward)) {
			toggleDirection(CMD_FWD, v);
		}
		else if (v == findViewById(R.id.tbReverse)) {
			toggleDirection(CMD_REV, v);
		}
		else if (v == findViewById(R.id.tbLeft)) {
			toggleDirection(CMD_LFT, v);
		}
		else if (v == findViewById(R.id.tbRight)) {
			toggleDirection(CMD_RT, v);
		}
		else if (v == findViewById(R.id.tbCW)) {
			toggleDirection(CMD_CW, v);
		}
		else if (v == findViewById(R.id.tbCCW)) {
			toggleDirection(CMD_CCW, v);
		}
	}

	/**
	 * Simulate the handling of events. We get the ID of the event that
	 * happened, as well as the actual button that was pressed. This lets us
	 * both turn off the prior action's toggle button, and change the text being
	 * displayed.
	 * 
	 * @param i
	 *            ID of the event corresponding to the button
	 * @param btn
	 *            Reference to the button that was pressed
	 */
	private void toggleDirection(int i, View btn)
	{
		TextView tv = (TextView)findViewById(R.id.tvCurrentState);
		
		// are we stopping motion?
		if (currCmd == i) {
			tv.setText("");
			currCmd = CMD_NONE;
			return;
		}
		
		// un-toggle all buttons except the one we just pressed
		//
		// NB: this is overkill, since we have currCmd... 
		for (ToggleButton tb : btns)
			if ((tb != btn) && (tb.isChecked()))
				tb.setChecked(false);
				
		// update the text
		tv.setText(messages[i]);
		currCmd = i;
	}
}
