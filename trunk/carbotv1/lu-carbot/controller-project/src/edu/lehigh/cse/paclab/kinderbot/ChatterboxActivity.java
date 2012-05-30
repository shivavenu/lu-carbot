package edu.lehigh.cse.paclab.kinderbot;

import android.os.Bundle;
import edu.lehigh.cse.paclab.kinderbot.support.BasicBotActivity;

/**
 * This is going to be a silly speech-to-text game. We're going to let the kids
 * talk to the robot, and see if it can understand their words at all :)
 * 
 * [TODO] It would be nice to use Bluetooth as a remote control, for launching
 * Speech-to-Text intents...
 */
public class ChatterboxActivity extends BasicBotActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // [TODO]
        
    }

    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
        // [TODO] if we have BT, we need to do stuff here...
        
    }

}
