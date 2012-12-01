package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class CarbotBeta extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // draw the screen
        setContentView(R.layout.carbot_beta);
    }
	
	public void launchActivity(View v){
		if(v == findViewById(R.id.btnCarbotLegacy))
			startActivity(new Intent(this, Carbot.class));
		if(v == findViewById(R.id.btnTetheredBot))
			startActivity(new Intent(this, TetheredBotBeta.class));
	}
	
}