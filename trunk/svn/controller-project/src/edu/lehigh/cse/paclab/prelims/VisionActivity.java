package edu.lehigh.cse.paclab.prelims;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class VisionActivity extends Activity
{
    /** Called when the activity is first created. */
    public RelativeLayout relativeLayout;
    private VisionShowView mShowView;
    private VisionSelectView sView;
    public static VisionActivity _self;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Hide the window title.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create our Preview view and set it as the content of our activity.
        // Create our DrawOnTop view.
        mShowView = new VisionShowView(this);
        sView = new VisionSelectView(this, mShowView);
        _self = this;

        relativeLayout = new RelativeLayout(this);
        relativeLayout.addView(sView);

        setContentView(relativeLayout);
        addContentView(mShowView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }
}
