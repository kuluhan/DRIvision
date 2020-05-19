package jp.co.recruit_lifestyle.sample.service;
import org.jcodec.api.android.AndroidSequenceEncoder;
import java.sql.Timestamp;
import java.util.Date;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.tensorflow.lite.examples.detection.tracking.DetectorService;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.simon.cameraapp.CameraService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;
import jp.co.recruit_lifestyle.sample.MainActivity;

import static com.example.simon.cameraapp.CameraService.SIZEOFRECENTPICS;
import static com.example.simon.cameraapp.CameraService.counterForFrame;
import static com.example.simon.cameraapp.CameraService.getPreviewHeight;
import static com.example.simon.cameraapp.CameraService.getPreviewWidth;
import static com.example.simon.cameraapp.CameraService.lockk;
import static java.lang.Thread.sleep;


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class FloatingViewService extends Service implements FloatingViewListener {
    private WindowManager mWindowManager;
    public static View mFloatingView;
    boolean created = false;
    final int NUMOFFRAMESINSECTOWRITE=10;
    public static final String EXTRA_CUTOUT_SAFE_AREA = "cutout_safe_area";
    private static Bitmap rgbFrameBitmap;
    private static final int NOTIFICATION_ID = 908114;
    public static byte[][] yuvBytes = new byte[3][];
    private static final String PREF_KEY_LAST_POSITION_X = "last_position_x";
public static boolean startCounter;
    private static final String PREF_KEY_LAST_POSITION_Y = "last_position_y";
    public static int[] rgbBytes= new int[getPreviewWidth()*getPreviewHeight()];

    private static FloatingViewManager mFloatingViewManager;
    private static ImageView trafficSignView;
    private static LayoutInflater inflater;
    private static FloatingViewManager.Options options;
    private static DisplayMetrics metrics;
    public static boolean show;
    public static Semaphore sem;
   // private boolean recordStopp;
    public static SeekableByteChannel out = null;
    AndroidSequenceEncoder encoder;
    // Binder given to clients
    IBinder mBinder = new LocalBinder();
    public static boolean stopOrderCameIn;
    // Class used for the client Binder.
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public FloatingViewService getServerInstance() {
            return FloatingViewService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFloatingViewManager != null) {
            return START_STICKY;
        }
         stopOrderCameIn =false;
        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_view, null);
        sem = new Semaphore(1);
       // recordStopp=false;
        out = null;
        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        show = false;
        createOne();



        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
//The root element of the expanded view layout
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);


