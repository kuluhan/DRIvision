package jp.co.recruit_lifestyle.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
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

    FloatingViewControlFragment fragment;

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
    FloatingViewService mServer;
    public static boolean created = false;

    ServiceConnection mConnection = new ServiceConnection() {
        @SuppressLint("WrongConstant")
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Toast.makeText(DetectorActivity.this, "Service is disconnected", 1000).show();
            System.out.println("DISCONNECTED");
            mBounded = false;
            mServer = null;
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Toast.makeText(DetectorActivity.this, "Service is connected", 1000).show();
            System.out.println("CONNECTED");
            mBounded = true;
            FloatingViewService.LocalBinder mLocalBinder = (FloatingViewService.LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // TO BE CHANGED


        if(!created) {
            runOnUiThread(new Runnable() {
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
            });
            created = true;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(!mBounded) {
            Intent mIntent = new Intent(this, FloatingViewService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            mBounded = true;
        }

        Switch feature1 = (Switch)findViewById(R.id.switch1);
        Switch feature2  = (Switch) findViewById(R.id.switch2);
        Switch feature3 = (Switch) findViewById(R.id.switch3);
        Button submit = (Button) findViewById(R.id.getBtn);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str1, str2;
                if (feature1.isChecked()) {
                    //str1 = feature1.getTextOn().toString();
                    if(!mServer.show) {
                        mServer.showSpeedLimit();
                    }
                }
                else {
                    mServer.destroy();
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
            MainActivity.this.startService(intent);
            Intent intent2 = new Intent(MainActivity.this, DetectorService.class);
            MainActivity.this.startService(intent2);

        } else {
            requestPermission();
        }


        /*
        this.mllFirst = (LinearLayout) findViewById(R.id.backcamera_preview);
        this.mCamera = getCameraInstance(0);

        //this.frontCamera = getCameraInstance(1);
        this.mCameraPreview = new CameraPreviews(this, this.mCamera);
        //this.frontCameraPreview = new FrontCameraPreviews(this, this.frontCamera);
        this.mllFirst.addView(this.mCameraPreview);
        //this.mllSecond.addView(this.frontCameraPreview);

         */




        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

         /*
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        */


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

        if(!mBounded) {
            Intent mIntent = new Intent(this, FloatingViewService.class);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            mBounded = true;
        }
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
        LOGGER.d("onDestroy " + this);
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onDestroy();
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

                Intent intent2 = new Intent(MainActivity.this, DetectorService.class);
                MainActivity.this.startService(intent2);
            } else {
                requestPermission();
            }
        }
    }

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
