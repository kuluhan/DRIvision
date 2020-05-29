package com.example.simon.cameraapp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import 	org.json.JSONArray;
import org.json.JSONObject;
import 	android.media.MediaPlayer;

import androidx.annotation.RequiresApi;

import java.util.Collections;

//import jp.co.recruit.floatingview.R;
import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.MainActivity;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import static com.example.simon.cameraapp.CameraService.readLock;
import static jp.co.recruit_lifestyle.sample.MainActivity.closeAppStopDetection;
import static com.example.simon.cameraapp.CameraService.recentPics;
import static com.example.simon.cameraapp.FaceService.notLooking;


public class VehicleService extends Service  {
    public static boolean started;
    static Bitmap data;

    public static OkHttpClient client;
    MediaType JSON;
    URL url;
    String host;
    int port;
    String file;
    String protocol;
    String fileToRequest;
    String fileToRequest2;
    String fileName;
    public static Runnable vehicleDetector;
    public static Thread vehicleThread;
    int k;
    static boolean responseReceived = true;
    final double FILERESOLUTIONPERCENT= 0.5;
    ArrayList<ArrayList<Rectangle>> toRemember;
    MediaPlayer mp;
    public IBinder mBinder = new LocalBinder();
    //public static boolean stopOrderCameIn;
    // Class used for the client Binder.
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public VehicleService getServerInstance() {
            return VehicleService.this;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        System.out.println("Vehicle başladı");
        host = "142.93.38.174"; ///////////////////ip of digitalocean : 142.93.38.174 /// zeyn local ip 192.168.1.22
        port = 8001;
        file = "/predict";
        protocol ="HTTP";
        started=true;
        //initialize empty vehicleList and load alarm sound
        toRemember =new ArrayList();
        mp = MediaPlayer.create(this, R.raw.sample);
        k=1;
        try {
            url = new URL(protocol, host,port, file);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        notLooking=true;
        client = new OkHttpClient().newBuilder().connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).build();

        JSON = MediaType.parse("application/json; charset=utf-8");
        vehicleDetector = new Runnable(){
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                while(true) {
                    if (closeAppStopDetection || Thread.currentThread().isInterrupted()) {
                        System.out.println("Interruptedddddd!");
                           break;
                    } else {
                        readLock.lock();
                        try {
                            // access the resource protected by this lock
                            Bitmap bmp = (recentPics).get(recentPics.size() - 1);
                            data = bmp.copy(bmp.getConfig(), false);
                        } finally {
                            readLock.unlock();
                        }
                        try {
                            makeGetRequest();
                            SystemClock.sleep(500);
                        } catch (IOException ignored) {
                            System.out.println("IOException make request" + ignored);
                        } catch (InterruptedException m) {
                            System.out.println("InterruptedException " + m);
                        }
                    }
                }
                try{
                   throw new InterruptedException();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        vehicleThread =new Thread(vehicleDetector);
        vehicleThread.start();
        return Service.START_NOT_STICKY;
    }



    public void makeGetRequest() throws IOException,InterruptedException {
        GetTask task = new GetTask();
        task.execute();
    }


    //I changed the result to JSONObject
    public  class  GetTask extends AsyncTask<Void , Void, JSONObject> {
        private Exception exception;
        protected  JSONObject doInBackground(Void... urls) {
            try {
                // rastgee bi string verdim iÃ§ine override iÃ§in bir amaci yok
                return get(url.toString());
            } catch (Exception e) {
                this.exception = e;
                System.out.println(e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject getResponse) {
            if (getResponse!=null) {

                try {
                    String vehiclestr =getResponse.get("vehicles").toString();
                    System.out.println(vehiclestr);
                    vehiclestr= vehiclestr.substring(1,vehiclestr.length()-1);
                    JSONArray jaFiles= new JSONArray(vehiclestr);

                    ArrayList<ArrayList<Rectangle>> fileVehicleList= new ArrayList();

                    for(int t=0;t<jaFiles.length();t++){

                        String oneFileResult =jaFiles.get(t).toString();
                        JSONArray picsVehic =new JSONArray(oneFileResult);
                        ArrayList<Rectangle> recs=new ArrayList();
                        for(int k=0;k<picsVehic.length();k++){
                            String or=picsVehic.get(k).toString();
                            String[] dividedRecInfo = or.substring(1,or.length()-1).split(",");
                            Rectangle r= new Rectangle(dividedRecInfo[0],Math.round(Float.parseFloat(dividedRecInfo[1])),Math.round(Float.parseFloat(dividedRecInfo[2])),Math.round(Float.parseFloat(dividedRecInfo[3])),Math.round(Float.parseFloat(dividedRecInfo[4])),Float.parseFloat(dividedRecInfo[5]));
                            recs.add(r);
                        }
                        Collections.sort(recs);
                        fileVehicleList.add(recs);
                    }

                    for(int y=0;y<fileVehicleList.size();y++)
                    {
                        toRemember.add(fileVehicleList.get(y));
                    }
                    //split("\[")[1]
                    if(evaluateCrashing()&& notLooking)
                    {
                        mp.start();
                    }

                } catch (Exception e)
                {
                    Log.e("Error :(","--"+e);
                }
            }

        }
        private boolean evaluateCrashing(){

            int remSize=toRemember.size();
            //ring if it is too big car or medium person
            ArrayList<Rectangle> latest= toRemember.get(remSize-1);

            for(int k=0;k<latest.size();k++){
                Rectangle cur =latest.get(k);
                if(((cur.getType().equals("person"))&&(cur.getArea()>100*200*FILERESOLUTIONPERCENT))||((cur.getType().equals("car")||(cur.getType().equals("bus")))&&cur.getArea()>FILERESOLUTIONPERCENT*300*300 ))
                {
                    Log.d("Here","It found person or realy big car");
                    return true;
                }
            }

            //noticing too much speed and car getting close compared to earlier images
            if(remSize>0)
            {
                for(int u=0;u<remSize-1;u++)
                {
                    ArrayList<Rectangle> earlier=toRemember.get(u);
                    ArrayList<Rectangle> later= toRemember.get(u+1);
                    //FILERESOLUTIONPERCENT
                    for(int iter=0;iter<later.size();iter++)
                    {
                        Rectangle cur =later.get(iter);
                        ArrayList<Integer> matches= possibleMatch(earlier,cur);
                        if((matches.isEmpty())&&(((cur.getArea())>250*300*FILERESOLUTIONPERCENT)||(cur.getH()>280*FILERESOLUTIONPERCENT)||(cur.getW()>420*FILERESOLUTIONPERCENT))) {
                            Log.d("Tagat","noticing too much speed and car getting close compared to earlier images ");
                            return true;
                        }
                    }
                }
                if(remSize>3)
                    toRemember.remove(0);
            }
            return false;
        }
        private ArrayList<Integer> possibleMatch(ArrayList<Rectangle> list,Rectangle given){

            ArrayList<Integer> matches=new ArrayList();

            for(int c=0;c<list.size();c++)
            {
                Rectangle cur= list.get(c);
                if((Math.abs((given.getArea()/cur.getArea())-1)<0.1)&&((Math.abs(given.getX()/cur.getX()-1)<0.08))&&((Math.abs(given.getY()/cur.getY()-1)<0.08))) ///image yÃ¼zde 30dan fazla deÄŸiÅŸmediÄŸyse
                    matches.add(c);
            }
            return matches;

        }
        public JSONObject get(String url) throws IOException {
            try {
                // take the image from the asset folder
                //String img = "img.jpg";
                System.out.println("vehicle run:"+k);
                k++;

                Response response= makeRequest();
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
        private Response makeRequest(){
            //  AssetManager assetManager = getAssets();
            // make request to send picture && aws account belongs to Musab

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            data.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            data.recycle();

            MediaType mediaType = MediaType.parse("multipart/form-data; boundary=--------------------------205063402178265581033669");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("fileToRequest", "fileName",
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
            }catch (IOException e){
                System.out.println("make request execute exception bağlanamadı: " + e );
            }
            return null;
        }
        /*private byte[] readImage(String img, AssetManager assetManager){
            InputStream inputStream = null;
            try {
                inputStream = assetManager.open(img);

            } catch (IOException e) {
                Log.e("Test", "Cannot load image from assets");
            }
            Bitmap input = BitmapFactory.decodeStream(inputStream);


            // Compress the image because process jpeg is shorter than other types
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            input.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return byteArray;
        }*/
    }

}
