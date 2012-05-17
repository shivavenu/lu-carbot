package edu.lehigh.cse.paclab.carbot;

/**
 * The main TODO we have to cover before we can build out the bot controllers regards the service.  In particular, we need to:
 * 
 * 1 - make sure we can access it from multiple activities without recreating it
 * 
 * 2 - get the configuration correct... right now we need to configure twice, for some reason
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * This is the main activity for now. Its only job is to let us launch the other
 * activities that we have made, so that we can test the various components
 */
public class CarbotActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // draw the screen
        setContentView(R.layout.mainlayout);
    }

    /**
     * For now, all we do is launch the demos, but soon we'll do real things
     * instead...
     * 
     * @param v
     *            The button that was pressed
     */
    public void launchActivity(View v)
    {
        if (v == findViewById(R.id.btnLaunchDemos)) {
            startActivity(new Intent(this, edu.lehigh.cse.paclab.prelims.DemosActivity.class));
        }
    }
}