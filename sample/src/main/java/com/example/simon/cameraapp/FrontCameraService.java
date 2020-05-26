package com.example.simon.cameraapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.RequiresApi;
import jp.co.recruit.floatingview.R;

/// bunlar değişecekkk


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class FrontCameraService extends Service implements Camera.PreviewCallback {
    public static ReentrantReadWriteLock lck =new ReentrantReadWriteLock();
    public static Lock writeLock =lck.writeLock();
    public static CopyOnWriteArrayList<Bitmap> recentPics;
    public static int previewWidth = 0;
    public static int previewHeight = 0;
    public static Runnable imageConverter;
    private static Matrix frameToCropTransform;
    private static Bitmap croppedBitmap = null;
    public static boolean isProcessingFrame = false;
    public static byte[][] yuvBytes = new byte[3][];
    public static int[] rgbBytes = null;
    public static int yRowStride;
    public static Handler imagesaverHandler=   (new Handler());
    public static Lock readLock = lck.readLock();
    public static ReentrantReadWriteLock lck2 =new ReentrantReadWriteLock();
    public static Lock writeLock2 =lck2.writeLock();
    public static Lock readLock2 = lck2.readLock();
    String[] ImagePath;
    List picList;
    Camera.PictureCallback mPicture;
    Camera.PictureCallback mPictureBack;
    public static Bitmap rgbFrameBitmap = null;
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
    boolean noFrontCamera = false;
    public static int counterForFrame;
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private AutoFitTextureView textureView2;
    private WindowManager mWindowManager;
    public static View tex2;
    private HandlerThread backgroundThread;
    public static Runnable imageSaver;
    public static final Object lockk = new Object();
    public static final int SIZEOFRECENTPICS=100;

    // public static  final MonitorObject myMonitorObject =new MonitorObject();
    public  IBinder mBinder = new LocalBinder();
    //public static boolean stopOrderCameIn;
    // Class used for the client Binder.
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public FrontCameraService getServerInstance() {
            return FrontCameraService.this;
        }
    }


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
        //  myMonitorObject = new MonitorObject();
        //recentPics.add(data);
        counterForFrame=0;
        isProcessingFrame = true;
        //yuvBytes[0] = data;
        imageSaver= new Runnable() {
            @Override
            public void run() {
                writeLock.lock();
                try {
                    // access the resource protected by this lock
                    camera.addCallbackBuffer(data);
                    imageConverter.run();
                    recentPics.add(rgbFrameBitmap);
                    while (recentPics.size() > SIZEOFRECENTPICS)
                        recentPics.remove(0);
                    //  System.out.println("AddingNEWDATA" + recentPics.size());
                }finally {
                    writeLock.unlock();
                }
                readyForNextImage2();
            }
        };
        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        yuvBytes[0] = data;
                        DetectorService.yRowStride = previewWidth;
                        ImageUtils.convertYUV420SPToARGB8888(data, previewWidth, previewHeight, rgbBytes);
                        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
                        int sensorOrientation = 90 - getScreenOrientation();
                        frameToCropTransform  =
                                ImageUtils.getTransformationMatrix(
                                        previewWidth, previewHeight,
                                        previewWidth, previewHeight,
                                        sensorOrientation, false);
                        Bitmap newBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                        final Canvas canvas = new Canvas(newBitmap);
                        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
                        rgbFrameBitmap = newBitmap.copy(newBitmap.getConfig(),false);
                    }
                };

        readyForNextImage2();
    }
    public static void readyForNextImage2() {

        if( (imageSaver != null) &&(imagesaverHandler!=null)){
            synchronized (lockk) {
                lockk.notify(); // Will wake up lock.wait()
            }
            imagesaverHandler = new Handler();
            imagesaverHandler.postDelayed(imageSaver, 100);
        }

    }


    protected int getScreenOrientation() {

        WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int currentRatation = windowService.getDefaultDisplay().getRotation();

        if (Surface.ROTATION_0 == currentRatation) {
            currentRatation = 0;
        } else if(Surface.ROTATION_180 == currentRatation) {
            currentRatation = 180;
        } else if(Surface.ROTATION_90 == currentRatation) {
            currentRatation = 90;
        } else if(Surface.ROTATION_270 == currentRatation) {
            currentRatation = 270;
        }

        return currentRatation;
    }
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

                    mBackServiceCamera.setPreviewCallbackWithBuffer(FrontCameraService.this);
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
        recentPics= new CopyOnWriteArrayList<Bitmap>();
        tex2 = LayoutInflater.from(this).inflate(R.layout.texture2, null);
        textureView2 =  tex2.findViewById(R.id.texture2);
        textureView2.setSurfaceTextureListener(backSurfaceTextureListener);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //textureView2.setVisibility(View.GONE);

        picList =Collections.synchronizedList (recentPics);

        int  cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        if(cameraCount>1)
        {
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        mBackServiceCamera = getCameraInstance(camIdx);
                        Toast.makeText(getApplicationContext(), "We can use your two Camera!", Toast.LENGTH_LONG).show();
                    } catch (RuntimeException e) {
                        Log.e(TAG, " Front Camera failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
            if(mBackServiceCamera== null)
            {
                noFrontCamera = true;
                Toast.makeText(getApplicationContext(), "No Usable Front Camera =(", Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            noFrontCamera = true;
            Toast.makeText(getApplicationContext(), "No Usable Front Camera =(", Toast.LENGTH_LONG).show();
        }
        if(noFrontCamera == false)
        {
            mWindowManager.addView(tex2, params);
        }

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

        return START_STICKY;
    }



    @Override
    public void onDestroy() {

        if(noFrontCamera == false)
        {
            stopCameraPreview(mServiceCamera);
            stopCamera();
        }
        stopBackgroundThread();

        if(noFrontCamera == false)
        {
            mWindowManager.removeView(tex2);
        }
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
        } catch (final InterruptedException e) {
        }
    }

    protected void stopCamera() {
        if(mBackServiceCamera != null )
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
        }
        return camera;
    }

    private void stopCameraPreview(Camera mCamera) {
        if (mCamera != null ) {
            this.mBackServiceCamera.release();
            this.mBackServiceCamera = null;
            this.mBackServiceCamera = getCameraInstance(1);
            this.mBackSurfaceView = new FrontCameraPreviews(this, this.mBackServiceCamera);
            this.mBackSurfaceView.refreshDrawableState();
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


}
