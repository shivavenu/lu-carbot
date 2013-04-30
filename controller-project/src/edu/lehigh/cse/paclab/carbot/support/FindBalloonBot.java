package edu.lehigh.cse.paclab.carbot.support;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
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
public class FindBalloonBot extends BasicBotActivity
{
    public static FindBalloonBot self;

    int rotationmillis = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        self = this;

        // Create a layout, then nest a View inside a SurfaceView inside the
        // layout
        RelativeLayout layout = new RelativeLayout(this);
        BallFindOverlayView overlay = new BallFindOverlayView(this);
        CameraPreviewSurfaceView camera = new CameraPreviewSurfaceView(this, overlay);

        layout.addView(camera);
        layout.addView(overlay);

        overlay.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onToggle();
            }
        });

        setContentView(layout);

        // how long to rotate 360 degrees?
        SharedPreferences prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity",
                Activity.MODE_WORLD_WRITEABLE);
        rotationmillis = Integer.parseInt(prefs.getString(PREF_TAG_ROTATE, "5000"));

        initBTStatus();
        allowedtorun = false;
    }

    private synchronized void onToggle()
    {
        allowedtorun = !allowedtorun;
        if (!allowedtorun) {
            robotStop();
            moving = false;
            turning = false;
        }
    }

    void ack()
    {
        // Send an ack
        Log.i("CARBOT", "sending ack");
        btService.write("ACK".getBytes());
    }

    @Override
    protected void receiveMessage(byte[] readBuf, int bytes)
    {
        String msg = new String(readBuf, 0, bytes);
        Log.i("CARBOT", "RECEIVED:::" + msg);
        // check for known non-int messages
        if (msg.equals("TOGGLE")) {
            ack();
            onToggle();
            return;
        }
        Toast.makeText(this, "Unknown message: " + msg, Toast.LENGTH_SHORT).show();
    }

    // track if we are moving or not
    boolean allowedtorun = false;
    boolean moving = false;
    boolean turning = false;

    /**
     * When the camera decides that it sees the ball, it calls this to let the
     * robot know that it should move
     * 
     * NB: this is a bit confusing since the camera is mounted sideways... we
     * pass in y and height, but use them as x and width
     * 
     * @param x
     * @param width
     */
    public synchronized void shouldMove(int x, int width)
    {
        if (!allowedtorun)
            return;
        if (turning)
            return;
        if (x > width - 75) {
            robotStop();
            robotPointTurnLeft();
            // set a timer for when to stop
            Intent intent = new Intent(this, AlarmLookAgainReceiver.class);
            intent.putExtra("AlarmID", alarmNum);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent,
                    0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (rotationmillis / 48),
                    pendingIntent);
            turning = true;
            moving = false;
            return;
        }
        else if (x < 75) {
            robotStop();
            robotPointTurnRight();
            // set a timer for when to stop
            Intent intent = new Intent(this, AlarmLookAgainReceiver.class);
            intent.putExtra("AlarmID", alarmNum);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent,
                    0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (rotationmillis / 48),
                    pendingIntent);
            turning = true;
            moving = false;
            return;
        }
        if (moving)
            return;
        robotForward();
        moving = true;
    }

    /**
     * When the camera decides that it doesn't see the ball, it calls this to
     * let the robot know that it should stop
     */
    public synchronized void shouldStop()
    {
        if (!allowedtorun)
            return;
        if (turning)
            return;
        if (moving) {
            robotStop();
            moving = false;
        }
        shouldSearch();
    }

    // for alarms...
    int alarmNum = 0;

    private synchronized void shouldSearch()
    {
        if (!allowedtorun)
            return;
        turning = true;
        robotClockwise();

        // set a timer for when to stop
        Intent intent = new Intent(this, AlarmLookAgainReceiver.class);
        intent.putExtra("AlarmID", alarmNum);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), alarmNum++, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (long) (rotationmillis / 24),
                pendingIntent);
    }

    // this calls asynchronously after the timer
    public synchronized void stopSearch()
    {
        turning = false;
        robotStop();
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

    // this tells us the center of the point that we found
    private CvPoint foundObjectCenter = new CvPoint(0, 0);

    // the thresholds for finding a point
    private CvScalar loThresh;
    private CvScalar hiThresh;

    private static double saturatingAdd(double x, double y, double lower, double upper)
    {
        double ans = x + y;
        if (ans > upper)
            ans = upper;
        if (ans < lower)
            ans = lower;
        return ans;
    }

    CvScalar avgHue = new CvScalar(1);
    CvScalar stdHue = new CvScalar(1);
    CvScalar avgSat = new CvScalar(1);
    CvScalar stdSat = new CvScalar(1);
    CvScalar avgVal = new CvScalar(1);
    CvScalar stdVal = new CvScalar(1);

    private void configFields(int width, int height)
    {
        // set up a scratch image and a buffer for holding RGB ints
        scratchImage = IplImage.create(width, height, IPL_DEPTH_8U, 4);
        rgb = new int[width * height];

        // compute the thresholds
        SharedPreferences prefs = FindBalloonBot.self.getSharedPreferences(
                "edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_READABLE);
        double avghue = Double.parseDouble(prefs.getString(BasicBotActivity.PREF_HUE_AVG, "0"));
        double stdhue = Double.parseDouble(prefs.getString(BasicBotActivity.PREF_HUE_STD, "0"));
        double avgsat = Double.parseDouble(prefs.getString(BasicBotActivity.PREF_SAT_AVG, "0"));
        double stdsat = Double.parseDouble(prefs.getString(BasicBotActivity.PREF_SAT_STD, "0"));
        double avgval = Double.parseDouble(prefs.getString(BasicBotActivity.PREF_VAL_AVG, "0"));
        double stdval = Double.parseDouble(prefs.getString(BasicBotActivity.PREF_VAL_STD, "0"));

        // force a big enough stdev
        if (stdhue < 10)
            stdhue = 10;
        if (stdsat < 10)
            stdsat = 10;
        if (stdval < 10)
            stdval = 10;

        double lohue = saturatingAdd(avghue, -3 * stdhue, 0, 179);
        double hihue = saturatingAdd(avghue, +3 * stdhue, 0, 179);
        double losat = saturatingAdd(avgsat, -3 * stdsat, 0, 255);
        double hisat = saturatingAdd(avgsat, +3 * stdsat, 0, 255);
        double loval = saturatingAdd(avgval, -3 * stdval, 0, 255);
        double hival = saturatingAdd(avgval, +3 * stdval, 0, 255);

        loThresh = new CvScalar(lohue, losat, loval, 0);
        hiThresh = new CvScalar(hihue, hisat, hival, 0);
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
        // it would be good to downsample the image...

        // make sure our temp fields are configured
        if (scratchImage == null) {
            configFields(width, height);
        }

        ImageUtils.decodeYUV420SP(rgb, yuvData, width, height);

        // dump the image data into our overlay image
        scratchImage.getIntBuffer().put(rgb);

        // copy the image to the processing scratchpad, then threshold it,
        // get coordinates of the blob, and then be sure to release the buffer
        // each time, so we can reclaim the memory
        IplImage thresholdBuffer = ImageUtils.getThresholdedImageHSV(scratchImage, loThresh, hiThresh);
        foundObjectCenter = ImageUtils.getCoordinates(thresholdBuffer);
        cvReleaseImage(thresholdBuffer);

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

        Log.v("DOTINFO", "(x,y) = (" + foundObjectCenter.x() + "," + foundObjectCenter.y() + "); imgWH = ("
                + scratchImage.width() + "," + scratchImage.height() + ")");

        // draw a circle at the red center
        if (foundObjectCenter.x() > 0 && foundObjectCenter.y() > 0) {
            FindBalloonBot.self.shouldMove(foundObjectCenter.y(), getHeight());
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            canvas.drawCircle(foundObjectCenter.x() * scaleX, foundObjectCenter.y() * scaleY, 30, paint);
        }
        else {
            FindBalloonBot.self.shouldStop();
        }

        // dump HSV info
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);
        String s;
        s = "lo = (" + loThresh.getVal(0) + ", " + loThresh.getVal(1) + ", " + loThresh.getVal(2) + ")";
        canvas.drawText(s, 10, 120, paint);
        s = "hi = (" + hiThresh.getVal(0) + ", " + hiThresh.getVal(1) + ", " + hiThresh.getVal(2) + ")";
        canvas.drawText(s, 10, 150, paint);
    }
}