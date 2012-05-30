package edu.lehigh.cse.paclab.prelims;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VisionSelectView extends SurfaceView implements SurfaceHolder.Callback
{
    SurfaceHolder mHolder;
    Camera mCamera;
    VisionShowView mDrawOnTop;
    boolean mFinished;

    public VisionSelectView(Context context, VisionShowView drawOnTop)
    {
        super(context);

        mDrawOnTop = drawOnTop;
        mFinished = false;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * This is called when the surface is created... then we can start rendering
     */
    public void surfaceCreated(SurfaceHolder holder)
    {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);

            // Preview callback used whenever new viewfinder frame is available
            mCamera.setPreviewCallback(new PreviewCallback()
            {
                public void onPreviewFrame(byte[] data, Camera camera)
                {
                    // if we don't have a drawable view, or if the activity is
                    // shutting down, then don't do any drawing.
                    if ((mDrawOnTop == null) || mFinished)
                        return;

                    // if we don't have a bitmap yet, then we need to configure
                    // one
                    if (!mDrawOnTop.isConfigured())
                        mDrawOnTop.configure(camera, data.length);

                    // Pass YUV data to draw-on-top companion
                    System.arraycopy(data, 0, mDrawOnTop.mYUVData, 0, data.length);
                    mDrawOnTop.invalidate();
                }
            });
        }
        catch (IOException exception) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mFinished = true;
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h)
    {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * This is called when the surface is resized (or sized for the first time)
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(sizes, w, h);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}
