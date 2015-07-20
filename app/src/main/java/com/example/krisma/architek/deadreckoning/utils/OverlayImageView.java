package com.example.krisma.architek.deadreckoning.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.example.krisma.architek.deadreckoning.particlefilter.Particle;
import com.example.krisma.architek.deadreckoning.particlefilter.ParticleSet;
import com.example.krisma.architek.deadreckoning.particlefilter.Pose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smp on 16/07/15.
 */
public class OverlayImageView extends ImageView {

    private final Context context;
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint avgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    List<Point> points = new ArrayList<Point>();
    private boolean userLocationSet;
    private Particle[] particles;

    public ParticleSet getParticleSet() {
        return particleSet;
    }

    private ParticleSet particleSet;
    private Bitmap bitmap;


    public OverlayImageView(Context context) {
        super(context);
        this.context = context;
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        avgPaint.setColor(Color.GREEN);
        avgPaint.setStyle(Paint.Style.STROKE);
        avgPaint.setStrokeWidth(10);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.setPadding(0, 0, 0, 0);

        for (Point point : points) {
            canvas.drawCircle(point.x, point.y, 10, paint);
        }

        Pose p;

        if (particles != null && particles.length > 0) {
            for (int i = 0; i < particles.length; i++)
            {
                p = particles[i].getPose();

                canvas.drawCircle((float)p.getX(), (float)p.getY(), 5f, paint);
            }

            Pose averagePose = particleSet.getAveragePose(); // map to latlng
            canvas.drawCircle((float) averagePose.getX(), (float) averagePose.getY(), 5f, avgPaint);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(!userLocationSet){
            Point point = new Point();
            point.x = (int)event.getX();
            point.y = (int)event.getY();
            points.add(point);
            invalidate();

            this.particleSet = new ParticleSet(context, 1000, bitmap, point.x, point.y);

            userLocationSet = true;
        }


        return super.onTouchEvent(event);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.setImageBitmap(bitmap);
    }

    public void displayParticles(){
        Pose p = particleSet.getPose();
        this.particles = particleSet.getParticles();

        invalidate();
        Log.d("Particles", p.getX() + " : " + p.getY());
    }
}
