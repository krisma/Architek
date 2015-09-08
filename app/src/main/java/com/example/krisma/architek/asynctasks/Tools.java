package com.example.krisma.architek.asynctasks;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by smp on 02/09/15.
 */
public class Tools {

    public static ProgressDialog getSpinnerDialog(Context context){
        ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Please wait...");
        progress.setCancelable(false);
        return progress;
    }
}
