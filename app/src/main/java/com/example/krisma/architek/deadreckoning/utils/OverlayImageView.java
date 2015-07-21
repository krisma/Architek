package com.example.krisma.architek.deadreckoning.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.example.krisma.architek.deadreckoning.particlefilter.Particle;
import com.example.krisma.architek.deadreckoning.particlefilter.ParticleSet;
import com.example.krisma.architek.deadreckoning.particlefilter.Pose;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smp on 16/07/15.
 */
public class OverlayImageView extends ImageView {

    private Context context;
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

    List<Point> pointsOnRoute = new ArrayList<>();

    //region Constructors
    public OverlayImageView(Context context) {
        super(context);
        init(context);
    }

    public OverlayImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OverlayImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        avgPaint.setColor(Color.GREEN);
        avgPaint.setStyle(Paint.Style.STROKE);
        avgPaint.setStrokeWidth(10);
    }

    //endregion

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.setPadding(0, 0, 0, 0);

        for (Point point : points) {
            canvas.drawCircle(point.x, point.y, 10, paint); // TODO: FLIP?
        }

        Pose p;

        if (particles != null && particles.length > 0) {
            for (int i = 0; i < particles.length; i++) {
                p = particles[i].getPose();

                canvas.drawCircle((float) p.getX(), (float) p.getY(), 5f, paint); // TODO: FLIP?
            }

            Pose averagePose = particleSet.getPose(); // map to latlng
            canvas.drawCircle((float) averagePose.getX(), (float) averagePose.getY(), 5f, avgPaint); // TODO: FLIP?
        }

        drawCorners(canvas, 0);
        canvas.drawRect(this.getDrawable().getBounds(), avgPaint);

        updatePath(canvas);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Point point = new Point();
        point.x = (int) event.getX();
        point.y = (int) event.getY();

        points = new ArrayList<>();
        pointsOnRoute = new ArrayList<>();

        if(!userLocationSet){
            particleSet = new ParticleSet(context, 1000, bitmap, point.x, point.y); // TODO: FLIP?
            this.particles = particleSet.getParticles();
        } else {
            particleSet.reset(point.x, point.y);
            this.particles = particleSet.getParticles();
        }

        points.add(point);

        invalidate();

        return super.onTouchEvent(event);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.setImageBitmap(bitmap);
    }

    public void displayParticles() {
        Pose p = particleSet.getPose();
        pointsOnRoute.add(new Point((int) p.getX(), (int) p.getY())); // TODO: FLIP?
        this.particles = particleSet.getParticles();

        invalidate();
        // Log.d("Particles", p.getX() + " : " + p.getY());
    }

    private void drawCorners(Canvas canvas, float offsetH) {
        canvas.drawCircle(getLeft(), getTop() + offsetH, 30, avgPaint);
        canvas.drawCircle(getRight(), getTop() + offsetH, 30, avgPaint);
        canvas.drawCircle(getLeft(), getBottom() - offsetH, 30, avgPaint);
        canvas.drawCircle(getRight(), getBottom() - offsetH, 30, avgPaint);
    }

    private void updatePath(Canvas canvas) {
        Point lastPoint = null;

        for (int z = 0; z < pointsOnRoute.size(); z++) {
            Point point = pointsOnRoute.get(z);
            if (lastPoint == null) {
                lastPoint = point;
                continue;
            }
            canvas.drawLine(lastPoint.x, lastPoint.y, point.x, point.y, avgPaint); // TODO: FLIP?
            lastPoint = point;
        }
    }

}
