package edu.lehigh.cse.paclab.carbot;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class ColorDetectionActivity extends BasicBotActivityBeta
{
    private static final String   TAG             = "ColorDetectionActivity";

    private CameraView            mView;

    static ColorDetectionActivity self;

    /**
     * Took this object declaration block from OpenCv. Basically, this application is developed using async
     * initialization. That means that it uses the OpenCV manager (as separate app) to access OpenCV libraries
     * externally installed in the target system. I am not sure if we want to do static initialization or not, but
     * OpenCV recommended this route so I just went with it.
     */
    private BaseLoaderCallback    mOpenCVCallBack = new BaseLoaderCallback(this)
                                                  {
                                                      @Override
                                                      public void onManagerConnected(int status)
                                                      {
                                                          switch (status) {
                                                              case LoaderCallbackInterface.SUCCESS:
                                                                  Log.i(TAG, "OpenCV loaded successfully");

                                                                  // Load native library after(!) OpenCV initialization
                                                                  System.loadLibrary("mixed_sample");

                                                                  // Create and set View
                                                                  mView = new CameraView(mAppContext);
                                                                  setContentView(mView);

                                                                  // Check native OpenCV camera
                                                                  if (!mView.openCamera()) {
                                                                      AlertDialog ad = new AlertDialog.Builder(
                                                                              mAppContext).create();
                                                                      ad.setCancelable(false); // This blocks the 'BACK'
                                                                                               // button
                                                                      ad.setMessage("Fatal error: can't open camera!");
                                                                      // [mfs] Should this be setPositiveButton? See
                                                                      // RCSender...
                                                                      ad.setButton("OK",
                                                                              new DialogInterface.OnClickListener()
                                                                              {
                                                                                  public void onClick(
                                                                                          DialogInterface dialog,
                                                                                          int which)
                                                                                  {
                                                                                      dialog.dismiss();
                                                                                      finish();
                                                                                  }
                                                                              });
                                                                      ad.show();
                                                                  }
                                                                  break;
                                                              default:
                                                                  super.onManagerConnected(status);
                                                                  break;
                                                          }
                                                      }
                                                  };

    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause");
        super.onPause();
        // [mfs] in this conditional, should we set mView to null?
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
            // [mfs] Should this be setPositiveButton? See RCSender...
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

        self = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // RelativeLayout layout = new RelativeLayout(this);
        // CameraView camera = new CameraView(this);

        // layout.addView(camera);

        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
    }

    @Override
    public void callback()
    {
    }

    /**
     * Wrapper for calling point turn left only if we haven't issued a DTMF command in 200 ms
     * 
     * @param y
     *            the y coordinate that triggered this call
     */
    void PTL(int y)
    {
        if (lastEventTime > (System.currentTimeMillis() - 200))
            return;
        robotPointTurnLeft();
        lastEventTime = System.currentTimeMillis();
    }

    /**
     * Wrapper for calling forward only if we haven't issued a DTMF command in 200 ms
     * 
     * @param y
     *            the y coordinate that triggered this call
     */
    void FWD(int y)
    {
        if (lastEventTime > (System.currentTimeMillis() - 200))
            return;
        robotForward();
        lastEventTime = System.currentTimeMillis();
    }

    /**
     * Wrapper for calling point turn right only if we haven't issued a DTMF command in 200 ms
     * 
     * @param y
     *            the y coordinate that triggered this call
     */
    void PTR(int y)
    {
        if (lastEventTime > (System.currentTimeMillis() - 200))
            return;
        robotPointTurnRight();
        lastEventTime = System.currentTimeMillis();
    }

    /**
     * Wrapper for calling rotate only if we haven't issued a DTMF command in 200 ms
     * 
     * @param y
     *            the y coordinate that triggered this call
     */
    void CW()
    {
        if (lastEventTime > (System.currentTimeMillis() - 200))
            return;
        robotClockwise();
        lastEventTime = System.currentTimeMillis();
    }

    /**
     * Track time of last event, so that we can tell if it's OK to issue a new DTMF command yet.
     */
    public static long lastEventTime = 0;
}
