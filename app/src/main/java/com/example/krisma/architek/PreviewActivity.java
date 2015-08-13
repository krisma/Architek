package com.example.krisma.architek;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;


public class PreviewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        
        Bundle bundle = getIntent().getBundleExtra("bitmapBundle");
        if(bundle != null){
            Bitmap imageBitmap = (Bitmap) bundle.get("bitmap");
            ImageView preview = (ImageView) findViewById(R.id.preview);
            preview.setImageBitmap(imageBitmap);
        }

    }

}
