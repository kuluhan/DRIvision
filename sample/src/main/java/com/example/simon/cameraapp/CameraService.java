package com.example.simon.cameraapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.tensorflow.lite.examples.detection.customview.AutoFitTextureView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.tracking.DetectorService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.MainActivity;

import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.show;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.imageSaver;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.isProcessingFrame;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.previewHeight;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.previewWidth;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.processImage;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.readyForNextImage;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.readyForNextImage2;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.rgbBytes;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.yuvBytes;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.recentPics;
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class CameraService extends Service implements Camera.PreviewCallback {
    String[] ImagePath;

    Camera.PictureCallback mPicture;
    Camera.PictureCallback mPictureBack;
    //public TextureView mSurfaceView;
    public static Camera mServiceCamera;
    private SurfaceView mBackSurfaceView;
    private static Camera mBackServiceCamera;
    private boolean isPlay = false;
    public static final String PREFS = "CAMERA_APP";
    protected SharedPreferences prefs;
    int count = 0;
    public static boolean safeToTakePicture = false;

    public static String TAG = "DualCamActivity";

    //private LinearLayout mllFirst;
    public static CameraPreviews mCameraPreview;
    private SurfaceTexture surfaceTexture;

    //public static int[] rgbBytes = null;
    //public static byte[][] yuvBytes = new byte[3][];
    //public static int yRowStride;
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private AutoFitTextureView textureView;
    private AutoFitTextureView textureView2;
    private WindowManager mWindowManager;
    private WindowManager mWindowManager2;
    public static View tex;
    public static View tex2;
    private HandlerThread backgroundThread;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isProcessingFrame) {
            //LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                //onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            //LOGGER.e(e, "Exception!");
            return;
        }
        recentPics.add(data);
        isProcessingFrame = true;
        yuvBytes[0] = data;
        imageSaver= new Runnable() {
            @Override
            public void run() {
                camera.addCallbackBuffer(data);
                recentPics.add(data);
                while (recentPics.size()>300)
                    recentPics.remove();
                System.out.println("AddingNEWDATA"+recentPics.size());
                readyForNextImage();
            }
        };

        readyForNextImage();
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {

                    try {
                        Camera.Parameters parameters = mServiceCamera.getParameters();
                        List<String> focusModes = parameters.getSupportedFocusModes();
                        if (focusModes != null
                                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
                        Size[] sizes = new Size[cameraSizes.size()];
                        int i = 0;
                        for (Camera.Size size : cameraSizes) {
                            sizes[i++] = new Size(size.width, size.height);
                        }
                        Size previewSize =
                                CameraService.chooseOptimalSize(
                                        sizes, 1, 1);
                        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
                        mServiceCamera.setDisplayOrientation(90);
                        mServiceCamera.setParameters(parameters);
                        mServiceCamera.setPreviewTexture(texture);
                    } catch (IOException exception) {
                        mServiceCamera.release();
                    }

                    mServiceCamera.setPreviewCallbackWithBuffer(CameraService.this);
                    Camera.Size s = mServiceCamera.getParameters().getPreviewSize();
                    mServiceCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);

                    textureView.setAspectRatio(s.height, s.width);
                    textureView.setVisibility(View.GONE);

                    mServiceCamera.startPreview();
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
            };

    private final TextureView.SurfaceTextureListener backSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {

                    try {
                        System.out.println("buraya girebiliyor mu");
                        Camera.Parameters parameters = mBackServiceCamera.getParameters();
                        List<String> focusModes = parameters.getSupportedFocusModes();
                        if (focusModes != null
                                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
                        Size[] sizes = new Size[cameraSizes.size()];
                        int i = 0;
                        for (Camera.Size size : cameraSizes) {
                            sizes[i++] = new Size(size.width, size.height);
                        }
                        Size previewSize =
                                CameraService.chooseOptimalSize(
                                        sizes, 1, 1);
                        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
                        mBackServiceCamera.setDisplayOrientation(90);
                        mBackServiceCamera.setParameters(parameters);
                        mBackServiceCamera.setPreviewTexture(texture);
                    } catch (IOException exception) {
                        mBackServiceCamera.release();
                    }

                    mBackServiceCamera.setPreviewCallbackWithBuffer(CameraService.this);
                    Camera.Size s = mBackServiceCamera.getParameters().getPreviewSize();
                    mBackServiceCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);


                    textureView2.setAspectRatio(s.height, s.width);

                    textureView2.setVisibility(View.GONE);

                    mBackServiceCamera.startPreview();
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
            };
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }


        if (exactSizeFound) {
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            return chosenSize;
        } else {
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    @Override
    public void onCreate() {
        System.out.println("niyeeee");
        System.out.println(Camera.getNumberOfCameras());
        recentPics= new LinkedList<byte[]>();
        mServiceCamera = getCameraInstance(0);
        mBackServiceCamera = getCameraInstance(1);
        System.out.println("CAMERA " + mBackServiceCamera.toString());

        previewWidth = mServiceCamera.getParameters().getPreviewSize().width;
        previewHeight = mServiceCamera.getParameters().getPreviewSize().height;
        System.out.println("WIDTH: " + previewWidth + " HEIGHT: " + previewHeight);
        tex = LayoutInflater.from(this).inflate(R.layout.texture, null);
        tex2 = LayoutInflater.from(this).inflate(R.layout.texture2, null);
        textureView =  tex.findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        textureView2 =  tex2.findViewById(R.id.texture2);
        textureView2.setSurfaceTextureListener(backSurfaceTextureListener);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mWindowManager.addView(tex2, params);
        //textureView2.setVisibility(View.GONE);
        mWindowManager.addView(tex, params);

        super.onCreate();
    }


    public static int getPreviewWidth(){
        return previewWidth;
    }

    public static int getPreviewHeight(){
        return previewHeight;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startBackgroundThread();
//        startTakingPicture();
        if (textureView.isAvailable()) {
            mServiceCamera.startPreview();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
        if (textureView2.isAvailable()) {
            System.out.println("onstart");
            mBackServiceCamera.startPreview();
        } else {
            textureView2.setSurfaceTextureListener(backSurfaceTextureListener);
        }


        return START_STICKY;
    }

    void startTakingPicture(){
        this.mServiceCamera.takePicture(null, null, this.mPicture);

        //System.out.println("START TAKING PICTURE");
        /*
        if(safeToTakePicture) {
            this.mServiceCamera.takePicture(null, null, this.mPicture);
            safeToTakePicture = false;
            System.out.println("PICTURE TAKEN");
        }

         */
        //this.mBackServiceCamera.takePicture(null, null, this.mPictureBack);
    }

    @Override
    public void onDestroy() {
        stopCameraPreview(mServiceCamera,0);
        stopCamera(0);
        stopCameraPreview(mServiceCamera,1);
        stopCamera(1);
        stopBackgroundThread();
        //stopCameraPreview(mBackServiceCamera,1);
        mWindowManager.removeView(tex);
        mWindowManager2.removeView(tex2);
        super.onDestroy();
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
        } catch (final InterruptedException e) {
        }
    }

    protected void stopCamera(int id) {
        if (mServiceCamera != null && id == 0) {
            mServiceCamera.stopPreview();
            mServiceCamera.setPreviewCallback(null);
            mServiceCamera.release();
            mServiceCamera = null;
        }
        if(mBackServiceCamera != null && id == 1)
        {
            mBackServiceCamera.stopPreview();
            mBackServiceCamera.setPreviewCallback(null);
            mBackServiceCamera.release();
            mBackServiceCamera = null;
        }
    }


    @SuppressLint({"NewApi"})
    private Camera getCameraInstance(int mCamId) {
        Camera camera = null;
        try {
            camera = Camera.open(mCamId);
        } catch (Exception e) {
            System.out.println(mCamId);
        }
        return camera;
    }

    private void stopCameraPreview(Camera mCamera, int id) {
        if (id == 0 &&mCamera != null ) {
            mServiceCamera.stopPreview();
            this.mServiceCamera.release();
            this.mServiceCamera = null;
            this.mServiceCamera = getCameraInstance(0);
            //this.mSurfaceView = new CameraPreviews(this, this.mServiceCamera);
            //this.mSurfaceView.refreshDrawableState();



        }
        if (id == 1 && mCamera != null ) {
            this.mBackServiceCamera.release();
            this.mBackServiceCamera = null;
            this.mBackServiceCamera = getCameraInstance(1);
        }

    }

    private static File getOutputMediaFile(int count) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "MyCameraApp");
        if(!mediaStorageDir.exists())
        {
            if(mediaStorageDir.mkdir()) {
                File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+count + ".jpg");
                Log.d("Image Path", "Path is : " + mediaFile.getAbsolutePath());
                return mediaFile;
            }
            Log.d("MyCameraApp", "failed to create directory");
            return null;
        }
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+count + ".jpg");
        Log.d("Image Path", "Path is : " + mediaFile.getAbsolutePath());
        return mediaFile;

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
