package jp.co.recruit_lifestyle.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.simon.cameraapp.CameraPreviews;
import com.example.simon.cameraapp.CameraService;
import com.example.simon.cameraapp.FrontCameraPreviews;

import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tracking.DetectorService;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.fragment.FloatingViewControlFragment;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // TO BE CHANGED

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
                    final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    fragment = FloatingViewControlFragment.newInstance();
                    ft.add(R.id.container, fragment);
                    ft.commit();
                }
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        System.out.println("NUMBER OF CAMERAS: " + getNumberOfCameras());
        /*
        this.mllFirst = (LinearLayout) findViewById(R.id.backcamera_preview);
        this.mCamera = getCameraInstance(0);

        //this.frontCamera = getCameraInstance(1);
        this.mCameraPreview = new CameraPreviews(this, this.mCamera);
        //this.frontCameraPreview = new FrontCameraPreviews(this, this.frontCamera);
        this.mllFirst.addView(this.mCameraPreview);
        //this.mllSecond.addView(this.frontCameraPreview);

         */

        Intent intent = new Intent(MainActivity.this, CameraService.class);
        MainActivity.this.startService(intent);

        Intent intent2 = new Intent(MainActivity.this, DetectorService.class);
        MainActivity.this.startService(intent2);

        /*
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

}
