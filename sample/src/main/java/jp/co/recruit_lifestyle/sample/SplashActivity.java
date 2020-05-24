package jp.co.recruit_lifestyle.sample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import jp.co.recruit.floatingview.R;

public class SplashActivity extends AppCompatActivity {

    // Seconds to wait until application starts
    private final int SECONDS_TO_WAIT = 2;
    private final int CAMERA_PERMISSION_CODE = 1;
    private final int FLOATING_VIEW_CODE = 2;
    private final int EXTERNAL_STORAGE_CODE = 3;
    private final int INTERNET_ACCESS_CODE = 4;
    int notGranted = 0;
    int responseReceived = 0;
    boolean runtimeNotif = (Build.VERSION.SDK_INT >= 23);
    private final String[] PERMISSIONS_NEEDED = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };
    private final int PERMISSION_CODE = 1;


    /**
     * Activity for the opening screen of the application
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Hide Navigation and Status Bar
        View decorView = getWindow().getDecorView();

        // Set layout
        setContentView(R.layout.activity_splash);

        int uiOptions;
        // Not displaying the status bar is only supported in SDK >= 16
        if (Build.VERSION.SDK_INT >= 16) {
            uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
        } else {
            uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        decorView.setSystemUiVisibility(uiOptions);

        Thread checkPermissions = new Thread() {
            // Request necessary permissions

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                super.run();
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(PERMISSIONS_NEEDED, PERMISSION_CODE);
                } else {
                    try {
                        TimeUnit.SECONDS.sleep(SECONDS_TO_WAIT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        Intent redirect = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(redirect);
                    }
                }
            }
        };
        checkPermissions.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Thread continueApp = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                   TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Intent redirect = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(redirect);
                }
            }
        };
        continueApp.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult: grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        notGranted++;
                    }
                }
            }
            if (notGranted > 0) {
                Toast.makeText(SplashActivity.this, "Necessary permissions not granted, exiting...", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                try {
                    TimeUnit.SECONDS.sleep(SECONDS_TO_WAIT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Intent redirect = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(redirect);
                }
            }
        }
    }
}
