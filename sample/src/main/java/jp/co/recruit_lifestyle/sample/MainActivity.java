package jp.co.recruit_lifestyle.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.simon.cameraapp.CameraPreviews;
import com.example.simon.cameraapp.CameraService;
import com.example.simon.cameraapp.FrontCameraPreviews;

import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tracking.DetectorService;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.fragment.FloatingViewControlFragment;
import jp.co.recruit_lifestyle.sample.service.FloatingViewService;

import static android.hardware.Camera.getNumberOfCameras;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static boolean closeAppStopDetection;

    FloatingViewControlFragment fragment;
    public static Runnable UIrunnable ;
    String[] ImagePath;
    private ImageView btnChangeCam;
    private ImageView btnSave;
    private ImageView btnTakePic;
    private int currentCameraId;
    private boolean isFirst;
    public static Camera mCamera;
    public static Camera frontCamera;
    public static CameraPreviews mCameraPreview;
    public static FrontCameraPreviews frontCameraPreview;
    Camera.PictureCallback mPicture;
    Camera.PictureCallback mPictureBack;
    public static LinearLayout mllFirst;
    private LinearLayout mllSecond;
    private boolean isPlay = false;
    public static final String PREFS = "CAMERA_APP";
    protected SharedPreferences prefs;
    int count = 0;
    public static String TAG = "DualCamActivity";
    private static Handler handler;
    private HandlerThread handlerThread;
    private boolean isChecked = false;
    boolean mBounded;
    boolean detectorBounded;
    public static Thread detectorServiceThread;
    FloatingViewService mServer;
    DetectorService detectorServer;
   public static Handler floatingHandler =new Handler();
    public static boolean created = false;
    public static Thread cameraThread;


    ServiceConnection mConnection = new ServiceConnection() {
        @SuppressLint("WrongConstant")
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Toast.makeText(DetectorActivity.this, "Service is disconnected", 1000).show();
            System.out.println("DISCONNECTED");
            mBounded = false;
            detectorBounded=false;
            mServer = null;
            detectorServer=null;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @SuppressLint("WrongConstant")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //System.out.println(name.getClass().getName()+" otek"+FloatingViewService.LocalBinder.class.getName());
            if(service.getClass().getName().equals(FloatingViewService.LocalBinder.class.getName()))
                onServiceConnected1( name, (FloatingViewService.LocalBinder) service);
            else if(service.getClass().getName().equals(DetectorService.LocalBinder.class.getName()))
                onServiceConnected2( name, (DetectorService.LocalBinder) service);
            //Toast.makeText(DetectorActivity.this, "Service is connected", 1000).show();
            System.out.println("General service override");
        }
        public void onServiceConnected1(ComponentName name, FloatingViewService.LocalBinder service) {
            System.out.println("CONNECTED");
            mBounded = true;
            FloatingViewService.LocalBinder mLocalBinder = service;
            mServer = mLocalBinder.getServerInstance();
        }
        public void onServiceConnected2(ComponentName name, DetectorService.LocalBinder service) {
            System.out.println("CONNECTED");
            detectorBounded = true;
            DetectorService.LocalBinder mLocalBinder =service;
            detectorServer = mLocalBinder.getServerInstance();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // TO BE CHANGED


        if(!created) {
            UIrunnable = new Runnable() {
                @Override
                public void run() {
                    // create default notification channel
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        final String channelId = getString(R.string.default_floatingview_channel_id);
                        final String channelName = getString(R.string.default_floatingview_channel_name);
                        final NotificationChannel defaultChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN);
                        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.createNotificationChannel(defaultChannel);
                        }
                    }

                    if (savedInstanceState == null) {
                        System.out.println("FLOATING VIEW CREATED AGAIN");
                        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        fragment = FloatingViewControlFragment.newInstance();
                        ft.add(R.id.container, fragment);
                        ft.commit();
                    }
                }
            };
            runOnUiThread(UIrunnable);
            created = true;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(!mBounded) {
            Intent mIntent = new Intent(this, FloatingViewService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            mBounded = true;
        }

        /*if(!detectorBounded) {
            Intent mIntent = new Intent(this, DetectorService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            detectorBounded = true;
        }*/

        Switch feature1 = (Switch)findViewById(R.id.switch1);
        Switch feature2  = (Switch) findViewById(R.id.switch2);
        Switch feature3 = (Switch) findViewById(R.id.switch3);
        Button submit = (Button) findViewById(R.id.getBtn);
        submit.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                String str1, str2;
                if (feature1.isChecked()) {
                    //str1 = feature1.getTextOn().toString();
                    if(!mServer.show) {
                        mServer.showSpeedLimit();
                        Intent mIntent = new Intent(MainActivity.this, DetectorService.class);
                       /*
                        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                        MainActivity.this.startService(mIntent);
                        */
                         detectorServiceThread = new Thread(){
                            public void run(){
                                bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                                MainActivity.this.startService(mIntent);
                             }
                        };
                        detectorServiceThread.start();
                    }
                }
                else {
                    mServer.destroy();
                    unbindService(mConnection);
                    detectorServiceThread.interrupt();
                    try {
                        detectorServiceThread.join();
                        detectorServiceThread = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (feature2.isChecked())
                    str2 = feature3.getTextOn().toString();
                else
                    str2 = feature3.getTextOff().toString();
                //Toast.makeText(getApplicationContext(), "Switch1 -  " + str1 + " \n" + "Switch2 - " + str2,Toast.LENGTH_SHORT).show();
            }
        });

        if (hasPermission()) {
            System.out.println("NUMBER OF CAMERAS: " + getNumberOfCameras());
            Intent intent = new Intent(MainActivity.this, CameraService.class);

            cameraThread = new Thread(){
                public void run(){
                    bindService(intent, mConnection, BIND_AUTO_CREATE);
                    MainActivity.this.startService(intent);
                }
            };
            cameraThread.start();

        } else {
            requestPermission();
        }
    }

    @SuppressLint({"NewApi"})
    private Camera getCameraInstance(int mCamId) {
        Camera camera = null;
        try {
            camera = Camera.open(mCamId);
        } catch (Exception e) {
            System.out.println("CAMERA NOT FOUND " + mCamId);
        }
        return camera;
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();
/*
        if(!mBounded) {
            Intent mIntent = new Intent(this, FloatingViewService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            mBounded = true;
        }
        if(!detectorBounded) {
            Intent mIntent = new Intent(this, DetectorService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            detectorBounded = true;
        }*/
        /*
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
         */
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

        /*
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

         */
        //unbindService(mConnection);
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        LOGGER.d("onDestroy " + this);
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    public static synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                System.out.println("NUMBER OF CAMERAS: " + getNumberOfCameras());
                Intent intent = new Intent(MainActivity.this, CameraService.class);
                MainActivity.this.startService(intent);
            /*  Intent intent2 = new Intent(MainActivity.this, DetectorService.class);
                MainActivity.this.startService(intent2);*/
            } else {
                requestPermission();
            }
        }
    }
//TODO: SHOW DUZENLEMESİ
    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        MainActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

}