//Set the close button
        ImageView closeButtonCollapsed = (ImageView) mFloatingView.findViewById(R.id.close_btn);
        closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //close the service and remove the from from the window
                stopSelf();
            }
        });
        Runnable videosaver= new Runnable() {
            @Override
            public void run() {
                File file = new File(getExternalFilesDir(null), File.separator + "driVideos");
                if (!file.exists()) {
                    file.mkdirs();
                }
                Date date = new Date();
                Timestamp ts = new Timestamp(date.getTime());
                File file2 = new File(file, ts + ".mp4");
                String path = file2.getPath();
                System.out.println(path);
                try {
                    try {
                        out = NIOUtils.writableFileChannel(path);
                        System.out.println("yok yok:" + out);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    // for Android use: AndroidSequenceEncoder
                    encoder = new AndroidSequenceEncoder(out, Rational.R(NUMOFFRAMESINSECTOWRITE, 1));
                    int counter = 0;
                    boolean pointerAQ = false;
                    Iterator<Bitmap> pointer = (DetectorService.recentPics).iterator();
                    //int currentSize = DetectorService.recentPics.size();
                    int getter =counter;
                   // System.out.println("FirstInıt iterator"+counterForFrame+"counter:"+counter);
                    CameraService.readLock2.lock();
                        while ((counterForFrame >= counter)) {
                            CameraService.readLock2.unlock();
                          //  System.out.println("total frame counter: " + counterForFrame + " counter: " + counter + "  StopOrder:" + stopOrderCameIn);
                            //
                            // sem.acquire();
                            CameraService.readLock.lock();
                            try {
                                // Generate the image, for Android use Bitmap
                                //earlier images
                                if(counter<counterForFrame) {
                                    Bitmap bitmapData = DetectorService.recentPics.get(getter);
                                    encoder.encodeImage(bitmapData);
                                    if (counter < SIZEOFRECENTPICS - 1)
                                        getter++;
                                    counter++;
                                    System.out.println("Resim islendi" + counter);
                                }
                                else {
                                    if(!stopOrderCameIn)
                                   {synchronized (lockk) {
                                        lockk.wait();
                                    }
                                   }else
                                       break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                System.out.println("total frame counter: " + counterForFrame + " counter: " + counter + "  StopOrder:" + stopOrderCameIn);
                              // sem.release();
                                CameraService.readLock.unlock();
                            }
                            CameraService.readLock2.lock();
                        }
                    System.out.println("wHİLEDAN CKT");
                    endVideoRecording();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        };

//Set the view while floating view is expanded.
//Set the play button.
        /*
        ImageView playButton = (ImageView) mFloatingView.findViewById(R.id.play_btn);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FloatingViewService.this, "Playing the song.", Toast.LENGTH_LONG).show();

            }
        });

         */


        /*
//Set the next button.
        ImageView nextButton = (ImageView) mFloatingView.findViewById(R.id.next_btn);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destroy();
            }
        });

  */



//Set the close button
        ImageView closeButton = (ImageView) mFloatingView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
            }
        });


//Open the application on thi button click
        ImageView openButton = (ImageView) mFloatingView.findViewById(R.id.open_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open the application  click.
                Intent intent = new Intent(FloatingViewService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        });
         ImageView recordStop = (ImageView) mFloatingView.findViewById(R.id.recordStop);
        recordStop.setVisibility(View.INVISIBLE);
        //Recording last 30 secs and rest.
        ImageView recordButton = (ImageView) mFloatingView.findViewById(R.id.record);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCounter=true;

                try {
                    sleep(10);
                    Thread thread =new Thread(videosaver);
                    thread.start();
                    recordButton.setVisibility(View.INVISIBLE);
                    recordStop.setVisibility(View.VISIBLE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

//Stoping and finishing record

        recordStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopOrderCameIn =true;
                // acquiring the lock
                recordButton.setVisibility(View.VISIBLE);
                recordStop.setVisibility(View.INVISIBLE);


            }
        });

        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:


                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;


                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);


                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);


                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        startForeground(NOTIFICATION_ID, createNotification(this));
        return START_REDELIVER_INTENT;
    }


    // büyük ihtimal buranın içine yazman gerekecek ama tam yapısını bilmiyorum ///
    public void createOne()
    {
        metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        inflater = LayoutInflater.from(this);
        //trafficSignView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        /*
        final LayoutInflater inflater = LayoutInflater.from(this);
        final ImageView iconView = (ImageView) inflater.inflate(R.layout.widget_mail, null, false);
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getString(R.string.mail_address), null));
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_title));
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_content));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
         */

        mFloatingViewManager = new FloatingViewManager(this, this);
        mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_trash_fixed);
        mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_trash_action);
        // Setting Options(you can change options at any time)
        loadDynamicOptions();
        // Initial Setting Options (you can't change options after created.)
        //final FloatingViewManager.Options options = loadOptions(metrics);
        //mFloatingViewManager.addViewToWindow(trafficSignView, options);
        created = true;
    }
    public void showSpeedLimit(){
        trafficSignView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        final FloatingViewManager.Options options = loadOptions(metrics);
        mFloatingViewManager.addViewToWindow(trafficSignView, options);
        show = true;
    }
    public void endVideoRecording() {
         // Finalize the encoding, i.e. clear the buffers, write the header, etc.
            try {
                encoder.finish();
                System.out.println("Encoding is done!");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                NIOUtils.closeQuietly(out);
                CameraService.writeLock2.lock();
                counterForFrame=0;
                CameraService.writeLock2.unlock();
                System.out.println("Video Saved!");
            }


    };
    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {
        if (!isFinishing) {
            // Save the last position
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putInt(PREF_KEY_LAST_POSITION_X, x);
            editor.putInt(PREF_KEY_LAST_POSITION_Y, y);
            editor.apply();
        }
    }

    public void destroy() {
        if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            //mFloatingViewManager = null;
            created = false;
            show = false;
        }
    }

    private static Notification createNotification(Context context) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.default_floatingview_channel_id));
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(context.getString(R.string.mail_content_title));
        builder.setContentText(context.getString(R.string.content_text));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);



        return builder.build();
    }

    private void loadDynamicOptions() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        final String displayModeSettings = sharedPref.getString("settings_display_mode", "");
        if ("Always".equals(displayModeSettings)) {
            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_SHOW_ALWAYS);
        } else if ("FullScreen".equals(displayModeSettings)) {
            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
        } else if ("Hide".equals(displayModeSettings)) {
            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS);
        }

    }

    private FloatingViewManager.Options loadOptions(DisplayMetrics metrics) {
        final FloatingViewManager.Options options = new FloatingViewManager.Options();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Shape
        final String shapeSettings = sharedPref.getString("settings_shape", "Rectangle");
        if ("Circle".equals(shapeSettings)) {
            options.shape = FloatingViewManager.SHAPE_CIRCLE;
        } else if ("Rectangle".equals(shapeSettings)) {
            options.shape = FloatingViewManager.SHAPE_RECTANGLE;
        }

        // Margin
        final String marginSettings = sharedPref.getString("settings_margin", String.valueOf(options.overMargin));
        options.overMargin = Integer.parseInt(marginSettings);

        // MoveDirection
        final String moveDirectionSettings = sharedPref.getString("settings_move_direction", "");
        if ("Default".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_DEFAULT;
        } else if ("Left".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_LEFT;
        } else if ("Right".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_RIGHT;
        } else if ("Nearest".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_NEAREST;
        } else if ("Fix".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_NONE;
        } else if ("Thrown".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_THROWN;
        }

        options.usePhysics = sharedPref.getBoolean("settings_use_physics", true);

        // Last position
        final boolean isUseLastPosition = sharedPref.getBoolean("settings_save_last_position", false);
        if (isUseLastPosition) {
            final int defaultX = options.floatingViewX;
            final int defaultY = options.floatingViewY;
            options.floatingViewX = sharedPref.getInt(PREF_KEY_LAST_POSITION_X, defaultX);
            options.floatingViewY = sharedPref.getInt(PREF_KEY_LAST_POSITION_Y, defaultY);
        } else {
            // Init X/Y
            final String initXSettings = sharedPref.getString("settings_init_x", "");
            final String initYSettings = sharedPref.getString("settings_init_y", "");
            if (!TextUtils.isEmpty(initXSettings) && !TextUtils.isEmpty(initYSettings)) {
                final int offset = (int) (48 + 8 * metrics.density);
                options.floatingViewX = 3 * (int) (metrics.widthPixels * Float.parseFloat(initXSettings) - offset);
                options.floatingViewY = 3 * (int) (metrics.heightPixels * Float.parseFloat(initYSettings) - offset);
            }
        }

        // Initial Animation
        final boolean animationSettings = sharedPref.getBoolean("settings_animation", options.animateInitialMove);
        options.animateInitialMove = animationSettings;

        return options;
    }

    public static void changeSpeedSign(String speedLimit){
        boolean noUpdate = false;
        mFloatingViewManager.removeAllViewToWindow();
        switch(speedLimit) {
            case "speed_limit_20":
                trafficSignView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
                break;
            case "speed_limit_30":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_30, null, false);
                break;
            case "speed_limit_50":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_50, null, false);
                break;
            case "speed_limit_60":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_60, null, false);
                break;
            case "speed_limit_70":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_70, null, false);
                break;
            case "speed_limit_80":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_80, null, false);
                break;
            case "restriction_ends_80":
                trafficSignView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
                break;
            case "speed_limit_100":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_100, null, false);
                break;
            case "speed_limit_120":
                trafficSignView = (ImageView) inflater.inflate(R.layout.speed_120, null, false);
                break;
            default:
                noUpdate = true;
                //iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        }
        if(!noUpdate) {
            options = new FloatingViewManager.Options();
            options.overMargin = (int) (16 * metrics.density);
            mFloatingViewManager.addViewToWindow(trafficSignView, options);
        }
    }

}
