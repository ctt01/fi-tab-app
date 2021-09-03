package com.simetriagrupo.fictab.activities;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class FicTab extends Application {
    public static String BASE_URL = "https://fichadas-api.gesmartweb.com/simetria";
    public static int POST_OK = 200;
    public static int CREATE_OK = 201;
    public static int POST_KO = 400;
    private static String PREFERENCES = "com.simetriagrupo.fictab";
    private static String DEVICE = "device";
    private static String LATITUD = "lat";
    private static String LONGITUD = "long";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = this.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
    }

    public String getDevice() {
        return prefs.getString(DEVICE,null);
    }

    public void setDevice(String device) {
        editor = prefs.edit();
        editor.putString(DEVICE,device);
        editor.commit();
    }
    public float getLatitud() {
        return prefs.getFloat(LATITUD,0);
    }

    public void setLatitud(float lat) {
        editor = prefs.edit();
        editor.putFloat(LATITUD,lat);
        editor.commit();
    }

    public float getLongitud() {
        return prefs.getFloat(LONGITUD,0);
    }

    public void setLongitud(float lon) {
        editor = prefs.edit();
        editor.putFloat(LONGITUD,lon);
        editor.commit();
    }
}
