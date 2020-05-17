package com.example.simon.cameraapp;

/**
 * Created by Simon on 2/11/2017.
 */


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.TextureView;

import androidx.annotation.RequiresApi;

import org.tensorflow.lite.examples.detection.env.ImageUtils;

import java.io.IOException;

import static org.tensorflow.lite.examples.detection.tracking.DetectorService.previewHeight;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.previewWidth;

//import static com.example.simon.cameraapp.CameraService.rgbBytes;
//import static com.example.simon.cameraapp.CameraService.yuvBytes;

public class CameraPreviews extends TextureView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public Camera mCamera;
    public SurfaceHolder mSurfaceHolder;
    private SurfaceTexture surfaceTexture;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    public CameraPreviews(Context context, Camera camera) throws IOException {
        super(context);
        //this.mCamera = CameraService.mServiceCamera;
        //mCamera.setDisplayOrientation(90);
        surfaceTexture = new SurfaceTexture(10);
        mCamera.setPreviewTexture(surfaceTexture);
        mCamera.setPreviewCallback(this);

        //this.mSurfaceHolder = getHolder();
        mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(previewHeight, previewWidth)]);

        //this.mSurfaceHolder.setType(3);
        // deprecated setting, but required on Android versions prior to 3.0
        //mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        /*
         */
        mCamera.startPreview();
        System.out.println("CAMERA PREVIEW CREATED");
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        System.out.println("SURFACE CREATE STARTED");
        if (this.mCamera == null) {
            //this.mCamera.setDisplayOrientation(90);
            try {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.setPreviewCallback(this);
                mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(previewHeight, previewWidth)]);
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
        System.out.println("SURFACE CHANGE STARTED");
        //Log.d("List", new StringBuilder(String.valueOf(new String[new File(Environment.getExternalStorageDirectory().getAbsolutePath()).list().length].length)).toString());
        Parameters parameters = this.mCamera.getParameters();
        Size size = getBestPreviewSize(width, height, parameters);
        Size pictureSize = getSmallestPictureSize(parameters);
        if (!(size == null || pictureSize == null)) {
            parameters.setPreviewSize(size.width, size.height);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);

        }
        try {
            this.mCamera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
        }
        this.mCamera.setParameters(parameters);
        this.mCamera.startPreview();
        //safeToTakePicture = true;
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