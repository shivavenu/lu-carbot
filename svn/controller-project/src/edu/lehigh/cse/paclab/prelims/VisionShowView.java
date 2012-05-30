package edu.lehigh.cse.paclab.prelims;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvInRangeS;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSplit;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGRA2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HIST_ARRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_MEDIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCalcHist;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvClearHist;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCreateHist;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetCentralMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetSpatialMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMoments;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import static com.googlecode.javacv.cpp.opencv_legacy.cvQueryHistValue_1D;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc.CvHistogram;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;

public class VisionShowView extends View
{
    // This is the bitmap that we will display
    public Bitmap mBitmap;

    // width of the camera preview
    public int mImageWidth;

    // height of the camera preview
    public int mImageHeight;

    // arrays for holding the image in YUV and RGB formats
    public byte[] mYUVData;
    public int[] mRGBData;

    /**
     * Indicate whether View has been configured.
     * 
     * This view has a lot of auxilliary fields so that we can do a lot of work
     * on every onDraw, and thus we need to be sure they are all configured.
     * 
     * @return false if configure() should be called
     */
    public boolean isConfigured()
    {
        return mBitmap != null;
    }

    /**
     * Initialize the fields of the view
     * 
     * @param camera
     *            the current camera. We use its params to know how big the
     *            bitmap needs to be.
     * @param datalength
     *            the number of bytes of data from the camera
     */
    public void configure(Camera camera, int datalength)
    {
        Camera.Parameters params = camera.getParameters();
        mImageWidth = params.getPreviewSize().width;
        mImageHeight = params.getPreviewSize().height;
        mBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        mRGBData = new int[mImageWidth * mImageHeight];
        mYUVData = new byte[datalength];
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

    // this is for passing the RGB buffer so we can threshold it
    private IplImage processingScratchpad = null;

    // reference to the place where we dump text on screen
    TextView tvInfo = null;

    /**
     * Simple constructor: just forward to super
     * 
     * @param context
     */
    public VisionShowView(Context context)
    {
        super(context);
    }

    /**
     * Run an InRangeS function on an image, return a new image that is B&W
     * 
     * @param img
     *            The BGRA image we will analyze
     * @param start
     *            The minimum threshold values, in HSV format
     * @param end
     *            The maximum threshold values, in HSV format
     * @return an HSV image with the threshold
     */
    private IplImage getThresholdedImageHSV(IplImage img, CvScalar start, CvScalar end)
    {
        // first convert the image to BGR
        IplImage three_channel = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 3);
        cvCvtColor(img, three_channel, CV_BGRA2BGR);
        // now convert that to HSV
        IplImage imgHSV = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 3);
        cvCvtColor(three_channel, imgHSV, CV_BGR2HSV);
        // threshold the HSV based on the start and end vectors
        IplImage imgThreshed = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
        cvInRangeS(imgHSV, start, end, imgThreshed);
        // return memory from the images we're done with
        cvReleaseImage(imgHSV);
        cvReleaseImage(three_channel);
        // smooth out the thresholded image and return it
        cvSmooth(imgThreshed, imgThreshed, CV_MEDIAN, 13);
        return imgThreshed;
    }

    /**
     * Given a black and white image (a threshold Image), compute the moment of
     * the biggest white blob in it.
     * 
     * [mfs] I don't know what algorithm we are using here. The code is from
     * Giulio, and it appears to work.
     * 
     * @param thresholdImage
     *            the B&W image that we produced from getThresholdedImageHSV
     * 
     * @return an (x,y) point representing the center
     */
    private static CvPoint getCoordinates(IplImage thresholdImage)
    {
        int posX = 0;
        int posY = 0;
        CvMoments moments = new CvMoments();
        cvMoments(thresholdImage, moments, 1);
        // cv Spatial moment : Mji=sumx,y(I(x,y)•xj•yi)
        // where I(x,y) is the intensity of the pixel (x, y).
        double momX10 = cvGetSpatialMoment(moments, 1, 0); // (x,y)
        double momY01 = cvGetSpatialMoment(moments, 0, 1);// (x,y)
        double area = cvGetCentralMoment(moments, 0, 0);
        posX = (int) (momX10 / area);
        posY = (int) (momY01 / area);
        return new CvPoint(posX, posY);
    }

    /**
     * From stackoverflow: convert a yuv420sp formatted byte array to a RGB
     * array
     */
    private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height)
    {
        int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;

                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((b << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((r >> 10) & 0xff);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        // set up the textview
        if (tvInfo == null) {
            tvInfo = new TextView(VisionActivity._self);
            VisionActivity._self.relativeLayout.addView(tvInfo);
        }
        tvInfo.setText("");

        // if we've got a bitmap object, we can figure out what to draw in it
        if (mBitmap != null) {
            // get an RGB image from the camera's YUV image... note that we
            // populated YUVData in SelectView and then invalidated this
            // ShowView to force onDraw to run, so we know that YUVData has the
            // latest image
            decodeYUV420SP(mRGBData, mYUVData, mImageWidth, mImageHeight);

            // draw the picture onto our bitmap
            mBitmap.setPixels(mRGBData, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight);

            // make sure we have room for copying the image, so that we can
            // threshold it
            if (processingScratchpad == null)
                processingScratchpad = IplImage.create(mImageWidth, mImageHeight, IPL_DEPTH_8U, 4);

            // copy the image to the processing scratchpad, then threshold it
            // for RED, get coordinates of red blob, threshold it for YELLOW,
            // get coordinates of yellow blob. Be sure to release the buffer
            // each time, so we can reclaim the memory
            mBitmap.copyPixelsToBuffer(processingScratchpad.getByteBuffer());
            IplImage thresholdBuffer = getThresholdedImageHSV(processingScratchpad, startColorRedHSV, endColorRedHSV);
            mPointRed = getCoordinates(thresholdBuffer);
            cvReleaseImage(thresholdBuffer);
            thresholdBuffer = getThresholdedImageHSV(processingScratchpad, startColorYellowHSV, endColorYellowHSV);
            mPointYellow = getCoordinates(thresholdBuffer);
            cvReleaseImage(thresholdBuffer);

            // now dump histograms
            dumpHistograms(processingScratchpad);

            // draw a circle at the red center
            if (mPointRed.x() > 0 && mPointRed.y() > 0) {
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                canvas.drawCircle(mPointRed.x(), mPointRed.y(), 30, paint);
            }

            // draw a circle at the yellow center
            if (mPointYellow.x() > 0 && mPointYellow.y() > 0) {
                Paint paint = new Paint();
                paint.setColor(Color.YELLOW);
                canvas.drawCircle(mPointYellow.x(), mPointYellow.y(), 30, paint);
            }

            // if we have red and yellow, then put their circle coordinates on
            // the screen
            if (mPointYellow.x() > 0 && mPointYellow.y() > 0 && mPointRed.x() > 0 && mPointRed.y() > 0) {
                tvInfo.setText("mpy = (" + mPointYellow.x() + "," + mPointYellow.y() + "), mpr = (" + mPointRed.x()
                        + "," + mPointRed.y() + ")");
            }
        }
    }

    /**
     * Compute histograms on an image. I think we can use this to have the phone
     * figure out which ball it is looking at
     */
    void dumpHistograms(IplImage img)
    {
        // first convert the image to BGR
        IplImage bgrImage = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 3);
        cvCvtColor(img, bgrImage, CV_BGRA2BGR);

        int[] numbins = { 256 };
        float[] range = { 0, 255 };
        float[][] ranges = { range };
        CvHistogram hist = cvCreateHist(1, numbins, CV_HIST_ARRAY, ranges, 1);
        cvClearHist(hist);

        IplImage imgRed = cvCreateImage(cvGetSize(bgrImage), 8, 1);
        IplImage imgGreen = cvCreateImage(cvGetSize(bgrImage), 8, 1);
        IplImage imgBlue = cvCreateImage(cvGetSize(bgrImage), 8, 1);

        // NB: BGR... is that right?
        cvSplit(bgrImage, imgBlue, imgGreen, imgRed, null);

        // do red:
        IplImage[] send = { imgRed };
        cvCalcHist(send, hist, 0, null);
        dumpHist(hist, "Red");
        cvClearHist(hist);

        // do green:
        send[0] = imgGreen;
        cvCalcHist(send, hist, 0, null);
        dumpHist(hist, "Green");
        cvClearHist(hist);

        // do blue:
        send[0] = imgBlue;
        cvCalcHist(send, hist, 0, null);
        dumpHist(hist, "Blue");
        cvClearHist(hist);
    }

    void dumpHist(CvHistogram hist, String tag)
    {
        Log.i("SHOWVIEW", "Dumping histogram " + tag);
        for (int i = 0; i < 256; ++i) {
            float val = cvQueryHistValue_1D(hist, i);
            Log.i("SHOWVIEW", i + ":" + val);
        }
    }

}
