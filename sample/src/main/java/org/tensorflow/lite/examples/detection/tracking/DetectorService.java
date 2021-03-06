package org.tensorflow.lite.examples.detection.tracking;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.Image;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import jp.co.recruit_lifestyle.sample.MainActivity;
import jp.co.recruit_lifestyle.sample.service.FloatingViewService;
import static com.example.simon.cameraapp.CameraService.recentPics;
import static com.example.simon.cameraapp.CameraService.previewHeight;
import static com.example.simon.cameraapp.CameraService.previewWidth;
import static com.example.simon.cameraapp.CameraService.isProcessingFrame;
import static com.example.simon.cameraapp.CameraService.rgbBytes;
import static com.example.simon.cameraapp.CameraService.yuvBytes;
import static com.example.simon.cameraapp.CameraService.imageConverter;
import static com.example.simon.cameraapp.CameraService.readLock;
import static java.lang.Thread.sleep;
import static jp.co.recruit_lifestyle.sample.MainActivity.UIrunnable;
import static jp.co.recruit_lifestyle.sample.MainActivity.closeAppStopDetection;
import static jp.co.recruit_lifestyle.sample.MainActivity.detectorServiceThread;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.addOtherSign;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.changeSpeedSign;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.removeOtherSign;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.show;
import static org.tensorflow.lite.examples.detection.tracking.DetectorService.DetectorMode.TF_OD_API;


