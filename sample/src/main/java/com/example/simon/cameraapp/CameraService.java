package com.example.simon.cameraapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.MainActivity;

import static org.tensorflow.lite.examples.detection.tracking.DetectorService.detectTrafficSign;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.started;


public class CameraService extends Service{
    String[] ImagePath;

    Camera.PictureCallback mPicture;
    Camera.PictureCallback mPictureBack;
    public SurfaceView mSurfaceView;
    public static Camera mServiceCamera;
    private SurfaceView mBackSurfaceView;
    private static Camera mBackServiceCamera;
    private boolean isPlay = false;
    public static final String PREFS = "CAMERA_APP";
    protected SharedPreferences prefs;
    int count = 0;
    public static boolean safeToTakePicture = false;

    public static String TAG = "DualCamActivity";
    public static int previewWidth;
    public static int previewHeight;

    private LinearLayout mllFirst;
    public static CameraPreviews mCameraPreview;

    class takePicture implements Camera.PictureCallback {
        int id;
        takePicture(int id) {
            this.id = id;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onPictureTaken(byte[] data, Camera camera) {
            CameraService.this.count++;
            Log.d("Image Path", "Path is : ");
            File pictureFile = CameraService.getOutputMediaFile(count);
           /* MainActivity.this.mCamera = getCameraInstance(MainActivity.this.currentCameraId);
            MainActivity.this.mCameraPreview = new CameraPreviews(MainActivity.this, MainActivity.this.mCamera);
            if (MainActivity.this.isFirst) {
                MainActivity.this.mllFirst.removeAllViews();
                MainActivity.this.mllFirst.addView(MainActivity.this.mCameraPreview);
                return;
            }
            MainActivity.this.mllSecond.removeAllViews();
            MainActivity.this.mllSecond.addView(MainActivity.this.mCameraPreview);*/
            if (pictureFile != null) {

                try {
                    Log.d("Image Path", "Path is : " + pictureFile.getAbsolutePath());
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    CameraService.this.ImagePath[0] = pictureFile.getAbsolutePath();
                    Bitmap bmp = BitmapFactory.decodeByteArray(data,0, data.length);
                    /*
                    ImageView image = new ImageView(this);
                    image.setImageBitmap(bmp);

                     */
                    if(started)
                        detectTrafficSign(bmp);

                    CameraService.this.stopCameraPreview(camera, id);
                } catch (FileNotFoundException e) {
                } catch (IOException e2) {
                }

            }
            safeToTakePicture = true;
        }
    }

    @Override
    public void onCreate() {
        System.out.println("niyeeee");
        this.mllFirst = MainActivity.mllFirst;
        System.out.println("mllFirst: " + mllFirst);
        //this.frontCamera = getCameraInstance(1);
        mServiceCamera = getCameraInstance(0);
        System.out.println("CAMERA " + mServiceCamera.toString());
        /*
        SurfaceTexture surfaceTexture = new SurfaceTexture(10);
        try {
            mServiceCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

         */
        mSurfaceView = new CameraPreviews(this, mServiceCamera);
        System.out.println("SURFACE PREVIEW: " + mSurfaceView);

        //this.frontCameraPreview = new FrontCameraPreviews(this, this.frontCamera);
        LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        @SuppressLint("ResourceType")
        LinearLayout l = (LinearLayout) li.inflate(R.id.backcamera_preview, null);
        l.addView(mSurfaceView);
        System.out.println("CHECKPOINT");
        //MainActivity.mllFirst.addView(mSurfaceView);

        //mBackServiceCamera = MainActivity.frontCamera;
        //mBackSurfaceView = MainActivity.frontCameraPreview;
        this.ImagePath = new String[2];
        this.ImagePath[0] = "null";
        this.ImagePath[1] = "null";
        this.mPicture = new takePicture(0);
        //this.mPictureBack = new takePicture(1);

        previewWidth = mServiceCamera.getParameters().getPreviewSize().width;
        previewHeight = mServiceCamera.getParameters().getPreviewSize().height;
        System.out.println("WIDTH: " + previewWidth + " HEIGHT: " + previewHeight);

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

        startTakingPicture();
        return START_STICKY;
    }

    void startTakingPicture(){
        this.mServiceCamera.takePicture(null, null, this.mPicture);
        //System.out.println("START TAKING PICTURE");
        if(safeToTakePicture) {
            this.mServiceCamera.takePicture(null, null, this.mPicture);
            safeToTakePicture = false;
            System.out.println("PICTURE TAKEN");
        }
        //this.mBackServiceCamera.takePicture(null, null, this.mPictureBack);
    }

    @Override
    public void onDestroy() {
        stopCameraPreview(mServiceCamera,0);
        //stopCameraPreview(mBackServiceCamera,1);
        super.onDestroy();
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

    private void stopCameraPreview(Camera mCamera, int id) {
        if (id == 0 &&mCamera != null ) {
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


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
