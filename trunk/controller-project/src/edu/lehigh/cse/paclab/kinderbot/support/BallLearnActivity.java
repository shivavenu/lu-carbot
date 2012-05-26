package edu.lehigh.cse.paclab.kinderbot.support;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvAvgSdv;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSplit;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGBA2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * Simple activity consisting (for now) of just a CameraDisplay. As far as I can
 * tell, Android requires two objects in the Layout to display a camera preview:
 * a View and a SurfaceView.
 * 
 * Ultimately, this is going to be the way that we teach the robot about balls.
 * We start the activity, put a ball in the viewport, and then click a button
 * and the robot will remember the key image color parameters
 */
public class BallLearnActivity extends Activity
{
    /**
     * The activity consists of a SurfaceView to show the camera preview, a view
     * on top of the SurfaceView for our HUD-style drawing, and a button that
     * remembers the image parameters.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Create a layout, then nest a View inside a SurfaceView inside the
        // layout
        RelativeLayout layout = new RelativeLayout(this);
        BallLearnOverlayView overlay = new BallLearnOverlayView(this);
        CameraPreviewSurfaceView camera = new CameraPreviewSurfaceView(this, overlay);
        layout.addView(camera);
        layout.addView(overlay);

        // add a button that gets the data from the image and finishes the
        // activity
        Button b = new Button(this);
        b.setText("OK");
        b.setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                computeThreshAndSave();
            }
        });

        // NB: need to stick button inside a LinearLayout, then shove that onto
        // the FrameLayout. For now, we just set the button size and wish it was
        // somewhere else...
        b.setWidth(45);
        b.setHeight(45);
        layout.addView(b);

        // reset the flag for whether we want the thresholds saved
        requestThresholdCapture = false;

        // save a reference to the activity
        self = this;

        // draw the Activity
        setContentView(layout);
    }

    /**
     * Asynchronous mechanism for informing the preview that it ought to compute
     * properties of the image
     * 
     * Note that right now, the first click captures data, and the second kills
     * the activity. Eventually, the finish() should go in onDraw where we
     * currently dump, so that we just register the image and return.
     */
    private void computeThreshAndSave()
    {
        if (requestThresholdCapture == false)
            requestThresholdCapture = true;
        else
            finish();
    }

    /**
     * A flag for notifying the overlay that we want it to capture the image
     */
    public static volatile boolean requestThresholdCapture = false;

    /**
     * A reference to the activity, so that we can shut it down programatically
     */
    public static BallLearnActivity self;
}

/**
 * This view is where we draw things that appear 'on top of' the camera preview.
 */
class BallLearnOverlayView extends View implements Camera.PreviewCallback
{
    /**
     * A buffer for storing an RGB representation of the bytes of the preview
     * image
     */
    private int[] rgb;

    /**
     * Keep an image around, so that we don't have a lot of memory churn
     */
    private IplImage scratchImg;

    /**
     * Simple constructor: we don't have to do anything special...
     * 
     * @param context
     *            the activity where this resides
     */
    public BallLearnOverlayView(Activity context)
    {
        super(context);
        hasCaptureData = false;
    }

    /**
     * Whenever we get a preview frame, this is the hook (via
     * Camera.PreviewCallback) for processing the frame
     */
    public void onPreviewFrame(final byte[] data, final Camera camera)
    {
        try {
            Camera.Size size = camera.getParameters().getPreviewSize();
            processImage(data, size.width, size.height);
            camera.addCallbackBuffer(data);
        }
        catch (RuntimeException e) {
            // Ignore errors. There is an async issue when releasing the camera,
            // and we might get an exception due to the code running while the
            // camera is being released
        }
    }

    /**
     * This is where we actually process the image
     * 
     * @param yuvData
     *            The bytes of the image
     * @param width
     *            The width of the camera viewport
     * @param height
     *            The height of the camera viewport
     */
    protected void processImage(byte[] yuvData, int width, int height)
    {
        // make sure our temp fields are configured
        if (scratchImg == null) {
            configFields(width, height);
        }

        // only compute the average and stdev if there is a pending request and
        // we haven't done it yet
        if (BallLearnActivity.requestThresholdCapture && !hasCaptureData) {
            // convert from YUV420S to RGB format...
            ImageUtils.decodeYUV420SP(rgb, yuvData, width, height);

            // it may be good to downsample the image...
            //
            // Create an image for the output
            // IplImage* out = cvCreateImage(cvSize(img->width/2, img->height/2), 
            //                               img->depth, img->nChannels);
            //
            // Reduce the image by 2
            // cvPyrDown(img, out);

            // dump the image data into our scratch space
            scratchImg.getIntBuffer().put(rgb);

            // try doing ROI
            cvSetImageROI(scratchImg, cvRect(vpLeft, vpTop, vpWidth, vpHeight));

            // now dump histograms
            computeAvgStd(scratchImg);

            hasCaptureData = true;
        }

        // force the View to be redrawn
        postInvalidate();
    }

