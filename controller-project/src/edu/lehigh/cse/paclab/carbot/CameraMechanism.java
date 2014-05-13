/**
 * TODO: fix formatting of license
 * 
 * Copyright (c) 2008-2012 CommonsWare, LLC Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0. Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 * 
 * From _The Busy Coder's Guide to Advanced Android Development_ http://commonsware.com/AdvAndroid
 */

// this code is based on
// https://github.com/commonsguy/cw-advandroid/blob/master/Camera/Picture/src/com/commonsware/android/picture/PictureDemo.java

package edu.lehigh.cse.paclab.carbot;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * CameraMechanism moves all of the Camera code into a separate class, so that we can keep licenses independent. This
 * code is based on the above reference, and modified only as necessary to meet our needs. It is used by RCReceiver to
 * take photos upon request from the remote controller.
 */
public class CameraMechanism
{
    /**
     * The on-screen location where we show the preview
     */
    private SurfaceView   preview          = null;

    /**
     * The wrapper around the preview image, to which we can attach callbacks
     */
    private SurfaceHolder previewHolder    = null;

    /**
     * The camera object
     */
    private Camera        camera           = null;

    /**
     * Track whether the surface is showing a preview or a snapped photo
     */
    private boolean       inPreview        = false;

    /**
     * Track if the camera is configured
     */
    private boolean       cameraConfigured = false;

    /**
     * Parent activity, so that we can toast
     */
    private Context       myContext        = null;

    /**
     * The output stream of the socket, so we can write over the network
     */
    OutputStream          myWriter         = null;

    /**
     * Call this from parentActivity's onCreate to configure the camera
     * 
     * @param parentActivity
     *            The activity owning the camera... we need this for Toasts
     */
    @SuppressWarnings("deprecation")
    void onCreateCamera(Activity parentActivity)
    {
        myContext = parentActivity;
        // configure the surface view
        preview = (SurfaceView) parentActivity.findViewById(R.id.svPreview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Method for snapping a photo
     */
    public void snap()
    {
        if (inPreview) {
            camera.takePicture(null, null, photoCallback);
            inPreview = false;
        }
    }

    /**
     * Helper method to determine the size of the preview to display, based on the input size and width
     * 
     * @param width
     *            The target width
     * 
     * @param height
     *            The target height
     * 
     * @param parameters
     *            An object encapsulating the configuration options of the camera
     * 
     * @return A target size for the preview
     */
    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters)
    {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                }
                else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Determine the smallest possible picture size supported by the camera
     * 
     * @param parameters
     *            The configuration options of the camera
     * 
     * @return A size for the picture
     */
    private Camera.Size getSmallestPictureSize(Camera.Parameters parameters)
    {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            }
            else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea < resultArea) {
                    result = size;
                }
            }
        }
        return result;
    }

    /**
     * Initialize the camera preview to show what is currently being observed by the camera
     * 
     * @param width
     *            The width of the preview
     * 
     * @param height
     *            The height of the preview
     */
    private void initPreview(int width, int height)
    {
        if (camera != null && previewHolder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
                Toast.makeText(myContext, t.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height, parameters);
                Camera.Size pictureSize = getSmallestPictureSize(parameters);

                if (size != null && pictureSize != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                    parameters.setPictureFormat(ImageFormat.JPEG);
                    camera.setParameters(parameters);
                    cameraConfigured = true;
                }
            }
        }
    }

    /**
     * Start previewing from the camera
     */
    private void startPreview()
    {
        if (cameraConfigured && camera != null) {
            camera.startPreview();
            inPreview = true;
        }
    }

    /**
     * Configure the callback that manages previewing
     * 
     * TODO: handle this within constructor, so it isn't so messy?
     */
    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback()
                                           {
                                               /**
                                                * Do nothing until the surface changes
                                                */
                                               public void surfaceCreated(SurfaceHolder holder)
                                               {
                                               }

                                               /**
                                                * When the surface changes, do a preview
                                                */
                                               public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                                       int height)
                                               {
                                                   initPreview(width, height);
                                                   startPreview();
                                               }

                                               /**
                                                * Do nothing when the surface is destroyed
                                                */
                                               public void surfaceDestroyed(SurfaceHolder holder)
                                               {
                                               }
                                           };

    /**
     * Configure the callback for when a picture is taken
     * 
     * TODO: handle this in constructor, so that the code isn't so messy?
     */
    Camera.PictureCallback photoCallback   = new Camera.PictureCallback()
                                           {
                                               /**
                                                * Shoot the photo over the network
                                                */
                                               public void onPictureTaken(byte[] data, Camera camera)
                                               {
                                                   new SavePhotoTask().execute(data);
                                                   camera.startPreview();
                                                   inPreview = true;
                                               }
                                           };

    /**
     * A background task for sending the photo, so we don't have to block during the transmission
     */
    class SavePhotoTask extends AsyncTask<byte[], String, String>
    {
        /**
         * The main behavior of the task is to zap a photo over the network
         */
        @Override
        protected String doInBackground(byte[]... jpeg)
        {
            try {
                // protocol is simply write length, then write bytes
                DataOutputStream dos = new DataOutputStream(myWriter);
                dos.writeInt(jpeg[0].length);
                dos.write(jpeg[0], 0, jpeg[0].length);
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Whenever the parent activity calls onResume, it forwards to this to manage resumption of the camera
     */
    void onResumeCamera()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    camera = Camera.open(i);
            }
        }

        if (camera == null)
            camera = Camera.open();

        startPreview();
    }

    /**
     * Whenever the parent activity calls onPause, it forwards to this to manage pausing the camera
     */
    void onPauseCamera()
    {
        if (inPreview)
            camera.stopPreview();

        camera.release();
        camera = null;
        inPreview = false;
    }
}