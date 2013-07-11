package edu.lehigh.cse.paclab.carbot;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

/**
 * ColorDetector is responsible for finding regions of color inside of a matrix, turning each region into a contour,
 * finding the biggest contour, and using that contour to udpate the display and to drive the robot.
 * 
 * Right now, the color threshold is hard-coded, which can sometimes lead to unintuitive behaviors. The best technique
 * is to choose colors carefully. Hot Pink is a great choice.
 * 
 * TODO: one possibility for refining the code would be to have two thresholds instead of one, so that we could try the
 * tight threshold first and favor whatever it detects.
 * 
 * TODO: another possibility is to favor the contour that contains the point that we tracked in the last iteration of
 * the process() function.
 */
public class ColorDetector
{
    /**
     * When we are trying to find a color, this color represents a 'minimum' threshold for colors that are similar to
     * the target color
     */
    private Scalar mLowerBound    = new Scalar(0);

    /**
     * When we are trying to find a color, this color represents a 'maximum' threshold for colors that are similar to
     * the target color
     */
    private Scalar mUpperBound    = new Scalar(0);

    /**
     * Color radius for range checking in HSV color space
     * 
     * NB: this is a hard-coded cylinder size around the color point that we are trying to find
     */
    private Scalar mColorRadius   = new Scalar(25, 50, 50, 0);

    /**
     * When we have a valid contour to chase, this stores a reference to the contour
     */
    MatOfPoint     bestContour    = null;

    /**
     * When we have a valid contour to chase, this is the contour center's Y coordinate
     */
    public int     contourCenterY = 0;

    /**
     * When we have a valid contour to chase, this is the contour center's X coordinate
     */
    public int     contourCenterX = 0;

    /**
     * Track the last move we made
     */
    private String lastMove = "";
    
    /**
     * Compute the upper and lower bounds that specify the range of colors that count as being 'identical' to the color
     * we trying to identify
     * 
     * @param hsvColor
     *            The color that we are trying to find
     */
    public void setHsvColor(Scalar hsvColor)
    {
        // set up our color search as a 3d box centered at hsvColor, then distort the box to prevent overflow or
        // underflow
        mLowerBound.val[0] = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0] - mColorRadius.val[0] : 0;
        mUpperBound.val[0] = (hsvColor.val[0] + mColorRadius.val[0] <= 255) ? hsvColor.val[0] + mColorRadius.val[0]
                : 255;
        mLowerBound.val[1] = (hsvColor.val[1] >= mColorRadius.val[1]) ? hsvColor.val[1] - mColorRadius.val[1] : 0;
        mUpperBound.val[1] = (hsvColor.val[1] + mColorRadius.val[1] <= 255) ? hsvColor.val[1] + mColorRadius.val[1]
                : 255;
        mLowerBound.val[2] = (hsvColor.val[2] >= mColorRadius.val[2]) ? hsvColor.val[2] - mColorRadius.val[2] : 0;
        mUpperBound.val[2] = (hsvColor.val[2] + mColorRadius.val[2] <= 255) ? hsvColor.val[2] + mColorRadius.val[2]
                : 255;

