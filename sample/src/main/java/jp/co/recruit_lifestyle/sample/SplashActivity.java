package jp.co.recruit_lifestyle.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import jp.co.recruit.floatingview.R;

public class SplashActivity extends AppCompatActivity {

    // Seconds to wait until application starts
    private final int SECONDS_TO_WAIT = 4;

    /**
     * Activity for the opening screen of the application
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Set layout
        setContentView(R.layout.activity_splash);

        // Waiting thread
        Thread timedSplash = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(SECONDS_TO_WAIT * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Intent redirect = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(redirect);
                }
            }
        };
        timedSplash.start();
    }
}
