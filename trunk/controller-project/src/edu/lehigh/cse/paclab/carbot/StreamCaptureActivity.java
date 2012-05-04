package edu.lehigh.cse.paclab.carbot;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;

/**
 * Demo how to use preview callbacks to snap a bunch of pictures
 */
public class StreamCaptureActivity extends Activity implements OnClickListener, SurfaceHolder.Callback
{
    /**
     * For disambiguation of intent replies. When we snap a picture and then use
     * a callback to save it, this is the UID the callback uses to notify us of
     * completion
     */
    static final int INTENT_PHOTO_DONE = 66711324;

    /**
     * Standard Android practice is to give a TAG that can be used in logcat
     * messages
     */
    private static final String TAG = "CameraTest";

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

    /**
     * Called when the activity is first created. This draws the screen and
     * configures any important variables
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // start by calling parent method
        super.onCreate(savedInstanceState);

        // log where we are
        Log.e(TAG, "onCreate");

        // turn off window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // NB: we aren't handling lifecycle right now... we will need to,
        // eventually...
        // 
        // we could also use this to specify the prefix for images being captured...
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            Log.e(TAG, extras.toString());

        // draw the screen
        setContentView(R.layout.streamcapturelayout);

        // wire up the surfaceview to the camera, connect the surfaceholder to
        // the surfaceview
        mSurfaceView = (SurfaceView) findViewById(R.id.cameraDisplay);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // clicking the screen triggers a picture, then calls our callback
        mSurfaceView.setOnClickListener(this);
        mSurfaceHolder.addCallback(this);
    }

    /**
     * Callback to run when the picture is taken
     */
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] imageData, Camera c)
        {
            if (imageData != null) {
                StoreByteImage(StreamCaptureActivity.this, imageData, 50, "ImageName");
                mCamera.startPreview();

                // If we wanted to return the picture immediately, this is how
                // we would do it:
                // Intent mIntent = new Intent();
                // setResult(INTENT_PHOTO_DONE, mIntent);
                // finish();
            }
        }
    };

    int imgcounter = 0;

    /**
     * When the SurfaceView is created, we open up the camera
     */
    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.e(TAG, "surfaceCreated");
        mCamera = getBestCamera();

        // set a callback
        Camera.PreviewCallback previewCallback = new Camera.PreviewCallback()
        {
            public void onPreviewFrame(byte[] data, Camera camera)
            {
                // only save 10 previews, to prevent us from filling the
                // sdcard...
                if (imgcounter > 10)
                    return;
                imgcounter++;

                Camera.Parameters parameters = camera.getParameters();
                int format = parameters.getPreviewFormat();

                // YUV formats require more conversion, but since our image is YUV, we have no choice...
                assert (format == ImageFormat.NV21);

                int w = parameters.getPreviewSize().width;
                int h = parameters.getPreviewSize().height;
                // Get the YuV image
                YuvImage yuv_image = new YuvImage(data, format, w, h, null);
                // Convert YuV to Jpeg... note that we can auto-crop it right
                // here ;)
                Rect rect = new Rect(10, 10, w - 10, h - 10);
                ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
                yuv_image.compressToJpeg(rect, 100, output_stream);

                byte[] byt = output_stream.toByteArray();
                File sdImageMainDirectory = new File("/sdcard");
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(sdImageMainDirectory.toString() + "/image" + imgcounter
                            + ".jpg");
                    BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
                    bos.write(byt);
                    bos.flush();
                    bos.close();
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };

        mCamera.setPreviewCallback(previewCallback);

    }

    /**
     * Open a camera... favor the front-facing one...
     */
    private Camera getBestCamera()
    {
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
        Log.e(TAG, "surfaceChanged");

        // stop showing what's on the screen
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }

        // jpeg previewing... not a good idea for performance...
        // Camera.Parameters p = mCamera.getParameters();
        // p.setPreviewFormat(ImageFormat.JPEG);
        // mCamera.setParameters(p);

        // default FMT is 256... that's JPEG
        // Camera.Parameters p = mCamera.getParameters();
        // Log.i("FMT", "" + p.getPictureFormat());

        // configure the camera for a height and width. Note that we're
        // currently dumping the camera info to logcat so we can find a good
        // size

        // Camera.Parameters p = mCamera.getParameters();
        // List<Camera.Size> sizes = p.getSupportedPreviewSizes();
        // for (Camera.Size s : sizes) {
        // Log.d(TAG, s.width + " : " + s.height);
        // }

        // for now, we set the camera to 480x320 (landscape)
        // then we connect the camera to the surface via the holder
        // p.setPreviewSize(480, 320);

        // mCamera.setParameters(p);
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
        Log.e(TAG, "surfaceDestroyed");
        mCamera.stopPreview();
        mPreviewRunning = false;
        mCamera.setPreviewCallback(null);
        mCamera.release();
    }

    /**
     * Handler to run when the surface is touched... we just take a picture
     */
    public void onClick(View arg0)
    {
        mCamera.takePicture(null, mPictureCallback, mPictureCallback);
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
        File sdImageMainDirectory = new File("/sdcard");
        FileOutputStream fileOutputStream = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            // note: we're downsampling here... and note that quality is low, so
            // we're not a very good looking jpeg
            options.inSampleSize = 5;
            Bitmap myImage_uncrop = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
            // NB: here's a nice trick for cropping: we can build a Bitmap from
            // a Bitmap to get the crop. This works for pure Bitmap, which is
            // nice since the byte[] data we have here is a jpeg...
            Bitmap myImage = Bitmap.createBitmap(myImage_uncrop, 10, 10, myImage_uncrop.getWidth() / 2,
                    myImage_uncrop.getHeight() - 10);
            fileOutputStream = new FileOutputStream(sdImageMainDirectory.toString() + "/image.jpg");
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
}