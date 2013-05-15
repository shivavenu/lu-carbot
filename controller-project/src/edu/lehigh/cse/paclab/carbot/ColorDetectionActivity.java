package edu.lehigh.cse.paclab.carbot;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/**
 * This code suffers from a few bugs right now.
 * 
 * TODO: We observed some ForceClose issues earlier, related to a null pointer exception. It is not clear that this is
 * resolved... I think we were just getting lucky
 * 
 * TODO: The logic for steering the robot is not quite right. The robot never turns to the left, and it favors moving
 * forward over keeping its target centered on the screen, which leads to lots of drift.
 * 
 * TODO: The USBManager interface created some hassle, due to bad interactions with the NativeCamera. Often, one would
 * (1) start the activity, (2) connect usbmanager, (3) see that the camera was frozen, (4) hit back, (5) restart the
 * activity, and then it would magically work. We can surely do better.
 */
public class ColorDetectionActivity extends BasicBotActivityBeta
{
    /**
     * TODO: standardize debug tags... not sure we even need this one
     */
    private static final String   TAG             = "ColorDetectionActivity";

    private CameraView            mView;

    static ColorDetectionActivity self;

    /**
     * Took this object declaration block from OpenCv. Basically, this application is developed using async
     * initialization. That means that it uses the OpenCV manager (as separate app) to access OpenCV libraries
     * externally installed in the target system. I am not sure if we want to do static initialization or not, but
     * OpenCV recommended this route so I just went with it.
     * 
     * TODO: if we could change how we start the native camera relative to how we start USBManager, we might be able to
     * call a method to configure this, in which case the code wouldn't have such horrible indentation
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
                                                                      // TODO: Should this be setPositiveButton? See
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
            // TODO: Should this be setPositiveButton? See RCSender...
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
        self = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
     * [TODO] This and subsequent wrappers can be removed now that we aren't doing DTMF anymore...
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
     * Wrapper for calling stop only if we haven't issued a DTMF command in 200 ms
     * 
     * @param y
     *            the y coordinate that triggered this call
     */
    void HLT(int y)
    {
        if (lastEventTime > (System.currentTimeMillis() - 200))
            return;
        robotStop();
        lastEventTime = System.currentTimeMillis();
    }

    /**
     * Track time of last event, so that we can tell if it's OK to issue a new DTMF command yet.
     */
    public static long lastEventTime = 0;
}
