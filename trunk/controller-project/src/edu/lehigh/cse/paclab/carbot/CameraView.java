package edu.lehigh.cse.paclab.carbot;

import java.io.IOException;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * The CameraView is where the picture is drawn on the screen when we are getting camera images, marking them up, and
 * using them to drive the robot.
 */
public class CameraView extends SurfaceView
{
    /**
     * A debugging tag, to help disambiguate the sources of log messages
     */
    private static final String TAG        = "CameraView";

    /**
     * This is our camera object. The way these work in Android is that we configure the camera, and then we attach a
     * callback to it. The callback we use is a buffered callback, which means we have a mechanism for managing byte
     * arrays containing the current preview image. Our strategy is to have the callback process this buffer and then
     * display the processed buffer on the screen, rather than simply display the buffer immediately to screen. In this
     * manner, we can both (a) draw on the image, and (b) analyze the image to determine how to change the robot's
     * behavior.
     */
    private Camera              mCamera;

    /**
     * Technically, this code is defining a View that can be displayed in an activity, and the speicific View type is
     * SurfaceView. Broadly speaking, a SurfaceView is a view that displays graphics, and in order to do it right, we
     * need to have a "SurfaceHolder" that encapsulates the SurfaceView and provides an interface for controlling the
     * surface size and format, and for drawing/changing actual pixels.
     * 
     * From http://developer.android.com/reference/android/view/SurfaceHolder.html:
     * 
     * When using this interface from a thread other than the one running its SurfaceView, you will want to carefully
     * read the methods lockCanvas() and Callback.surfaceCreated().
     * 
     * Note that mHolder has a bunch of interesting methods, such as setSizeFromLayout, setKeepScreenOn, setFormat,
     * setType, and isCreating.
     */
    private SurfaceHolder       mHolder;

    /**
     * We are using the callback mechanism that implicitly manages buffers. To that end, we need to have a byte array
     * that we can continually re-cycle through the callback.
     */
    private byte[]              mBuffer;

    /**
     * Whenever the callback gives us data in the buffer, we copy it into this array and then process it. The copy
     * allows us to let the image processing run asynchronously with respect to the camera's generation of preview
     * images: once we copy into the mFrame, we can let the camera continue. If it takes a long time to process the
     * image, we'll drop some frames, but the camera and main thread won't block waiting for the processing of mFrame.
     */
    private byte[]              mFrame;

    /**
     * In HoneyComb and later, we are limited in our ability to draw to a SurfaceHolder. The issue has to do with
     * threading: if we connect the holder to the camera so that the camera can display images, then we can't draw on
     * the holder from another thread.
     * 
     * The solution is to instruct the Camera to draw its preview onto a SurfaceTexture. We just *ignore* the texture,
     * but since the pixels go here, the camera is happy. Then, the previewCallback of the camera still runs as before,
     * and it also has a copy of the preview (as a byte array), and everything works just like in Android 2.3.
     * 
     * Note that for some odd reason, we need to keep a reference to the Texture, or else this crashes on a Nexus 4.
     */
    private SurfaceTexture      myTexture;

    /**
     * When we are shutting down the activity, we need to have a means of coordinating the destruction of the Surface
     * with the execution of the render thread, so that the render thread is stopped before the surface is destroyed. We
     * use this flag for that purpose.
     */
    private volatile boolean    mThreadRun = false;

    /**
     * Store the width of the Camera image
     */
    private int                 mFrameWidth;

    /**
     * Store the height of the Camera image
     */
    private int                 mFrameHeight;

    /**
     * Store the temporary matrices and other variables needed by our OpenCV code
     */
    private OpenCVTempVars      ocvVars;