    private boolean hasCaptureData = false;

    private int vpTop;
    private int vpLeft;
    private int vpHeight;
    private int vpWidth;
    
    CvScalar avgHue = new CvScalar(1);
    CvScalar stdHue = new CvScalar(1);
    CvScalar avgSat = new CvScalar(1);
    CvScalar stdSat = new CvScalar(1);
    CvScalar avgVal = new CvScalar(1);
    CvScalar stdVal = new CvScalar(1);

    private void configFields(int width, int height)
    {
        scratchImg = IplImage.create(width, height, IPL_DEPTH_8U, 4);
        rgb = new int[width * height];
        
        // configure the viewport
        vpHeight = height / 3;
        vpWidth = width / 4;
        vpTop = height / 2 - vpHeight / 2;
        vpLeft = width / 2 - vpWidth / 2;
    }

    /**
     * Compute average and standard deviation on each color channel in an image
     */
    void computeAvgStd(IplImage rgbaImage)
    {
        // first convert the image from RGBA to BGR
        IplImage bgrImage = cvCreateImage(cvGetSize(rgbaImage), IPL_DEPTH_8U, 3);
        cvCvtColor(rgbaImage, bgrImage, CV_RGBA2BGR);

        // now convert to HSV
        IplImage hsvImage = cvCreateImage(cvGetSize(rgbaImage), IPL_DEPTH_8U, 3);
        cvCvtColor(bgrImage, hsvImage, CV_BGR2HSV);
        
        // create separate images for the three channels
        IplImage imgHue = cvCreateImage(cvGetSize(bgrImage), 8, 1);
        IplImage imgSat = cvCreateImage(cvGetSize(bgrImage), 8, 1);
        IplImage imgVal = cvCreateImage(cvGetSize(bgrImage), 8, 1);

        // split image
        cvSplit(hsvImage, imgHue, imgSat, imgVal, null);

        
        // do red avg/stdev
        cvAvgSdv(imgHue, avgHue, stdHue, null);

        // do green avg/stdev
        cvAvgSdv(imgSat, avgSat, stdSat, null);

        // do blue avg/stdev
        cvAvgSdv(imgVal, avgVal, stdVal, null);

        // now save the values to Shared Preferences
        SharedPreferences prefs = BallLearnActivity.self.getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_READABLE);
        Editor e = prefs.edit();
        e.putString(BasicBotActivity.PREF_HUE_AVG, avgHue.getVal(0)+"");
        Log.i(BasicBotActivity.PREF_HUE_AVG, avgHue.getVal(0)+"");
        e.putString(BasicBotActivity.PREF_HUE_STD, stdHue.getVal(0)+"");
        e.putString(BasicBotActivity.PREF_SAT_AVG, avgSat.getVal(0)+"");
        e.putString(BasicBotActivity.PREF_SAT_STD, stdSat.getVal(0)+"");
        e.putString(BasicBotActivity.PREF_VAL_AVG, avgVal.getVal(0)+"");
        e.putString(BasicBotActivity.PREF_VAL_STD, stdVal.getVal(0)+"");
        e.commit();
        
        // Free memory
        cvReleaseImage(imgHue);
        cvReleaseImage(imgSat);
        cvReleaseImage(imgVal);
        cvReleaseImage(bgrImage);
        cvReleaseImage(hsvImage);
    }

    /**
     * Actually draw the overlay
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        // it is possible that onDraw was called before onPreviewFrame, in which
        // case we won't have an IplImage to use for processing... if that
        // happens, just return, or we'll crash
        if (scratchImg == null)
            return;

        // compute scaling factor between screen geometry and image geometry
        float scaleX = (float) getWidth() / scratchImg.width();
        float scaleY = (float) getHeight() / scratchImg.height();

        // draw the viewport
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        CvRect r = new CvRect(vpLeft, vpTop, vpWidth, vpHeight);
        int x = r.x(), y = r.y(), w = r.width(), h = r.height();
        canvas.drawRect(x * scaleX, y * scaleY, (x + w) * scaleX, (y + h) * scaleY, paint);

        // if we don't have capture data yet, keep running
        if (!hasCaptureData)
            return;

        // dump avg/stdev info
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);
        String s;
        s = "hue: avg = " + avgHue.getVal(0) + ", sd = " + stdHue.getVal(0);
        canvas.drawText(s, 10, 120, paint);
        s = "sat: avg = " + avgSat.getVal(0) + ", sd = " + stdSat.getVal(0);
        canvas.drawText(s, 10, 150, paint);
        s = "val: avg = " + avgVal.getVal(0) + ", sd = " + stdVal.getVal(0);
        canvas.drawText(s, 10, 180, paint);
    }
}
