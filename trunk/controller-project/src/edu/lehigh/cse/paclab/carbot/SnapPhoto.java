package edu.lehigh.cse.paclab.carbot;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.R;

/**
 * A pretty gritty camera. When opened, it looks at the prefs, finds the
 * appropriate times, and sets timers to cause an automatic picture and
 * automatic return
 * 
 * TODO: it's quite ugly to implement OnClickListener and
 * SurfaceHolder.Callback. We should use anonymous classes... oh well
 */
public class SnapPhoto extends Activity implements OnClickListener, SurfaceHolder.Callback
{
    /**
     * A reference to the camera that we are using
     */
    Camera mCamera;

    /**
     * State var to track if the preview is being shown
     */
    boolean mPreviewRunning = false;

    /**
     * Reference to the View that displays the camera contents
     */
    private SurfaceView mSurfaceView;

    /**
     * The SurfaceHolder lets us control the SurfaceView
     */
    private SurfaceHolder mSurfaceHolder;
    SharedPreferences prefs;

    /**
     * Called when the activity is first created. This draws the screen and
     * configures any important variables
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        self = this;

        // turn off window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // draw the screen
        setContentView(R.layout.facecapturelayout);

        // wire up the surfaceview to the camera, connect the surfaceholder to
        // the surfaceview
        mSurfaceView = (SurfaceView) findViewById(R.id.cameraDisplay);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // clicking the screen triggers a picture, then calls our callback
        mSurfaceView.setOnClickListener(this);
        mSurfaceHolder.addCallback(this);

        // default...
        setResult(Activity.RESULT_CANCELED);

        // set an alarm to go off after the camera has started
        prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_READABLE);
        setAlarm(Integer.parseInt(prefs.getString(BasicBotActivity.PREF_TAG_CAMSTART, "5000")));
    }

    int alarmNum = 0;

    public static SnapPhoto self;

    public void onAlarm()
    {
        if (alarmNum == 1) {
            onClick(null);
            setAlarm(Integer.parseInt(prefs.getString(BasicBotActivity.PREF_TAG_CAMLAG, "5000")));
        }
        else {
            onClick(null);
        }
    }

    private void setAlarm(int time)
    {
        // set a timer for when to stop
        Intent intent = new Intent(this, AlarmSnapPhotoReceiver.class);
        intent.putExtra("AlarmID", alarmNum);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (time), pendingIntent);
    }

    /**
     * Callback to run when the picture is taken
     */
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] imageData, Camera c)
        {
            if (imageData != null) {
                Intent mIntent = new Intent();
                StoreByteImage(SnapPhoto.this, imageData, 50, "ImageName");
                mCamera.startPreview();
                setResult(BasicBotActivity.INTENT_PHOTO_DONE, mIntent);
            }
        }
    };

    /**
     * When the SurfaceView is created, we open up the camera
     */
    public void surfaceCreated(SurfaceHolder holder)
    {
        mCamera = getBestCamera();
    }

    /**
     * Open a camera... favor the front-facing one...
     */
    private Camera getBestCamera()
    {
        // this code only works with a target of 2.3 or higher... since I am
        // testing on 2.2. and 2.3 devices simultaneously, I've turned this off
        // for now...

        Camera.CameraInfo info = new Camera.CameraInfo();
        int num = Camera.getNumberOfCameras();
        for (int i = 0; i < num; ++i) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    Camera c = Camera.open(i);
                    return c;
                }
                catch (RuntimeException e) {
                }
            }
        }

        // worst case: use the default
        return Camera.open();
    }

    /**
     * Connect the surface to the camera so we can see what the camera sees
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
        // stop showing what's on the screen
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }

        try {
            mCamera.setPreviewDisplay(holder);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // start previewing data
        mCamera.startPreview();
        mPreviewRunning = true;
    }

    /**
     * When the surface is destroyed, we need to free up the camera
     */
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mCamera.stopPreview();
        mPreviewRunning = false;
        mCamera.release();
    }

    boolean firstclick = false;

    /**
     * Handler to run when the surface is touched... we just take a picture the
     * first time, return it the second time
     */
    public void onClick(View arg0)
    {
        if (!firstclick) {
            mCamera.takePicture(null, mPictureCallback, mPictureCallback);
            firstclick = true;
        }
        else {
            setResult(RESULT_OK);
            finish();
        }
    }

    /**
     * Save the picture that we took. This is copied code, which I don't yet
     * understand. Worse, it doesn't quite work yet. The final picture taken is
     * a mess!
     * 
     * @param mContext
     * @param imageData
     * @param quality
     * @param expName
     * @return
     */
    public boolean StoreByteImage(Context mContext, byte[] imageData, int quality, String expName)
    {
        FileOutputStream fileOutputStream = null;
        try {
            // TODO: need to understand what this is doing wrong...
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 5;
            Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
            fileOutputStream = new FileOutputStream(getOutputMediaFile());
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
            myImage.compress(CompressFormat.JPEG, quality, bos);
            bos.flush();
            bos.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "saved", Toast.LENGTH_SHORT).show();
        return true;
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile()
    {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CARBOT", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "snap.jpg");
        return mediaFile;
    }

}
