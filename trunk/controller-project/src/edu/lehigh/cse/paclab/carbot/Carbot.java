package edu.lehigh.cse.paclab.carbot;

/**
 * The main TODO we have to cover before we can build out the bot controllers regards the service.  In particular, we need to:
 * 
 * 1 - make sure we can access it from multiple activities without recreating it
 * 
 * 2 - get the configuration correct... right now we need to configure twice, for some reason
 */
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.support.LearnColor;

/**
 * This is the main activity for now. Its only job is to let us launch the other
 * activities that we have made, so that we can test the various components
 */
public class Carbot extends Activity
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // draw the screen
        setContentView(R.layout.carbot);
    }

    /**
     * This is the handler for clicking the various help buttons
     * 
     * @param v
     *            The button that was pressed
     */
    public void onClickHelp(View v)
    {
        if (v == findViewById(R.id.btnCarbotHelpBuildStation))
            showHelpDialog("Bot Builder Help", R.string.BotBuilderDescription);
        if (v == findViewById(R.id.btnCarbotHelpRemoteControlStation))
            showHelpDialog("Remote Control Help", R.string.RemoteControlDescription);
        if (v == findViewById(R.id.btnCarbotHelpBalloonChaseStation))
            showHelpDialog("Balloon Chase Help", R.string.BalloonChaseDescription);
        if (v == findViewById(R.id.btnCarbotHelpDrawToControlStation))
            showHelpDialog("Draw-To-Control Help", R.string.DrawToControlDescription);
        if (v == findViewById(R.id.btnCarbotHelpChatterbox))
            showHelpDialog("Chatterbox Help", R.string.ChatterboxDescription);
    }

    /**
     * Show a dialog and fill its text field
     * 
     * @param title
     *            The title to display on the dialog box
     * @param resId
     *            The id of the string to show (from strings.xml)
     */
    private void showHelpDialog(String title, int resId)
    {
        // set up dialog
        final Dialog dialog = new Dialog(Carbot.this);
        dialog.setContentView(R.layout.helpdialog);
        dialog.setTitle(title);
        dialog.setCancelable(true);

        // set up text
        TextView text = (TextView) dialog.findViewById(R.id.tvDialogHelp);
        text.setText(resId);

        // set up button
        Button button = (Button) dialog.findViewById(R.id.btnDialogHelp);
        button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
            }
        });

        // now that the dialog is set up, it's time to show it
        dialog.show();
    }

    /**
     * Handle clicks of buttons that launch activities
     * 
     * @param v
     *            The button that was clicked
     */
    public void launchActivity(View v)
    {
        // TetheredBot support
        if (v == findViewById(R.id.btnCarbotDirectControl))
            startActivity(new Intent(this, TetheredBot.class));

        // Remote Control Station
        if (v == findViewById(R.id.btnCarbotRemoteControlBot))
            startActivity(new Intent(this, RemoteControlBot.class));
        if (v == findViewById(R.id.btnCarbotRemoteControlPhone))
            startActivity(new Intent(this, RemoteControlPhone.class));

        // Draw To Control Station
        if (v == findViewById(R.id.btnCarbotDrawToControlBot))
            startActivity(new Intent(this, DrawToControlBot.class));

        // Chase Balloon Station
        if (v == findViewById(R.id.btnCarbotFindBalloonBot))
            startActivity(new Intent(this, FindBalloonBot.class));
        if (v == findViewById(R.id.btnCarbotFindBalloonPhone))
            // startActivity(new Intent(this, FindBalloonPhone.class));
            Toast.makeText(this, "This activity has been disabled", Toast.LENGTH_LONG).show();
        if (v == findViewById(R.id.btnCarbotLearnColor))
            startActivity(new Intent(this, LearnColor.class));

        // Final station
        if (v == findViewById(R.id.btnCarbotChatterbox))
            startActivity(new Intent(this, Chatterbox.class));

        // Configuration activity
        if (v == findViewById(R.id.btnCarbotConfigure))
            startActivity(new Intent(this, Configure.class));
    }
}