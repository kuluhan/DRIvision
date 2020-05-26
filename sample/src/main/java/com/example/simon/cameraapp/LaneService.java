package com.example.simon.cameraapp;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.co.recruit.floatingview.R;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.simon.cameraapp.CameraService.readLock;
import static com.example.simon.cameraapp.CameraService.rgbFrameBitmap;
import static jp.co.recruit_lifestyle.sample.MainActivity.closeAppStopDetection;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.removeLaneIcon;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.showLaneIcon;


public class LaneService extends Service {

    public IBinder mBinder = new LaneService.LocalBinder();
    public static boolean started;
    static Bitmap data;

    public static OkHttpClient client;
    MediaType JSON;
    URL url;
    URL url2;
    String host;
    int port;
    String endpoint;
    String endpoint2;
    String protocol;
    String fileToRequest;
    String fileToRequest2;
    String fileName;
    public static Runnable laneTracker;
    public static Thread laneThread;
    int k;
    static boolean responseReceived = true;
    final double FILERESOLUTIONPERCENT = 1.5;
    ArrayList<ArrayList<Rectangle>> toRemember;
    MediaPlayer mp;
    private static TextToSpeech t1;
    long alertMade;
    public static boolean laneIconShow = false;
    public static Handler handler;
    private long initTime;
    //public static boolean stopOrderCameIn;
    // Class used for the client Binder.

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public LaneService getServerInstance() {
            return LaneService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        System.out.println("başladı");
        host = "142.93.38.174"; ///////////////////ip of digitalocean : 142.93.38.174 /// zeyn local ip 192.168.1.22
        port = 8000;
        endpoint = "/predict";
        protocol = "HTTP";
        started = true;
        //initialize empty vehicleList and load alarm sound
        toRemember = new ArrayList();
        mp = MediaPlayer.create(this, R.raw.sample);
        k = 1;
        try {
            url = new URL(protocol, host, port, endpoint);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        client = new OkHttpClient().newBuilder().connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).build();

        JSON = MediaType.parse("application/json; charset=utf-8");
        laneTracker = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                if (closeAppStopDetection || Thread.currentThread().isInterrupted()) {
                    try {
                        throw new InterruptedException();
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                    }
                } else {
                    readLock.lock();
                    try {
                        // access the resource protected by this lock
                        // OLD VERSION
                        // Bitmap bmp = (recentPics).get(recentPics.size() - 1);
                        // data = bmp.copy(bmp.getConfig(), false);
                        // NEW VERSION
                        data = rgbFrameBitmap.copy(rgbFrameBitmap.getConfig(), false); // TODO CHANGE WHEN 2 CAMERA SUPPORT ADDED
                    } finally {
                        readLock.unlock();
                    }

                    try {
                        makeGetRequest();
                        SystemClock.sleep(700);

                    } catch (IOException ignored) {
                        System.out.println("IOException make request" + ignored);
                    } catch (InterruptedException m) {
                        System.out.println("InterruptedException " + m);
                    }
                    readyForNextIm();

                }
            }
        };
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        handler = new Handler();
        alertMade = System.currentTimeMillis();
        initTime = alertMade;
        laneThread = new Thread(laneTracker);
        laneThread.start();
        return Service.START_NOT_STICKY;
    }


    public void readyForNextIm() {
        if (laneTracker != null) {
            //facePoseEstimator.run();
            //(new Handler()).postDelayed(vehicleDetector, 1000);
            if(System.currentTimeMillis() - initTime > 30000){
                laneThread.interrupt();
                laneThread = null;
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
                laneThread = new Thread(laneTracker);
                laneThread.start();
                initTime = System.currentTimeMillis();
            }
            else {
                //SystemClock.sleep(100);
                laneTracker.run();
            }
            //facePoseEstimator.run();
            //(new Handler()).postDelayed(vehicleDetector, 1000);
        }

    }


    public void makeGetRequest() throws IOException, InterruptedException {
        GetTask task = new GetTask();
        task.execute();
    }


    //I changed the result to JSONObject
    public class GetTask extends AsyncTask<Void, Void, JSONObject> {
        private Exception exception;

        protected JSONObject doInBackground(Void... urls) {
            try {
                // rastgee bi string verdim iÃ§ine override iÃ§in bir amaci yok
                return get(url.toString());
            } catch (Exception e) {
                this.exception = e;
                System.out.println(e);
                return null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        protected void onPostExecute(JSONObject getResponse) {
            if (getResponse != null) {
                try {
                    double centerDist = getResponse.getJSONArray("distance_from_center_arr").getDouble(0);
                    System.out.println("distance from center: " + centerDist);
                    if(centerDist == -1){
                        // TODO put icon
                        /*
                        Toast.makeText(
                                LaneService.this,
                                "lane not detected",
                                Toast.LENGTH_LONG)
                                .show();

                         */
                        if(!laneIconShow){
                            Runnable temp1 = new Runnable() {
                                @Override
                                public void run() {
                                    removeLaneIcon();
                                    showLaneIcon("no_lane_icon");
                                }
                            };
                            handler.post(temp1);
                            laneIconShow = true;
                        }
                    }
                    else{
                        if(centerDist > 0.6){
                            Toast.makeText(
                                    LaneService.this,
                                    "lane switched",
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                        if(laneIconShow){
                            Runnable temp1 = new Runnable() {
                                @Override
                                public void run() {
                                    removeLaneIcon();
                                    showLaneIcon("green_lane_icon");
                                }
                            };
                            handler.post(temp1);
                            laneIconShow = false;
                        }
                    }

                } catch (Exception e) {
                    Log.e("Error :(", "--" + e);
                }
            }

        }

        public JSONObject get(String url) throws IOException {
            try {
                // take the image from the asset folder
                //String img = "img.jpg";
                System.out.println("k:" + k);
                String img = k + ".jpg";
                k++;

                Response response = makeRequest();
                JSONObject json = new JSONObject(response.body().string());

                return json;
                //  return new JSONObject(response.body().string());

            } catch (UnknownHostException | UnsupportedEncodingException e) {
                System.out.println("Error: " + e.getLocalizedMessage());
            } catch (Exception e) {
                System.out.println("Other Error: " + e.getLocalizedMessage());
            }

            return null;
        }

        private Response makeRequest() {
            //  AssetManager assetManager = getAssets();
            // make request to send picture && aws account belongs to Musab

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            data.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            data.recycle();

            MediaType mediaType = MediaType.parse("multipart/form-data; boundary=--------------------------205063402178265581033669");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("fileToRequest", "fileName.jpg",
                            RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .addHeader("Content-Type", "multipart/form-data; boundary=--------------------------205063402178265581033669")
                    .build();
            try {

                Response response = client.newCall(request).execute();

                return response;
            } catch (IOException e) {
                System.out.println("make request execute exception bağlanamadı: " + e);
            }
            return null;
        }
    }
}