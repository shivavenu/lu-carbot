package edu.lehigh.cse.paclab.kinderbot.support;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This view actually receives camera data and passes it through to the overlay
 * for further processing. It also handles drawing the actual picture onto the
 * screen.
 * 
 * To the best of my knowledge, the "surface" is just the camera picture as a
 * Bitmap being drawn to the screen
 * 
 * This should be generic enough to use in both the BallLearn activity and the BallFind activity
 */
public class CameraPreviewSurfaceView extends SurfaceView implements SurfaceHolder.Callback
{
    SurfaceHolder holder;
    Camera camera;
    Camera.PreviewCallback pc;

    /**
     * Standard SurfaceHolder constructor
     * 
     * @param context
     * @param previewCallback
     */
    public CameraPreviewSurfaceView(Context context, Camera.PreviewCallback previewCallback)
    {
        super(context);
        pc = previewCallback;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * When the Surface has been created, we can start rendering
     */
    public void surfaceCreated(SurfaceHolder holder)
    {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
        }
        catch (IOException exception) {
            camera.release();
            camera = null;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
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
        Camera.Parameters parameters = camera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(sizes, w, h);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        camera.setParameters(parameters);
        if (pc != null) {
            // set up a callback, allocate a buffer for the camera data
            camera.setPreviewCallbackWithBuffer(pc);
            Camera.Size size = parameters.getPreviewSize();
            byte[] data = new byte[size.width * size.height
                    * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8];
            camera.addCallbackBuffer(data);
        }
        camera.startPreview();
    }
}
