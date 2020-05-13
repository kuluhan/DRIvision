package com.example.simon.cameraapp;

/**
 * Created by Simon on 2/11/2017.
 */

import android.hardware.Camera;

public class cameraobj {
    static Camera cam;

    public static Camera getCam() {
        return cam;
    }

    public static void setCam(Camera cam) {
        cam = cam;
    }
}