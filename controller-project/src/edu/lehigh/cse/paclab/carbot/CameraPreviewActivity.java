package edu.lehigh.cse.paclab.carbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Example for taking a picture. Unlike previous example, this actually works.
 * 
 * TODO: crop the image, resize it, and then send it over bluetooth. Then on to
 * video streaming, which may benefit from
 * http://stackoverflow.com/questions/3376672
 * /how-to-capture-preview-image-frames
 * -from-camera-application-in-android-programmi
 */
public class CameraPreviewActivity extends Activity
{

    private final String TAG = "CameraPreviewActivity";
    private Camera mCamera;
    private CameraPreviewView mPreview;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapreviewlayout);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreviewView(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // now drop a square onto it
        ImageView sq = new ImageView(this);
        Drawable d = getResources().getDrawable(R.drawable.square);
        sq.setImageDrawable(d);
        preview.addView(sq);

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // get an image from the camera
                mCamera.takePicture(null, null, mPicture);
            }
        });
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance()
    {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private PictureCallback mPicture = new PictureCallback()
    {

        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            Toast.makeText(CameraPreviewActivity.this, pictureFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            }
            catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            }
            catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    protected void onPause()
    {
        super.onPause();
        releaseCamera(); // release the camera immediately on pause event
    }

    private void releaseCamera()
    {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    public static final int MEDIA_TYPE_IMAGE = 1;

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(int type)
    {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type)
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
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        else {
            return null;
        }

        return mediaFile;
    }
}