package com.example.krisma.architek;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Debug;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.krisma.architek.deadreckoning.DeadReckoning;
import com.example.krisma.architek.deadreckoning.utils.OverlayImageView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DebugActivity extends Activity {
    private static final Logger log = LoggerFactory.getLogger(DebugActivity.class);

    private DeadReckoning deadReckoning;
    private OverlayImageView oiv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        Intent i= new Intent(this, DeadReckoning.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        this.startService(i);


        RelativeLayout holder = (RelativeLayout) findViewById(R.id.debugHolder);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.demo2_edged_thick);


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        double aspect = bitmap.getWidth()/bitmap.getHeight();

        final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1080, (int) (909.473684208), true);

        oiv = (OverlayImageView) findViewById(R.id.debugView);
        oiv.setBitmap(scaledBitmap);
        oiv.setX(0);
        oiv.setY(0);

        log.info("W: {}, H: {} -- W: {}, H: {}, A: {} ({}) -- Scaled Height: {}", width, height, bitmap.getWidth(), bitmap.getHeight(), aspect, bitmap.getWidth()/bitmap.getHeight(), (int)(width * aspect));


    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            deadReckoning = ((DeadReckoning.LocalBinder)service).getService();
            deadReckoning.setActivity(DebugActivity.this);


        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            deadReckoning = null;
        }
    };

    public OverlayImageView getOIV(){
        return oiv;
    }
}
