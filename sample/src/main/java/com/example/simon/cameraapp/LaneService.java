package com.example.simon.cameraapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


public class LaneService extends Service {

    public IBinder mBinder = new LaneService.LocalBinder();
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
        //BURAYA
        return Service.START_NOT_STICKY;
    }
}