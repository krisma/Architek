package com.example.krisma.architek.asynctasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;

/**
 * Created by smp on 13/08/15.
 */
public class AsyncSendBitmap extends AsyncTask<Bitmap, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(Bitmap... params) {

        // TODO: 2. Upload to server
        // TODO: 3. Server processes image
        // TODO: 4. Image is returned to user a few seconds after

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap){

        // TODO: 5. User can position it on the map (like Google does)
        // TODO: 6. Once the map is placed, information is augmented ontop
        // TODO: 7. Done

    }
}