public class DetectorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public DetectorService getServerInstance() {
            return DetectorService.this;
        }
    }
    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "retrained_graph.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final DetectorMode MODE = TF_OD_API;
    private static Bitmap data;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    @SuppressLint("NewApi")
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    public static Runnable postInferenceCallback;
    public  IBinder mBinder = new LocalBinder();
    OverlayView trackingOverlay;
    private Integer sensorOrientation;


    private static Bitmap croppedBitmap = null;

    public static int yRowStride;

    private static Classifier detector;

    private static boolean computingDetection = false;

    boolean mBounded;
    static FloatingViewService mServer;
    static HashSet<String> speedLabels;
    static HashSet<String> otherSigns;
    //static HashMap<String, Integer> otherLabels;
    static String previousLabel;
    public static  boolean started = false;
    public static int flag = 3;

    private static Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    public static Handler handler;
    private static String[] otherLabels;
    private static int[] otherLabelDurations;
    private static int numOtherLabels;
    private static long initTime;
    static AssetManager assetM;

    public DetectorService() {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Detector service Started.");
        previousLabel = "";
        speedLabels = new HashSet<String>();
        speedLabels.add("speed_limit_20");
        speedLabels.add("speed_limit_30");
        speedLabels.add("speed_limit_50");
        speedLabels.add("speed_limit_60");
        speedLabels.add("speed_limit_70");
        speedLabels.add("speed_limit_80");
        speedLabels.add("speed_limit_100");
        speedLabels.add("speed_limit_120");


        otherSigns = new HashSet<>();
        otherSigns.add("priority_road");
        otherSigns.add("give_way");
        otherSigns.add("stop");
        otherSigns.add("no_entry");
        otherSigns.add("danger");
        otherSigns.add("go_right");
        otherSigns.add("go_left");
        otherSigns.add("go_straight");
        otherSigns.add("keep_right");
        otherSigns.add("keep_left");
        otherSigns.add("roundabout");

        otherLabels = new String[2];
        for(int i = 0; i < 2; i++){
            otherLabels[i] = "";
        }

        otherLabelDurations = new int[2];
        for(int i = 0; i < 2; i++){
            otherLabelDurations[i] = 0;
        }

        numOtherLabels = 0;

        /*
        Bundle bundle = intent.getExtras();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Size size =  bundle.getSize("size");
        }
         */

        int cropSize = TF_OD_API_INPUT_SIZE;
        try {
            assetM = getAssets();
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            assetM,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
        }
        closeAppStopDetection=false;


       // rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        sensorOrientation = 0;

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        postInferenceCallback =
                new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void run() {

                        if(closeAppStopDetection||Thread.currentThread().isInterrupted()){
                            try {
                                throw new InterruptedException();
                            } catch (InterruptedException e) {
                               // e.printStackTrace();
                            }
                        }
                        else{
                            readLock.lock();
                            if (recentPics.size() > 0) {
                               // System.out.println("recentpic is not empty");
                                Bitmap bmp = (recentPics).get(recentPics.size() - 1);
                                data = bmp.copy(bmp.getConfig(), false);
                                readLock.unlock();
                                isProcessingFrame = true;
                                processImage();
                                System.out.println("DETECT");
                                readyForNextImage();
                            } else {
                                System.out.println("DetectorService:0 recent pics");
                                readLock.unlock();
                            }
                            isProcessingFrame = false;
                        }
                    }
                };
        initTime = System.currentTimeMillis();
        started = true;
        super.onStartCommand(intent, flags, startId);
        detectorServiceThread = new Thread(postInferenceCallback);
        detectorServiceThread.start();
     //   sign_detect();
        handler = new Handler();
        return Service.START_NOT_STICKY;
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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void processImage() {
        //System.out.println("new image processing");
        //System.out.println("PROCESS IMAGE");
        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(data, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        //final List<Classifier.Recognition> results = new ArrayList<>();
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
        }

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {

                // Update Service HERE
                System.out.println("RESULT ID: " + result.getTitle());
                if (speedLabels.contains(result.getTitle()) && !result.getTitle().equals(previousLabel) && show) {
                    //mServer.changeSpeedSign(result.getTitle());
                    System.out.println("speed sign recognized");
                    UIrunnable = new Runnable() {
                        @Override
                        public void run() {
                            changeSpeedSign(result.getTitle());
                        }
                    };
                    handler.post(UIrunnable);
                    previousLabel = result.getTitle();
                }
                else if(numOtherLabels < 2 && otherSigns.contains(result.getTitle()) && show){
                    System.out.println("other sign recognized");
                    boolean contains = false;
                    for(int i = 0; i < 2; i++){
                        if(otherLabels[i] != null && otherLabels[i].equals(result.getTitle())){
                            contains = true;
                            break;
                        }
                    }
                    if(!contains) {
                        int i;
                        for(i = 0; i < 2; i++){
                            if(otherLabels[i] == null || otherLabels[i].equals("")){
                                otherLabels[i] = result.getTitle();
                                break;
                            }
                        }
                        otherLabelDurations[i] = 20;
                        numOtherLabels++;
                        // UPDATE UI
                        Runnable temp1 = new Runnable() {
                            @Override
                            public void run() {
                                addOtherSign(result.getTitle());
                            }
                        };
                        handler.post(temp1);
                    }
                }
            }
        }
        if(show) {
          //  System.out.println("Sign Detecor thread id:"+Thread.currentThread().getId()+" "+ Thread.currentThread().getName());
            for(int i = 0; i < 2; i++){
                otherLabelDurations[i] -= 1;
                if(otherLabelDurations[i] == 0 && otherLabels[i] != null && !otherLabels[i].equals("")){
                    // UPDATE UI
                    String label = otherLabels[i];
                    Runnable temp1 = new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("REMOVE LABEL");
                            removeOtherSign(label);
                        }
                    };
                    handler.post(temp1);
                    otherLabels[i] = "";
                    numOtherLabels--;
                }
            }
        }
        System.out.println("num other labels: " + numOtherLabels);
        computingDetection = false;
        //readyForNextImage();
    }

    public enum DetectorMode {
        TF_OD_API;
    }

    public static void readyForNextImage() {
        //System.out.println("Called nextIm");
        if (postInferenceCallback != null) {
            //System.out.println("Ready for new image");
            if(System.currentTimeMillis() - initTime > 20000){
                detectorServiceThread.interrupt();
                detectorServiceThread = null;
                /*
                try {
                    detector =
                            TFLiteObjectDetectionAPIModel.create(
                                    assetM,
                                    TF_OD_API_MODEL_FILE,
                                    TF_OD_API_LABELS_FILE,
                                    TF_OD_API_INPUT_SIZE,
                                    TF_OD_API_IS_QUANTIZED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("MODEL RELOADED");
                 */
                detectorServiceThread = new Thread(postInferenceCallback);
                detectorServiceThread.start();
                initTime = System.currentTimeMillis();
            }
            else {
                //SystemClock.sleep(100);
                postInferenceCallback.run();
            }
           // (new Handler()).postDelayed(postInferenceCallback, 100);

        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public static int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

}