        // NB: scalars have a 4th channel, but since HSV doesn't have a 4th dimension, we need to set this threshold to
        // the maximum
        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;
    }

    /**
     * When given an RGBA image, this code finds the pixels within our target color spectrum, combines them into
     * contours, selects the largest contour, and saves properties of that contour for later use.
     * 
     * @param rgbaImage
     *            an RGBA image to analyze
     */
    public void findColor(Mat rgbaImage)
    {
        // Uses a Gaussian Kernel to smooth the image and downsample it twice
        Mat pyrDownMat = new Mat();
        Imgproc.pyrDown(rgbaImage, pyrDownMat);
        Imgproc.pyrDown(pyrDownMat, pyrDownMat);

        // convert the image to HSV
        Mat hsvMat = new Mat();
        Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        // Creates the binary image of the thresholded image, where pixels are either white or black, depending on if
        // they are in or out of our color threshold
        Mat bwMatrix = new Mat();
        Core.inRange(hsvMat, mLowerBound, mUpperBound, bwMatrix);

        // dilate the image so that bright regions grow (the white gets bigger)
        Mat dilatedMask = new Mat();
        Imgproc.dilate(bwMatrix, dilatedMask, new Mat());

        // find all contours (regions of whiteness) in the image
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find the best contour, by choosing the one with the largest area
        //
        // NB: no guarantees that we have any contours!
        double maxArea = 0;
        bestContour = null;
        for (MatOfPoint mop : contours) {
            double area = Imgproc.contourArea(mop);
            if (area > maxArea) {
                maxArea = area;
                bestContour = mop;
            }
        }

        // The contour's dimensions are relative to a matrix that is 1/4 the size of our real matrix. Scale the contour
        // up, then find its center
        if (bestContour != null) {
            // the two downsamplings halved the array twice, so now we need to double the dimensions of the contour
            // twice in order to scale it back to screen size
            Core.multiply(bestContour, new Scalar(4, 4), bestContour);

            // save the center of the rectangle that bounds the contour
            Rect r = Imgproc.boundingRect(bestContour);
            contourCenterX = r.x + r.width / 2;
            contourCenterY = r.y + r.height / 2;
        }
    }

    /**
     * Draw the contour onto an RGBA matrix
     * 
     * @param rgbaMatrix
     *            The matrix onto which we draw
     * @param color
     *            The color of the outline of the contours
     */
    public void drawContours(Mat rgbaMatrix, Scalar color)
    {
        // quit if we don't have a contour to draw
        if (bestContour == null)
            return;

        // draw the contours
        ArrayList<MatOfPoint> mops = new ArrayList<MatOfPoint>();
        mops.add(bestContour);
        Imgproc.drawContours(rgbaMatrix, mops, -1, color);
    }

    /**
     * Use the center point of the main contour to draw a size x size box
     * 
     * @param rgbaMatrix
     *            The matrix onto which we draw
     * @param color
     *            The color of the box we're drawing
     * @param size
     *            The size of the box
     */
    public void drawFoundBox(Mat rgbaMatrix, Scalar color, int size)
    {
        // don't draw if we don't have a valid contour
        if (bestContour == null)
            return;

        // get dimensions of the rgbaMatrix
        int maxX = rgbaMatrix.cols() - 2;
        int maxY = rgbaMatrix.rows() - 2;

        // compute initial contourCenterY contourCenterX of the box we're about to draw
        int x = contourCenterX - size / 2;
        int y = contourCenterY - size / 2;

        // handle underflow
        x = (x < 0) ? 0 : x;
        y = (y < 0) ? 0 : y;

        // handle overflow
        x = ((x + size) > maxX) ? maxX - size : x;
        y = ((y + size) > maxY) ? maxY - size : y;

        // draw the box
        Mat foundbox = rgbaMatrix
                .submat(contourCenterY - 5, contourCenterY + 5, contourCenterX - 5, contourCenterX + 5);
        foundbox.setTo(color);
    }

    /**
     * This method actually drives the robot, based on the contours that we found before
     * 
     * @param rgbaMatrix
     *            This is the original matrix used to compute the contours. We use it here so that we can determine
     *            thresholds for steering vs. driving straight
     */
    public void driveRobot(Mat rgbaMatrix)
    {
        // start a spin to look for the color we want
        if (bestContour == null) {
            Log.d("Driving", "SEARCH");
            ColorDetectionActivity.self.robotClockwise();
            return;
        }

        // determine the range of possible values for contourCenterY. Remember that the camera is in landscape mode
        int maxY = rgbaMatrix.rows();
        Log.d("DRIVING", "my (" + contourCenterX + "," + contourCenterY + ") vs. global (" + rgbaMatrix.cols() + ", "
                + rgbaMatrix.rows() + ")");

        // if we are in the bottom third, turn right
        if (contourCenterY < maxY / 3) {
            Log.d("Driving", "RIGHT");
            if (!lastMove.equals("R"))
                ColorDetectionActivity.self.robotStop();
            lastMove = "R";
            ColorDetectionActivity.self.robotPointTurnRight();
        }
        // middle third goes straight
        else if (contourCenterY < (2 * maxY / 3)) {
            if (!lastMove.equals("F"))
                ColorDetectionActivity.self.robotStop();
            lastMove = "F";
            ColorDetectionActivity.self.robotForward();
            Log.d("Driving", "FORWARD");
        }
        // top third goes left
        else {
            if (!lastMove.equals("L"))
                ColorDetectionActivity.self.robotStop();
            lastMove = "L";
            ColorDetectionActivity.self.robotPointTurnLeft();
            Log.d("Driving", "LEFT");
        }
    }
}
