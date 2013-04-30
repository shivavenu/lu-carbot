package edu.lehigh.cse.paclab.carbot;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import edu.lehigh.cse.paclab.carbot.support.CameraView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.RelativeLayout;

public class ColorDetectionActivity extends Activity
{
    private static final String TAG = "ColorDetectionActivity";

    private CameraView          mView;

    /**
     * Took this object declaration block from OpenCv. Basically, this application is developed using async
     * initialization. That means that it uses the OpenCV manager (as separate app) to access OpenCV libraries
     * externally installed in the target system. I am not sure if we want to do static initialization or not, but
     * OpenCV recommended this route so I just went with it.
     */
    private BaseLoaderCallback  mOpenCVCallBack;

    private void setBaseLoaderCallback()
    {
        mOpenCVCallBack = new BaseLoaderCallback(this)
        {
            @Override
            public void onManagerConnected(int status)
            {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");

                        // Load native library after(!) OpenCV initialization
                        System.loadLibrary("mixed_sample");

                        // Create and set View
                        mView = new CameraView(mAppContext);
                        setContentView(mView);

                        // Check native OpenCV camera
                        if (!mView.openCamera()) {
                            AlertDialog ad = new AlertDialog.Builder(mAppContext).create();
                            ad.setCancelable(false); // This blocks the 'BACK'
                                                     // button
                            ad.setMessage("Fatal error: can't open camera!");
                            ad.setButton("OK", new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                    finish();
                                }
                            });
                            ad.show();
                        }
                    }
                        break;
                    default: {
                        super.onManagerConnected(status);
                    }
                        break;
                }
            }
        };
    }

    public ColorDetectionActivity()
    {
        Log.i(TAG, "Instantiated new ColorDetectionActivity");
    }

    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause");
        super.onPause();
        if (null != mView)
            mView.releaseCamera();
    }

    @Override
    protected void onResume()
    {
        Log.i(TAG, "onResume");
        super.onResume();
        if ((null != mView) && !mView.openCamera()) {
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setMessage("Fatal error: can't open camera!");
            ad.setButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    finish();
                }
            });
            ad.show();
        }
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // RelativeLayout layout = new RelativeLayout(this);
        // CameraView camera = new CameraView(this);

        // layout.addView(camera);

        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }

        setBaseLoaderCallback();
    }

}
