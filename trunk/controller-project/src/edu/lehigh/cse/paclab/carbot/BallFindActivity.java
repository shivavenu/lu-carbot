package edu.lehigh.cse.paclab.carbot;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
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
public class BallFindActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Create a layout, then nest a View inside a SurfaceView inside the
        // layout
        RelativeLayout layout = new RelativeLayout(this);
        BallFindOverlayView overlay = new BallFindOverlayView(this);
        CameraPreviewSurfaceView camera = new CameraPreviewSurfaceView(this, overlay);
        
        layout.addView(camera);
        layout.addView(overlay);
        setContentView(layout);
    }
}

/**
 * This view is where we draw things that appear 'on top of' the camera preview.
 */
class BallFindOverlayView extends View implements Camera.PreviewCallback
{
    /**
     * A buffer for storing an RGB representation of the bytes of the preview
     * image
     */
    private int[] rgb;

    /**
     * Keep an image around, so that we don't have a lot of memory churn
     */
    private IplImage scratchImage;

    /**
     * Simple constructor: we don't have to do anything special...
     * 
     * @param context
     *            the activity where this resides
     */
    public BallFindOverlayView(Activity context)
    {
        super(context);
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

    // this tells us the center of the red point.
    private CvPoint mPointRed = new CvPoint(0, 0);

    // this tells us the center of the yellow point
    private CvPoint mPointYellow = new CvPoint(0, 0);

    // the threshold for finding a red point
    private CvScalar startColorRedHSV = new CvScalar(160, 100, 100, 0);
    private CvScalar endColorRedHSV = new CvScalar(180, 255, 255, 0);

    // the threshold for finding a yellow point
    private CvScalar startColorYellowHSV = new CvScalar(20, 100, 100, 0);
    private CvScalar endColorYellowHSV = new CvScalar(30, 255, 255, 0);

    // holders for average and stdev
    CvScalar avRed;
    CvScalar sdRed;
    CvScalar avGreen;
    CvScalar sdGreen;
    CvScalar avBlue;
    CvScalar sdBlue;

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
        // it would be good to downsample the image...

        // Create an image for the output
        // IplImage* out = cvCreateImage( cvSize(img->width/2,img->height/2),
        // img->depth, img->nChannels );
        // Reduce the image by 2
        // cvPyrDown( img, out );

        // make sure our temp fields are configured
        if (scratchImage == null) {
            scratchImage = IplImage.create(width, height, IPL_DEPTH_8U, 4);
            rgb = new int[width * height];
            avRed = new CvScalar(2);
            sdRed = new CvScalar(2);
            avGreen = new CvScalar(2);
            sdGreen = new CvScalar(2);
            avBlue = new CvScalar(2);
            sdBlue = new CvScalar(2);
        }

        ImageUtils.decodeYUV420SP(rgb, yuvData, width, height);

        // dump the image data into our overlay image
        scratchImage.getIntBuffer().put(rgb);

        // try doing ROI
        cvSetImageROI(scratchImage, cvRect(30, 30, 130, 130));

        // copy the image to the processing scratchpad, then threshold it
        // for RED, get coordinates of red blob, threshold it for YELLOW,
        // get coordinates of yellow blob. Be sure to release the buffer
        // each time, so we can reclaim the memory
        IplImage thresholdBuffer = ImageUtils.getThresholdedImageHSV(scratchImage, startColorRedHSV, endColorRedHSV);
        mPointRed = ImageUtils.getCoordinates(thresholdBuffer);
        cvReleaseImage(thresholdBuffer);
        thresholdBuffer = ImageUtils.getThresholdedImageHSV(scratchImage, startColorYellowHSV, endColorYellowHSV);
        mPointYellow = ImageUtils.getCoordinates(thresholdBuffer);
        cvReleaseImage(thresholdBuffer);

        // if we have red and yellow, then put their circle coordinates on
        // the screen
        // if (mPointYellow.x() > 0 && mPointYellow.y() > 0 && mPointRed.x() > 0
        // && mPointRed.y() > 0) {
        // tvInfo.setText("mpy = (" + mPointYellow.x() + "," + mPointYellow.y()
        // + "), mpr = (" + mPointRed.x()
        // + "," + mPointRed.y() + ")");
        // }

        postInvalidate();
    }

    /**
     * Actually draw the overlay
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        // it is possible that onDraw was called before onPreviewFrame, in which
        // case we won't have an IplImage for
        if (scratchImage == null)
            return;

        // compute scaling factor between screen geometry and image geometry
        float scaleX = (float) getWidth() / scratchImage.width();
        float scaleY = (float) getHeight() / scratchImage.height();

        // draw a circle at the red center
        if (mPointRed.x() > 0 && mPointRed.y() > 0) {
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            canvas.drawCircle(mPointRed.x() * scaleX, mPointRed.y() * scaleY, 30, paint);
        }

        // draw a circle at the yellow center
        if (mPointYellow.x() > 0 && mPointYellow.y() > 0) {
            Paint paint = new Paint();
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(mPointYellow.x() * scaleX, mPointYellow.y() * scaleY, 30, paint);
        }

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);

        String s = "FacePreview - This side up.";
        float textWidth = paint.measureText(s);
        canvas.drawText(s, (getWidth() - textWidth) / 2, 20, paint);

        // dump point information
        if (mPointYellow.x() > 0 && mPointYellow.y() > 0 && mPointRed.x() > 0 && mPointRed.y() > 0) {
            s = "mpy = (" + mPointYellow.x() + "," + mPointYellow.y() + "), mpr = (" + mPointRed.x() + ","
                    + mPointRed.y() + ")";
            canvas.drawText(s, 10, 60, paint);
        }

        // dump avg/stdev info
        s = "red: avg = " + avRed.getVal(0) + ", stdev = " + sdRed.getVal(0) + "\n";
        canvas.drawText(s, 10, 120, paint);
        s = "green: avg = " + avGreen.getVal(0) + ", stdev = " + sdGreen.getVal(0) + "\n";
        canvas.drawText(s, 10, 150, paint);
        s = "blue: avg = " + avBlue.getVal(0) + ", stdev = " + sdBlue.getVal(0) + "\n";
        canvas.drawText(s, 10, 180, paint);

        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        CvRect r = new CvRect(30, 30, 130, 130);
        int x = r.x(), y = r.y(), w = r.width(), h = r.height();
        canvas.drawRect(x * scaleX, y * scaleY, (x + w) * scaleX, (y + h) * scaleY, paint);
    }
}