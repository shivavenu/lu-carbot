package edu.lehigh.cse.paclab.carbot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

// TODO: this code is not commented or well explained right now
public class ColorDetector
{
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar           mLowerBound     = new Scalar(0);
    private Scalar           mUpperBound     = new Scalar(0);

    // Minimum contour area in percent for contours filtering
    private static double    mMinContourArea = 0.1;

    // Color radius for range checking in HSV color space
    private Scalar           mColorRadius    = new Scalar(25, 50, 50, 0);
    private Mat              mSpectrum       = new Mat();
    private List<MatOfPoint> mContours       = new ArrayList<MatOfPoint>();

    public void setColorRadius(Scalar radius)
    {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor)
    {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0] - mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0] + mColorRadius.val[0] <= 255) ? hsvColor.val[0] + mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int) (maxH - minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH - minH; j++) {
            byte[] tmp = { (byte) (minH + j), (byte) 255, (byte) 255 };
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);

    }

    public Mat getSpectrum()
    {
        return mSpectrum;
    }

    public void setMinContourArea(double area)
    {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage)
    {
        Mat pyrDownMat = new Mat();

        // Uses a Gaussian Kernel to smooth the image
        Imgproc.pyrDown(rgbaImage, pyrDownMat);
        Imgproc.pyrDown(pyrDownMat, pyrDownMat);

        Mat hsvMat = new Mat();
        Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        Mat Mask = new Mat();
        // Creates the binary image of the thresholded image
        Core.inRange(hsvMat, mLowerBound, mUpperBound, Mask);
        Mat dilatedMask = new Mat();
        Imgproc.dilate(Mask, dilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        int indexOfLargestContour = 0;
        int i = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea) {
                maxArea = area;
                indexOfLargestContour = i;
            }
            i++;
        }

        // Filter contours by area and resize to fit the original image size
        /*
         * mContours.clear(); each = contours.iterator(); while (each.hasNext()) { MatOfPoint contour = each.next(); if
         * (Imgproc.contourArea(contour) > mMinContourArea * maxArea) { Core.multiply(contour, new Scalar(4, 4),
         * contour); mContours.add(contour); } }
         */

        // Filter the main contour by area and resize to fit the original image
        mContours.clear();
        if (!contours.isEmpty()) {
            MatOfPoint contour = contours.get(indexOfLargestContour);
            if (Imgproc.contourArea(contour) > mMinContourArea * maxArea) {
                Core.multiply(contour, new Scalar(4, 4), contour);
                mContours.add(contour);
                int x = Imgproc.boundingRect(contour).x;
                int y = Imgproc.boundingRect(contour).y;

                if (y > 300) {
                    ColorDetectionActivity.self.PTL();
                }
                else if (300 > y && y > 100) {
                    ColorDetectionActivity.self.FWD();
                }
                else if (100 > y) {
                    ColorDetectionActivity.self.PTR();
                }
            }
        }
        else {
            ColorDetectionActivity.self.CW();
        }

    }

    /**
     * TODO: do we really need getters and setters here, or could we just make the fields more visible?
     */
    public List<MatOfPoint> getContours()
    {
        return mContours;
    }
}
