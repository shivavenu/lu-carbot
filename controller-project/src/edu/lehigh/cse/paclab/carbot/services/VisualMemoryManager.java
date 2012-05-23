package edu.lehigh.cse.paclab.carbot.services;

import android.app.Application;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;

/**
 * This will ultimately become a singleton for storing all of the different
 * images that we have encountered during a particular execution. For now, it
 * just holds the most recent image
 */
public class VisualMemoryManager
{
    // we represent the image as six values: the average and stdev in each of
    // the Hue, Saturation, and Value dimensions
    public static CvScalar avgHue;
    public static CvScalar stdHue;
    public static CvScalar avgSat;
    public static CvScalar stdSat;
    public static CvScalar avgVal;
    public static CvScalar stdVal;

    /**
     * Initialize the singleton by creating storage for each of the fields
     */
    public static void initialize(Application appContext)
    {
        avgHue = new CvScalar(2);
        stdHue = new CvScalar(2);
        avgSat = new CvScalar(2);
        stdSat = new CvScalar(2);
        avgVal = new CvScalar(2);
        stdVal = new CvScalar(2);
    }
}
