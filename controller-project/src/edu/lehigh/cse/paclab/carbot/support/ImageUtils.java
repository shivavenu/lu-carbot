package edu.lehigh.cse.paclab.carbot.support;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvInRangeS;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGBA2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_MEDIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetCentralMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetSpatialMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMoments;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;

/**
 * This class provides a few static methods for image manipulation
 */
public class ImageUtils
{
    /**
     * From stackoverflow: convert a yuv420sp formatted byte array to a RGB
     * array
     */
    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height)
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

    /**
     * Given an HSV image and a range of color values, return a B&W image where
     * white is the region that is within the range and black is everything else
     * 
     * @param img
     *            The input HSV image
     * @param start
     *            The low end of the color range
     * @param end
     *            The high end of the color range
     * @return a new HSV image
     */
    public static IplImage getThresholdedImageHSV(IplImage img, CvScalar start, CvScalar end)
    {
        // first convert the image to BGR
        IplImage three_channel = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 3);
        cvCvtColor(img, three_channel, CV_RGBA2BGR);
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
    public static CvPoint getCoordinates(IplImage thresholdImage)
    {
        int posX = 0;
        int posY = 0;
        CvMoments moments = new CvMoments();
        cvMoments(thresholdImage, moments, 1);
        // cv Spatial moment : Mji=sumx,y(I(x,y)*xj*yi)
        // where I(x,y) is the intensity of the pixel (x, y) and '*' actually
        // means the dot product
        double momX10 = cvGetSpatialMoment(moments, 1, 0); // (x,y)
        double momY01 = cvGetSpatialMoment(moments, 0, 1);// (x,y)
        double area = cvGetCentralMoment(moments, 0, 0);
        posX = (int) (momX10 / area);
        posY = (int) (momY01 / area);
        return new CvPoint(posX, posY);
    }

}
