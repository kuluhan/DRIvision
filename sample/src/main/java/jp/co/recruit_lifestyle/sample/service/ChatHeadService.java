package jp.co.recruit_lifestyle.sample.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

public class ChatHeadService extends Service implements FloatingViewListener {

    private static final String TAG = "ChatHeadService";

    public static final String EXTRA_CUTOUT_SAFE_AREA = "cutout_safe_area";

    private static final int NOTIFICATION_ID = 9083150;

    private FloatingViewManager mFloatingViewManager;
    private ImageView iconView;
    private LayoutInflater inflater;
    private FloatingViewManager.Options options;
    private DisplayMetrics metrics;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFloatingViewManager != null) {
            return START_STICKY;
        }

        //final DisplayMetrics metrics = new DisplayMetrics();
        metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        //final LayoutInflater inflater = LayoutInflater.from(this);
        //final ImageView iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        inflater = LayoutInflater.from(this);
        iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, getString(R.string.chathead_click_message));
            }
        });

        mFloatingViewManager = new FloatingViewManager(this, this);
        mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_trash_fixed);
        mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_trash_action);
        mFloatingViewManager.setSafeInsetRect((Rect) intent.getParcelableExtra(EXTRA_CUTOUT_SAFE_AREA));
        //final FloatingViewManager.Options options = new FloatingViewManager.Options();
        options = new FloatingViewManager.Options();
        options.overMargin = (int) (16 * metrics.density);
        mFloatingViewManager.addViewToWindow(iconView, options);

        startForeground(NOTIFICATION_ID, createNotification(this));

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onFinishFloatingView() {
        stopSelf();
        Log.d(TAG, getString(R.string.finish_deleted));
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {
        if (isFinishing) {
            Log.d(TAG, getString(R.string.deleted_soon));
        } else {
            Log.d(TAG, getString(R.string.touch_finished_position, x, y));
        }
    }

    private void destroy() {
        if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }
    }

    private static Notification createNotification(Context context) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.default_floatingview_channel_id));
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(context.getString(R.string.chathead_content_title));
        builder.setContentText(context.getString(R.string.content_text));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);


        return builder.build();
    }

    public void changeSpeedSign(int speedLimit){
        switch(speedLimit) {
            case 1:
                iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
                break;
            case 2:
                iconView = (ImageView) inflater.inflate(R.layout.speed_30, null, false);
                break;
            case 3:
                iconView = (ImageView) inflater.inflate(R.layout.speed_50, null, false);
                break;
            case 4:
                iconView = (ImageView) inflater.inflate(R.layout.speed_60, null, false);
                break;
            case 5:
                iconView = (ImageView) inflater.inflate(R.layout.speed_70, null, false);
                break;
            case 6:
                iconView = (ImageView) inflater.inflate(R.layout.speed_80, null, false);
                break;
            case 7:
                iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
                break;
            case 8:
                iconView = (ImageView) inflater.inflate(R.layout.speed_100, null, false);
                break;
            case 9:
                iconView = (ImageView) inflater.inflate(R.layout.speed_120, null, false);
                break;
            default:
                iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        }
        options = new FloatingViewManager.Options();
        options.overMargin = (int) (16 * metrics.density);
        mFloatingViewManager.addViewToWindow(iconView, options);
    }
}
