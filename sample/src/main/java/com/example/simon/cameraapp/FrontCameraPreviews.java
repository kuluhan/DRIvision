package com.example.simon.cameraapp;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


public class FrontCameraPreviews  extends SurfaceView implements SurfaceHolder.Callback {
    public Camera mCamera;
    public SurfaceHolder mSurfaceHolder;

    class preview implements Camera.PreviewCallback {
        preview() {
        }

        public void onPreviewFrame(byte[] data, Camera arg1) {
            FrontCameraPreviews.this.invalidate();
        }
    }

    public FrontCameraPreviews(Context context, Camera camera) {
        super(context);
        this.mCamera = camera;
        mCamera.setDisplayOrientation(90);
        this.mSurfaceHolder = getHolder();
        this.mSurfaceHolder.addCallback(this);

        this.mSurfaceHolder.setType(3);
        // deprecated setting, but required on Android versions prior to 3.0
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        //Log.d("List", new StringBuilder(String.valueOf(new String[new File(Environment.getExternalStorageDirectory().getAbsolutePath()).list().length].length)).toString());
        Camera.Parameters parameters = this.mCamera.getParameters();
        Camera.Size size = getBestPreviewSize(width, height, parameters);
        Camera.Size pictureSize = getSmallestPictureSize(parameters);
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


    }

    private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else if (size.width * size.height < result.width * result.height) {
                result = size;
            }
        }
        return result;
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
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