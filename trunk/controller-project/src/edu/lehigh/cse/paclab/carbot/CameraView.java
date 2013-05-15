package edu.lehigh.cse.paclab.carbot;

import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * TODO: I'm not clear on why we need CameraView and CameraViewBase... could we roll them into one?
 */
public class CameraView extends CameraViewBase implements OnTouchListener
{
    public static final int     VIEW_MODE_RGBA   = 0;
    private static final String TAG              = "CameraView";

    private Mat                 mYuv;
    private Mat                 mRgba;
    private Mat                 mIntermediateMat;

    private int                 mViewMode;
    private Bitmap              mBitmap;

    private boolean             mIsColorSelected = false;
    private Scalar              mBlobColorRgba   = new Scalar(255);
    private Scalar              mBlobColorHsv    = new Scalar(255);
    private ColorDetector       mDetector        = new ColorDetector();
    private Mat                 mSpectrum        = new Mat();
    private static Size         SPECTRUM_SIZE    = new Size(200, 32);

    private static final Scalar CONTOUR_COLOR    = new Scalar(255, 0, 0, 255);

    public CameraView(Context context)
    {
        super(context);
        setOnTouchListener(this);
    }

    @Override
    protected void onPreviewStarted(int previewWidth, int previewHeight)
    {
        // initialize Mats
        mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);

        mRgba = new Mat();
        mIntermediateMat = new Mat();

        mBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onPreviewStopped()
    {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }

        // Explicitly deallocate Mats
        if (mYuv != null)
            mYuv.release();
        if (mRgba != null)
            if (mIntermediateMat != null)
                mIntermediateMat.release();

        mYuv = null;
        mRgba = null;
        mIntermediateMat = null;
    }

    @Override
    protected Bitmap processFrame(byte[] data)
    {
        // [mfs] observing crashes here on Activity close/restart... we should figure this out, but for now let's just
        // be careful.
        mYuv.put(0, 0, data);
        // mRgba.put(0, 0, data);

        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
        /*
         * switch (viewMode) { case VIEW_MODE_RGBA: Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4); break;
         * }
         */
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(2, 34, 2, 34);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(2, 2 + mSpectrum.rows(), 38, 38 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        Bitmap bmp = mBitmap;

        try {
            Utils.matToBitmap(mRgba, bmp);
        }
        catch (Exception e) {
            Log.e("org.opencv.samples.puzzle15", "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            bmp = null;
        }

        return bmp;
    }

    public void setViewMode(int viewMode)
    {
        mViewMode = viewMode;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (mIsColorSelected) {
            mIsColorSelected = false;
            ColorDetectionActivity.self.HLT(0);
            return false;
        }

        ColorDetectionActivity.lastEventTime = System.currentTimeMillis();

        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (getWidth() - cols) / 2;
        int yOffset = (getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
            return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++) {
            mBlobColorHsv.val[i] /= pointCount;
        }

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] + ", "
                + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor)
    {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

}
