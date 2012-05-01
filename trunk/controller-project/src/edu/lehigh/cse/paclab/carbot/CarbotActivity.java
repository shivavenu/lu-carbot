package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

public class CarbotActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ImageView iv = (ImageView)findViewById(R.id.imageView2);
		LayoutParams params = (LayoutParams)iv.getLayoutParams();
		params.leftMargin = 100;
		iv.setLayoutParams(params);
	}
}