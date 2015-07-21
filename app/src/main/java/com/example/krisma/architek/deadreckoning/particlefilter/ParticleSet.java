package com.example.krisma.architek.deadreckoning.particlefilter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;


public class ParticleSet{
    // Constants
    private static final float BIG_FLOAT = 10000f;

    // Static variables
    public static int maxIterations = 100;
    private double startY;
    private double startX;
    private final Bitmap floorplan;
    private final Canvas canvas;
    private final Paint paint;
    //private final ImageView debugView;
    private final Bitmap mutableBitmap;
    private final Context context;
    private final int startH;

    // Instance variables
    private float distanceNoiseFactor = 1f; // 2
    private float angleNoiseFactor = 10f;  // 10
    private float spreadArea = 10;
    private int numParticles;
    private Particle[] particles;


    private double _x, _y, _heading;
    private double minX, maxX, minY, maxY;
    private double varX, varY, varH;

    /**
     * Create a set of particles.
     */
    public ParticleSet(Context context, int numParticles, Bitmap floorplan, double startX, double startY) {
        this.context = context;
        this.numParticles = numParticles;
        this.startX = startX;
        this.startY = startY;
        this.startH = 0;
        this.floorplan = floorplan;

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        mutableBitmap = floorplan.copy(Bitmap.Config.ARGB_8888, true);

//        if(startX > floorplan.getWidth()){
//            startX = floorplan.getWidth() - 10;
//        }
//        if(startX < 0){
//            startX = 10;
//        }
//
//        if(startY > floorplan.getHeight()){
//            startY = floorplan.getHeight() - 10;
//        }
//        if(startY < 0){
//            startY = 10;
//        }

        canvas = new Canvas(mutableBitmap);
        canvas.drawCircle((float) startX, (float) startY, 10, paint);


        particles = new Particle[numParticles];
        for (int i = 0; i < numParticles; i++) {
            particles[i] = generateParticle();
        }
    }

    public Pose getPose() {
        estimatePose();
        return new Pose(_x, _y, _heading);
    }

    public void reset(double startX, double startY){
        this.startX = startX;
        this.startY = startY;
        particles = new Particle[numParticles];
        for (int i = 0; i < numParticles; i++) {
            particles[i] = generateParticle();
        }
    }

    private Particle generateParticle() {
        double hm = 360 * Math.random();

        double x = startX + Math.random() * spreadArea - Math.random() * spreadArea;
        double y = startY + Math.random() * spreadArea - Math.random() * spreadArea;

        Particle p = new Particle(context, floorplan, new Pose((float) x, (float) y, (float) hm));
        p.setWeight(1);

        return p;
    }

    /**
     * Estimate pose from weighted average of the particles
     * Calculate statistics
     */
    public void estimatePose() {
        float totalWeights = 0;
        float estimatedX = 0;
        float estimatedY = 0;
        float estimatedAngle = 0;
        varX = 0;
        varY = 0;
        varH = 0;
        minX = BIG_FLOAT;
        minY = BIG_FLOAT;
        maxX = -BIG_FLOAT;
        maxY = -BIG_FLOAT;

        for (int i = 0; i < numParticles; i++) {
            Pose p = particles[i].getPose();
            double x = p.getX();
            double y = p.getY();
            //float weight = particles.getParticle(i).getWeight();
            float weight = 1; // weight is historic at this point, as resample has been done
            estimatedX += (x * weight);
            varX += (x * x * weight);
            estimatedY += (y * weight);
            varY += (y * y * weight);
            double head = p.getHeading();
            estimatedAngle += (head * weight);
            varH += (head * head * weight);
            totalWeights += weight;

            if (x < minX) minX = x;

            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        estimatedX /= totalWeights;
        varX /= totalWeights;
        varX -= (estimatedX * estimatedX);
        estimatedY /= totalWeights;
        varY /= totalWeights;
        varY -= (estimatedY * estimatedY);
        estimatedAngle /= totalWeights;
        varH /= totalWeights;
        varH -= (estimatedAngle * estimatedAngle);

//        // Normalize angle
        while (estimatedAngle > 180) estimatedAngle -= 360;
        while (estimatedAngle < -180) estimatedAngle += 360;

        _x = estimatedX;
        _y = estimatedY;
        _heading = estimatedAngle;
    }

    /**
     * Resample the set picking those with higher weights.
     * <p/>
     * Note that the new set has multiple instances of the particles with higher
     * weights.
     *
     * @return true iff lost
     */

    public boolean resample() {
        // Rename particles as oldParticles and create a new set
        Particle[] oldParticles = particles.clone();
        particles = new Particle[numParticles];

        // Continually pick a random number and select the particles with
        // weights greater than or equal to it until we have a full
        // set of particles.
        int count = 0;
        int iterations = 0;

        while (count < numParticles) {
            iterations++;
            if (iterations >= maxIterations) {
                Log.d("Resample", "Lost: count = " + count);
                if (count > 0) { // Duplicate the ones we have so far
                    for (int i = count; i < numParticles; i++) {
                        particles[i] = new Particle(context, floorplan, particles[i % count].getPose());
                        particles[i].setWeight(particles[i % count].getWeight());
                    }
                    return false;
                } else { // Completely lost - generate a new set of particles
                    for (int i = 0; i < numParticles; i++) {
                        particles[i] = generateParticle();
                    }
                    return true;
                }
            }


            float rand = (float) Math.random();
            for (int i = 0; i < numParticles && count < numParticles; i++) {
                if (oldParticles[i].getWeight() == 0.f) continue;

                if (oldParticles[i].getWeight() >= rand) {
                    Pose p = oldParticles[i].getPose();
                    double x = p.getX();
                    double y = p.getY();
                    double angle = p.getHeading();

                    // Create a new instance of the particle and set its weight
                    particles[count] = new Particle(context, floorplan, new Pose(x, y, angle));
                    particles[count++].setWeight(1);
                }
            }
        }
        return false;
    }


    /**
     * Calculate the weight for each particle
     */
    public void calculateWeights(Bitmap floorplan) {
        for (int i = 0; i < numParticles; i++) {
            particles[i].calculateWeight(floorplan);
        }
    }

    /**
     * Apply a move to each particle
     *
     * @param move the move to apply
     */
    public void applyMove(Move move) {
        for (int i = 0; i < particles.length; i++) {
            particles[i].applyMove(move, distanceNoiseFactor, angleNoiseFactor);
        }
    }

    public Pose getAveragePose() {
        float x = 0;
        float y = 0;

        float xH = 0f, yH = 0f;

        for (Particle p : particles) {
            x += p.getPose().getX();
            y += p.getPose().getY();

            xH += Math.cos(Math.toRadians(p.getPose().getHeading()));
            yH += Math.sin(Math.toRadians(p.getPose().getHeading()));
        }

        return new Pose(x / particles.length, y / particles.length, Math.atan2(yH / particles.length, xH / particles.length));
    }


    public void updateParticles(Move move) {
        applyMove(move);
        calculateWeights(floorplan);
        resample();
    }


    public Particle[] getParticles() {
        return particles;
    }

}
