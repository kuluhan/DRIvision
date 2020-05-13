package com.example.simon.cameraapp;

/**
 * Created by Simon on 2/11/2017.
 */


import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.IOException;

import static com.example.simon.cameraapp.CameraService.safeToTakePicture;

public class CameraPreviews extends SurfaceView implements Callback {
    public Camera mCamera;
    public SurfaceHolder mSurfaceHolder;

    class preview implements PreviewCallback {
        preview() {
        }

        public void onPreviewFrame(byte[] data, Camera arg1) {
            CameraPreviews.this.invalidate();
        }
    }

    public CameraPreviews(Context context, Camera camera) {
        super(context);
        this.mCamera = camera;
        mCamera.setDisplayOrientation(90);
        this.mSurfaceHolder = getHolder();
        this.mSurfaceHolder.addCallback(this);

        this.mSurfaceHolder.setType(3);
        // deprecated setting, but required on Android versions prior to 3.0
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        System.out.println("CAMERA PREVIEW CREATED");
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (this.mCamera == null) {
            this.mCamera.setDisplayOrientation(90);
            try {
                this.mCamera.setPreviewDisplay(surfaceHolder);
                this.mCamera.setPreviewCallback(new preview());
            } catch (IOException e) {
                this.mCamera.release();
                this.mCamera = null;
            }
        }
        System.out.println("SURFACE CREATED");
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        //Log.d("List", new StringBuilder(String.valueOf(new String[new File(Environment.getExternalStorageDirectory().getAbsolutePath()).list().length].length)).toString());
        Parameters parameters = this.mCamera.getParameters();
        Size size = getBestPreviewSize(width, height, parameters);
        Size pictureSize = getSmallestPictureSize(parameters);
        if (!(size == null || pictureSize == null)) {
            parameters.setPreviewSize(size.width, size.height);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);

        }
        try {
            this.mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
        }
        this.mCamera.setParameters(parameters);
        this.mCamera.startPreview();
        safeToTakePicture = true;
        System.out.println("SURFACE CHANGED");

    }

    private Size getSmallestPictureSize(Parameters parameters) {
        Size result = null;
        for (Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else if (size.width * size.height < result.width * result.height) {
                result = size;
            }
        }
        return result;
    }

    private Size getBestPreviewSize(int width, int height, Parameters parameters) {
        Size result = null;
        for (Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else if (size.width * size.height > result.width * result.height) {
                    result = size;
                }
            }
        }
        return result;
    }
}