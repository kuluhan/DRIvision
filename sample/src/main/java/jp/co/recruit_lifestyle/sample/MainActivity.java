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

import com.example.simon.cameraapp.CalibrateService;
import com.example.simon.cameraapp.CameraPreviews;
import com.example.simon.cameraapp.CameraService;
import com.example.simon.cameraapp.FaceService;
import com.example.simon.cameraapp.FrontCameraPreviews;
import com.example.simon.cameraapp.LaneService;
import com.example.simon.cameraapp.VehicleService;

import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tracking.DetectorService;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.fragment.FloatingViewControlFragment;
import jp.co.recruit_lifestyle.sample.service.FloatingViewService;

import static android.hardware.Camera.getNumberOfCameras;
import static com.example.simon.cameraapp.VehicleService.vehicleThread;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static boolean closeAppStopDetection;
    VehicleService vehicleServer;
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
    boolean calibrateBounded;
    boolean detectorBounded;
    boolean vehicleBounded;
    boolean laneBounded;
    boolean faceBounded;
    public static Thread detectorServiceThread;
    FloatingViewService mServer;
    DetectorService detectorServer;
    LaneService laneServer;
    FaceService faceServer;
    CalibrateService calibrateServer;
   public static Handler floatingHandler =new Handler();
    public static boolean created = false;

    ServiceConnection mConnection = new ServiceConnection() {
        @SuppressLint("WrongConstant")
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", 1000).show();
            System.out.println("DISCONNECTED");
            mBounded = false;
            detectorBounded=false;
            vehicleBounded=false;
            laneBounded= false;
            faceBounded =false;
            mServer = null;
            vehicleServer =null;
            detectorServer=null;
            faceServer =null;
            laneServer=null;
            calibrateServer=null;
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
            else if(service.getClass().getName().equals(VehicleService.LocalBinder.class.getName()))
                onServiceConnected3( name, (VehicleService.LocalBinder) service);
            else if(service.getClass().getName().equals(FaceService.LocalBinder.class.getName()))
                onServiceConnected4( name, (FaceService.LocalBinder) service);
            else if(service.getClass().getName().equals(LaneService.LocalBinder.class.getName()))
                onServiceConnected5( name, (LaneService.LocalBinder) service);
            else if(service.getClass().getName().equals(CalibrateService.LocalBinder.class.getName()))
                onServiceConnected6( name, (CalibrateService.LocalBinder) service);
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
        public void onServiceConnected3(ComponentName name, VehicleService.LocalBinder service) {
            System.out.println("CONNECTED");
            vehicleBounded = true;
            VehicleService.LocalBinder mLocalBinder = service;
            vehicleServer = mLocalBinder.getServerInstance();
        }
        public void onServiceConnected4 (ComponentName name, FaceService.LocalBinder service) {
            System.out.println("CONNECTED");
            faceBounded = true;
            FaceService.LocalBinder mLocalBinder =service;
            faceServer = mLocalBinder.getServerInstance();
        }
        public void onServiceConnected5 (ComponentName name, LaneService.LocalBinder service) {
            System.out.println("CONNECTED");
            laneBounded = true;
            LaneService.LocalBinder mLocalBinder =service;
            laneServer = mLocalBinder.getServerInstance();
        }
        public void onServiceConnected6(ComponentName name, CalibrateService.LocalBinder service) {
            System.out.println("CONNECTED");
            calibrateBounded = true;
            CalibrateService.LocalBinder mLocalBinder = service;
            calibrateServer = mLocalBinder.getServerInstance();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // TO BE CHANGED
        closeAppStopDetection=false;

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
            MainActivity.this.startService(mIntent);
            mBounded = true;
        }

        /*if(!detectorBounded) {
            Intent mIntent = new Intent(this, DetectorService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            detectorBounded = true;
        }*/

        Button calibrate = (Button)findViewById(R.id.calibrate);
        calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(calibrateBounded){
                    Toast.makeText(
                            MainActivity.this,
                            "please look forward, your picture will be taken in 3 seconds.",
                            Toast.LENGTH_LONG)
                            .show();
                    calibrateServer.calibrate();
                }
            }
        });

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
                    if(!FloatingViewService.show) {
                         mServer.showSpeedLimit();
                         Intent mIntent = new Intent(MainActivity.this, DetectorService.class);
                         bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                         MainActivity.this.startService(mIntent);
                    }
                }
                else {
                    if(DetectorService.started)
                    {
                        mServer.destroy();
                        DetectorService.started=false;
                        // unbindService(mConnection);
                        detectorServiceThread.interrupt();
                        try {
                            detectorServiceThread.join();
                            detectorServiceThread = null;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            System.out.println("InterruptedException while Sign Detection join ");
                        }
                        System.out.println("Sign Detection stopped");
                    }
                }
                if (feature2.isChecked()){
                  /* Intent mIntent = new Intent(MainActivity.this, VehicleService.class);
                    bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                    MainActivity.this.startService(mIntent);*/
                }
                else{
                    //kapat

                }
                if (feature3.isChecked()){

                    Intent mIntent = new Intent(MainActivity.this, VehicleService.class);
                    bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                    MainActivity.this.startService(mIntent);
                }
                else{
                    if(VehicleService.started)
                    {
                        VehicleService.started=false;
                        // unbindService(mConnection);
                        vehicleThread.interrupt();
                        try {
                            vehicleThread.join();
                            vehicleThread = null;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            System.out.println("InterruptedException while Vehicle Detection join ");
                        }
                        System.out.println("Vehicle Detection stopped");
                    }
                }
            }
        });

        if (hasPermission()) {
            System.out.println("Main Activitythread id:"+Thread.currentThread().getId()+" "+ Thread.currentThread().getName());
            System.out.println("NUMBER OF CAMERAS: " + getNumberOfCameras());
            Intent intent = new Intent(MainActivity.this, CameraService.class);
            bindService(intent, mConnection, BIND_AUTO_CREATE);
            MainActivity.this.startService(intent);

            Intent intent2 = new Intent(MainActivity.this, CalibrateService.class);
            bindService(intent2, mConnection, BIND_AUTO_CREATE);
            MainActivity.this.startService(intent2);

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
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);
/* //DONT OPEN IT CAUSES TO STOP WHEN IT is Paused
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }*/

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
