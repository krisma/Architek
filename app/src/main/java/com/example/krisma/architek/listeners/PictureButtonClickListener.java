package com.example.krisma.architek.listeners;

import android.content.Intent;
import android.provider.MediaStore;
import android.view.View;

import com.example.krisma.architek.MapsActivity;

public class PictureButtonClickListener {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private final MapsActivity activity;

    View.OnClickListener picButtonClickListener;

    public View.OnClickListener getPicButtonClickListener() {
        return picButtonClickListener;
    }

    public PictureButtonClickListener(final MapsActivity act) {
            this.activity = act;
            this.picButtonClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dispatchTakePictureIntent();
                }
            };
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


}