    /**
     * The constructor simply sets up a holder callback and a touch listener. The holder callback will, in turn, start
     * up a thread for doing the rendering.
     * 
     * @param context
     *            The context (Activity) in which this view appears
     */
    public CameraView(Context context)
    {
        super(context);
        Log.i(TAG, "CameraView constructor");

        // get the SurfaceHolder for this SurfaceView, and attach a callback. The callback will execute thrice: on
        // create we'll run the onCreate code, immediately followed by the onSurfaceChanged code. When the activity
        // shuts down, we'll call the onDestroy code.
        mHolder = getHolder();
        mHolder.addCallback(new HolderCallback());
        // this is required on pre 3.0 devices
        // mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // set up a touch listener, so that we can use touches of the surface to both (a) identify what color to look
        // for, and (b) determine if we are currently supposed to let the robot move or not.
        setOnTouchListener(new Listener());
    }

    /**
     * We can't actually open the camera right away, since we need to get some OpenCV configuration done first. To
     * handle this, we'll require the Activity that is displaying this view to call this method after the OpenCV manager
     * is all connected and configured.
     * 
     * @return Flag indicating whether we were able to open up a camera or not
     */
    public boolean openCamera()
    {
        Log.i(TAG, "opening camera");

        // Just in case we are calling this in some strange setting where we already have the camera, let's release it
        // first
        releaseCamera();

        // now try to open the camera. If it fails, return false
        mCamera = Camera.open();
        if (mCamera == null) {
            Log.e(TAG, "Can't Open Camera");
            return false;
        }

        // we've got a camera! Now we need to set up the callback that describes what to do whenever the camera has a
        // new preview image. Our callback is simple: it just copies the bytes of the preview into mFrame and alerts our
        // render thread that there is work to do.
        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback()
        {
            public void onPreviewFrame(byte[] data, Camera camera)
            {
                // copy the bytes of the preview from data to mFrame
                synchronized (CameraView.this) {
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    CameraView.this.notify();
                }
                // NB: since we are using the 'buffered' version, we know that 'data' is actually 'mBuffer', and that
                // 'mBuffer' is now out of use. We can send 'mBuffer' right back to the camera's buffer pool, so that it
                // is reused for the next iteration of this callback cycle.
                camera.addCallbackBuffer(mBuffer);
            }
        });
        return true;
    }

    /**
     * Release the camera, so that it can be used by another application. Note that when the Activity pauses, we need to
     * release the camera, so this must be public.
     */
    public void releaseCamera()
    {
        Log.i(TAG, "Releasing Camera");

        // tell the Render thread to stop
        mThreadRun = false;

        // now stop previewing the camera and shut it down
        synchronized (CameraView.this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        // clean up the scratch variables used by OpenCV for processing the images
        if (ocvVars != null)
            ocvVars.cleanup();

        // TODO: We aren't actually acknowledging that the Render thread has stopped. It is possible that the render
        // thread just called mHolder.lockCanvas(), and wants to update the SurfaceView. If we reclaim that View now,
        // the Render thread could get a FC. For now, we'll just let it slide, since we haven't seen an error yet, but
        // eventually this is something we should worry about.
    }

    /**
     * This code sets up the Camera and gets a preview running
     * 
     * @param width
     *            The width of the surface to which we are connecting the camera
     * @param height
     *            The height of the surface to which we are connecting the camera
     */
    private void setupCamera(int width, int height)
    {
        // TODO: does this actually need to be synchronized?
        synchronized (CameraView.this) {
            // if we don't have a camera yet, we can't do this
            if (mCamera == null)
                return;

            // guess a size for the camera preview
            mFrameWidth = width;
            mFrameHeight = height;

            // now update our guess based on the camera's properties
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            int minDiff = Integer.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if ((Math.abs(size.height - height) < minDiff)) {
                    mFrameWidth = size.width;
                    mFrameHeight = size.height;
                    minDiff = Math.abs(size.height - height);
                }
            }
            params.setPreviewSize(mFrameWidth, mFrameHeight);

            // Change the focus mode of the camera to hopefully reduce blur
            List<String> FocusModes = params.getSupportedFocusModes();
            if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setParameters(params);

            // Set up our processing buffers. The way this works is that we need two buffers: one into which the camera
            // dumps a preview, and one into which we copy that preview so that we can process it without preventing the
            // camera from continuing to operate
            //
            // NB: buffer size must be in bytes, not pixels!
            int size = mFrameWidth * mFrameHeight;
            size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
            mBuffer = new byte[size];
            mFrame = new byte[size];

            // connect the camera to its scratch buffer
            mCamera.addCallbackBuffer(mBuffer);

            // Tell the camera how to display its preview images. Note that we actually don't want it to display
            // previews, so we have to turn off previews in an OS-specific way
            try {
                // for 3.0 and later, we create a SurfaceTexture and let the camera dump its picture there.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    myTexture = new SurfaceTexture(10);
                    mCamera.setPreviewTexture(myTexture);
                }
                // for older Android versions, we can just give a null preview display window
                else {
                    mCamera.setPreviewDisplay(null);
                }
            }
            catch (IOException e) {
                Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
            }

            // set up the temporary variables that our OpenCV code will use
            ocvVars = new OpenCVTempVars(params.getPreviewSize().width, params.getPreviewSize().height);

            // tell the camera to start displaying a preview (and routing the preview to our callback)
            mCamera.startPreview();
        }
    }

    /**
     * During the processing of images, it is useful to have a collection of Matrices, Scalars, and other OpenCV-related
     * variables on hand. For simplicity when configuring and cleaning up, we encapsulate all of the OpenCV temporary
     * variables into this class.
     */
    private class OpenCVTempVars
    {
        Mat           yuvMatrix;                                    // matrix for storing YUV-colored image
        Mat           rgbaMatrix;                                   // matrix for storing RGBA-colored image
        Mat           tmpMatrix;                                    // temporary result goes here

        Bitmap        mBitmap;                                      // a bitmap for displaying matrix on UI

        boolean       mIsColorSelected = false;                     // flag for if we have a color to chase right now
        Scalar        mBlobColorRgba   = new Scalar(255);
        Scalar        mBlobColorHsv    = new Scalar(255);
        ColorDetector mDetector        = new ColorDetector();       // a ColorDetector object, used for finding
                                                                     // contours and doing other cool stuff
        final Scalar  CONTOUR_COLOR    = new Scalar(255, 0, 0, 255); // color of the outline of contours we draw

        Scalar        locatedBox       = new Scalar(255, 0, 0, 0);  // a red box for now...

        /**
         * Initialize all variables, based on the width and height of the Camera preview
         * 
         * @param width
         *            - the width of the preview frames delivered by the camera
         * @param height
         *            - the height of the preview frames delivered by the camera
         */
        OpenCVTempVars(int width, int height)
        {
            // initialize Matrices
            yuvMatrix = new Mat(mFrameHeight + mFrameHeight / 2, mFrameWidth, CvType.CV_8UC1);
            rgbaMatrix = new Mat();
            tmpMatrix = new Mat();

            // build the bitmap that we will display to the screen
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        /**
         * When we close the camera, call this to free up the memory-intensive objects used for image processing.
         */
        void cleanup()
        {
            if (mBitmap != null)
                mBitmap.recycle();
            mBitmap = null;

            if (yuvMatrix != null)
                yuvMatrix.release();
            yuvMatrix = null;

            if (rgbaMatrix != null)
                rgbaMatrix.release();
            rgbaMatrix = null;

            if (tmpMatrix != null)
                tmpMatrix.release();
            tmpMatrix = null;
        }
    }

    /**
     * We render in a separate thread, which we create from this Runnable. This keeps us from doing processing on the
     * main or UI thread, and also helps us separate the different parts of this part of CarBot
     */
    private class RenderThread implements Runnable
    {
        /**
         * A tag for helping us distinguish log messages
         */
        static final String TAG = "CameraView.RenderThread";

        /**
         * The main loop of the render thread
         */
        public void run()
        {
            Log.i(TAG, "Starting processing thread");

            // wait until we are signaled, then we can start actually rendering... we'll be lazy and sleep for 1ms at a
            // time until we're ready to go...
            while (!mThreadRun) {
                try {
                    Thread.sleep(1);
                }
                // It's safe to ignore interruptions, since we're in a while...
                catch (InterruptedException e) {
                }
            }

            Log.i(TAG, "processing thread: main loop");
            while (mThreadRun) {
                Bitmap bmp = null;
                // Wait for an image from the camera, then process it
                synchronized (CameraView.this) {
                    try {
                        CameraView.this.wait();
                        // TODO: do we really need to block during all of the processing, or would it suffice to split
                        // processing in half, and just build matrixYUV while holding the lock?
                        bmp = processFrame(mFrame);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // If we have a valid bitmap, draw it onto the screen
                //
                // TODO: something about how we are determining dimensions looks funny on a Nexus 4
                if (bmp != null) {
                    Canvas canvas = mHolder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawBitmap(bmp, (canvas.getWidth() - mFrameWidth) / 2,
                                (canvas.getHeight() - mFrameHeight) / 2, null);
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        /**
         * This is the code for analyzing and processing an image
         * 
         * @param data
         *            a YUV-format byte array
         * @return a Bitmap object ready for display, or null
         */
        protected Bitmap processFrame(byte[] data)
        {
            // make a YUV-format matrix from the data, then convert it to RGBA
            //
            // NB: we ultimately need hsv, but OpenCV doesn't let us convert YUV direct to HSV
            ocvVars.yuvMatrix.put(0, 0, data);
            Imgproc.cvtColor(ocvVars.yuvMatrix, ocvVars.rgbaMatrix, Imgproc.COLOR_YUV420sp2RGB, 4);

            // if we've got a target color, run the contour algorithm
            if (ocvVars.mIsColorSelected) {
                // Find the color we're looking for
                ColorDetector cd = ocvVars.mDetector;
                cd.findColor(ocvVars.rgbaMatrix);

                // draw the region we found
                cd.drawContours(ocvVars.rgbaMatrix, ocvVars.CONTOUR_COLOR);

                // draw the swatch for the color we're looking for
                Mat colorLabel = ocvVars.rgbaMatrix.submat(2, 34, 2, 34);
                colorLabel.setTo(ocvVars.mBlobColorRgba);

                // draw the 'found' box
                cd.drawFoundBox(ocvVars.rgbaMatrix, ocvVars.locatedBox, 10);

                // drive the robot
                cd.driveRobot(ocvVars.rgbaMatrix);
            }

            // now let's update the bitmap based on our RGBA matrix
            Bitmap bmp = ocvVars.mBitmap;
            try {
                Utils.matToBitmap(ocvVars.rgbaMatrix, bmp);
            }
            catch (Exception e) {
                bmp = null;
            }
            return bmp;
        }
    }

    /**
     * A SurfaceHolder Callback object is used to define the actions taken upon creation of the surface, destruction of
     * the surface, and changes (i.e., resizes) of the surface
     */
    private class HolderCallback implements SurfaceHolder.Callback
    {
        /**
         * A tag for helping us distinguish log messages
         */
        static final String TAG = "CameraView.HolderCallback";

        /**
         * When the surface is created, we instantiate a new thread that will be responsible for drawing onto the
         * surface. Note that we must be careful not to start drawing prematurely.
         */
        public void surfaceCreated(SurfaceHolder holder)
        {
            Log.i(TAG, "surfaceCreated");
            // create the render thread. Note that it will block immediately, until surfaceChanged is done
            Thread t = new Thread(new RenderThread());
            t.start();
        }

        /**
         * Whenever the surface changes, we set up a camera and enable the render thread. This is always called after
         * surfaceCreated, so that we know it's appropriate to kick off the render thread.
         * 
         * TODO: this won't behave correctly if we actually have a subsequent surfaceChanged call!
         */
        public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height)
        {
            Log.i(TAG, "surfaceChanged");
            // the surface is sized correctly, so we can use its dimensions to set up the camera
            setupCamera(width, height);
            // now allow the renderer to actually execute
            mThreadRun = true;
        }

        /**
         * When the surface is destroyed, this will release the camera.
         * 
         * TODO: This should be sure that the render thread is dead before it returns.
         */
        public void surfaceDestroyed(SurfaceHolder holder)
        {
            Log.i(TAG, "surfaceDestroyed");
            releaseCamera();
        }
    }

    /**
     * When the preview image is touched, we take the color of the location that was touched, and we use it as the
     * target color. If the image is touched again, we drop the target color.
     */
    private class Listener implements OnTouchListener
    {
        /**
         * A quick helper method for computing the RGBA color of an HSV pixel
         * 
         * @param hsvColor
         *            The HSV pixel
         * @return The RGBA value
         */
        private Scalar convertScalarHsv2Rgba(Scalar hsvColor)
        {
            // Just make a matrix from the color, and then use existing OpenCV methods to convert to scalar
            //
            // TODO: Are these matrices causing a memory leak?
            Mat pointMatRgba = new Mat();
            Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
            Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
            return new Scalar(pointMatRgba.get(0, 0));
        }

        /**
         * On any touch, we toggle the color selection mode, and if necessary, figure out the color that we are supposed
         * to have the robot follow
         */
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            // swallow non-down presses
            if (event.getAction() != MotionEvent.ACTION_DOWN)
                return true;

            // if we were tracking a color, this will cause us to stop tracking and stop the robot
            if (ocvVars.mIsColorSelected) {
                ocvVars.mIsColorSelected = false;
                Log.d("Driving", "STOP");
                ColorDetectionActivity.self.HLT();
                return true;
            }

            // TODO: I think this is deprecated
            ColorDetectionActivity.lastEventTime = System.currentTimeMillis();

            // convert the screen coordinates of the touch into coordinates on the image
            int cols = ocvVars.rgbaMatrix.cols();
            int rows = ocvVars.rgbaMatrix.rows();
            int xOffset = (getWidth() - cols) / 2;
            int yOffset = (getHeight() - rows) / 2;
            int x = (int) event.getX() - xOffset;
            int y = (int) event.getY() - yOffset;
            Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

            // fail if we can't make sense of the touch
            if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
                return false;

            // Create a 4x4 rectangle representing the touch
            //
            // NB: we use the touch point as the bottom right, so there is no risk of overflow
            Rect touchedRect = new Rect();
            touchedRect.x = (x > 4) ? x - 4 : 0; // prevent x underflow
            touchedRect.y = (y > 4) ? y - 4 : 0; // prevent y underflow
            touchedRect.width = 4;
            touchedRect.height = 4;

            // Get the color of the rectangle, convert to HSV
            Mat touchedRegionRgba = ocvVars.rgbaMatrix.submat(touchedRect);
            Mat touchedRegionHsv = new Mat();
            Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

            // Calculate average color of touched region as HSV, send it to the color detector
            ocvVars.mBlobColorHsv = Core.sumElems(touchedRegionHsv);
            int pointCount = touchedRect.width * touchedRect.height;
            for (int i = 0; i < ocvVars.mBlobColorHsv.val.length; i++) {
                ocvVars.mBlobColorHsv.val[i] /= pointCount;
            }
            ocvVars.mDetector.setHsvColor(ocvVars.mBlobColorHsv);

            // convert back to RGBA so we can display it
            ocvVars.mBlobColorRgba = convertScalarHsv2Rgba(ocvVars.mBlobColorHsv);
            Log.i(TAG, "Touched rgba color: (" + ocvVars.mBlobColorRgba.val[0] + ", " + ocvVars.mBlobColorRgba.val[1]
                    + ", " + ocvVars.mBlobColorRgba.val[2] + ", " + ocvVars.mBlobColorRgba.val[3] + ")");

            // Start drawing contours and driving the robot
            ocvVars.mIsColorSelected = true;
            return true;
        }
    }
}