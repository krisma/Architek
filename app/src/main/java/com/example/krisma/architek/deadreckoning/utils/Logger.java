package com.example.krisma.architek.deadreckoning.utils;

import android.util.Log;

/**
 * Created by smp on 10/08/15.
 */
public class Logger {

    private final String TAG;
    private boolean enabled = true;

    public Logger(Class clazz){
        this.TAG = clazz.getSimpleName();
    }

    public void enableLogging(){
        this.enabled = true;
    }
    public void disableLogging(){
        this.enabled = false;
    }

    public void d(String s){
        if(enabled) Log.d(TAG, s);
    }

    public void i(String s){
        if(enabled) Log.i(TAG, s);
    }

    public void v(String s){
        if(enabled) Log.v(TAG, s);
    }

    public void w(String s){
        if(enabled) Log.w(TAG, s);
    }

    public void e(String s){
        if(enabled) Log.e(TAG, s);
    }
}
