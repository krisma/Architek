package com.example.krisma.architek.vision;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.line;

/**
 * Created by smp on 13/07/15.
 */
public class FloorplanProcessor {

    private MatOfPoint2f objectPoints2d;
    private Mat cameraMatrix;
    private MatOfDouble distortionMat;
    private MatOfPoint3f objectPoints;
    private int imageWidth = 0, imageHeight = 0;


    public FloorplanProcessor(){
    }

    public void process(){

        //Mat sceneMat = imread(new File("Input/Floorplans/Wheeler/wheeler-test.png").getAbsolutePath());

        //Mat objectMat = imread(new File("data/extinguisher.png").getAbsolutePath());

        //detect(sceneMat, objectMat);
    }

    private void canny(Mat scene){
        //mat gray image holder
        Mat imageGray = new Mat();

        //mat canny image
        Mat imageCny = new Mat();

        //Convert the image in to gray image
        Imgproc.cvtColor(scene, imageGray, Imgproc.COLOR_BGR2GRAY);

        //Show the gray image
        //UtilAR.imShow("Gray Image", imageGray);

        //Canny Edge Detection
        Imgproc.Canny(imageGray, imageCny, 10, 100, 3, true);

        //UtilAR.imShow("Canny Image", imageCny);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imageCny, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat mask = new Mat( new Size( imageCny.cols(), imageCny.rows() ), CvType.CV_8UC1 );
        mask.setTo( new Scalar( 0.0 ) );

        for(MatOfPoint con : contours){
            List<MatOfPoint> tmp = new ArrayList<>();
            tmp.add(con);
            RotatedRect rect = getRect(con);

            if(con.height() > 40){


                Imgproc.drawContours(mask, tmp, -1, new Scalar(0, 0, 0), -1);
            }

        }
        Core.bitwise_and(imageCny, mask, imageCny);
        //UtilAR.imShow("With Mask", imageCny);

    }



    /***
     * This method first creates a grayscale image from the webcam input. Then it transforms it into a black/white binary image.
     * @param camImage Webcam input
     * @param t is the lower treshold (values between 30-115 are usually good)
     * @return
     */
    private Mat toBinaryMat(Mat camImage, int t) {
        // Grayscale matrix
        Mat grayScaleMat = new Mat(imageWidth, imageHeight, CvType.CV_8U);
        Imgproc.cvtColor(camImage, grayScaleMat, Imgproc.COLOR_RGB2GRAY);

        //Binary Matrix
        Mat binaryMat = new Mat(grayScaleMat.size(), grayScaleMat.type());

        // Tresholding
        Imgproc.threshold(grayScaleMat, binaryMat, t, 255, Imgproc.THRESH_BINARY);
        return binaryMat;
    }

    public Mat detectEdges(Mat sc){
        Mat scene = sc;

        imageWidth = scene.width();
        imageHeight = scene.height();

        // For Image Processing
        objectPoints = new MatOfPoint3f();
        objectPoints.alloc(4);
        objectPoints.put(0, 0, 0, 0, 0);
        objectPoints.put(1, 0, 1, 0, 0);
        objectPoints.put(2,0,1,0,1);
        objectPoints.put(3, 0, 0, 0, 1);

        // For Homography
        objectPoints2d = new MatOfPoint2f();
        objectPoints2d.alloc(4);
        objectPoints2d.put(0,0,0,0);
        objectPoints2d.put(1,0,1*imageWidth,0);
        objectPoints2d.put(2,0,1* imageWidth, 1 * imageWidth);
        objectPoints2d.put(3,0,0,1*imageWidth);

        cameraMatrix = UtilAR.getDefaultIntrinsicMatrix(imageWidth, imageHeight);
        distortionMat = UtilAR.getDefaultDistortionCoefficients();

        Mat binaryMat = toBinaryMat(scene, 40); // 120
        // UtilAR.imShow("Binary", binaryMat);

        // find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // evaluate all contours
        if (!contours.isEmpty()) {

            Collections.sort(contours, new CustomComparator());

            for(MatOfPoint con : contours){
                List<MatOfPoint> tmp = new ArrayList<>();
                tmp.add(con);
                RotatedRect rect = getRect(con);


                Point r1 = new Point(scene.width()/2, scene.height()/2);
                double distance = Math.sqrt(Math.pow(r1.x - rect.center.x, 2) -  Math.pow(r1.y - rect.center.y, 2));
                System.out.println(distance);
                if(con.height() > 40){
                    Imgproc.drawContours(scene, tmp, -1, new Scalar(0, 0, 0), 6);

                }

            }

            imwrite("demo2_edged.png", scene);


            RotatedRect maxRect = getRect(contours.get(1));

            //Core.circle(scene,new Point(maxRect.center.x, maxRect.center.y), 50,  new Scalar(255, 0, 0));
            //Core.circle(scene,new Point(maxRect.br().x, maxRect.br().y), 50,  new Scalar(255, 0, 0));

            //UtilAR.imShow("Scene", scene);



            processImage(contours.get(1), scene);
        }

        return scene;
    }

    private RotatedRect getRect(MatOfPoint mat){
        MatOfPoint2f approxPolyInputMat = new MatOfPoint2f(mat.toArray());
        MatOfPoint2f approxPolyOutputMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(approxPolyInputMat, approxPolyOutputMat, 8, true);

        return Imgproc.minAreaRect(approxPolyOutputMat);
    }

    private MatOfPoint2f getCorners(MatOfPoint2f mat){
        double[] p1 = mat.get(0,0);
        double[] p2 = mat.get(1,0);
        double[] p3 = mat.get(2,0);
        double[] p4 = mat.get(3,0);

        MatOfPoint2f corners = new MatOfPoint2f();
        corners.alloc(4);
        corners.put(0, 0, p1[0], p1[1]);
        corners.put(1, 0, p2[0], p2[1]);
        corners.put(2, 0, p3[0], p3[1]);
        corners.put(3, 0, p4[0], p4[1]);

        return corners;
    }

    public class CustomComparator implements Comparator<MatOfPoint> {
        @Override
        public int compare(MatOfPoint o1, MatOfPoint o2) {
            RotatedRect r1 = getRect(o1);
            RotatedRect r2 = getRect(o2);

            return (int) (r2.boundingRect().area() - r1.boundingRect().area());
        }
    }

    private void processImage(MatOfPoint matOfPoint, Mat sceneMat) {
        // Create approximate polygons
        MatOfPoint2f approxPolyInputMat = new MatOfPoint2f(matOfPoint.toArray());
        MatOfPoint2f approxPolyOutputMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(approxPolyInputMat, approxPolyOutputMat, 8, true);


        // used for homography

        if (approxPolyOutputMat.height() == 4) {
            MatOfPoint2f corners = getCorners(approxPolyOutputMat);


            // Extrinsics needed for 3D rendering
            Mat rotVec = new Mat();
            Mat transVec = new Mat();

            // Solve for rotation and translation
            boolean success = Calib3d.solvePnP(objectPoints, corners, cameraMatrix, distortionMat, rotVec, transVec);

            if (success) {
                // Determine the Homography to better count the corners of the F
                Mat H = Calib3d.findHomography(corners, objectPoints2d);

                // De-Warp the image
                Mat warpOutput = new Mat();
                Imgproc.warpPerspective(sceneMat, warpOutput, H, new Size(imageWidth, imageWidth));
                Mat warpOutputInverted = new Mat();

                // Flip the de-warped image since it was mirrored
                Core.flip(warpOutput, warpOutputInverted, 1);

                // Image is flipped and ready for detection


                // find inner contours -- create new binary image of the de-warped image
                Mat inner_binaryMat = toBinaryMat(warpOutputInverted, 80); // 80

                // Find the inner contours (the F)
                List<MatOfPoint> inner_contours = new ArrayList<>();
                Mat inner_hierarchy = new Mat();
                Imgproc.findContours(inner_binaryMat, inner_contours, inner_hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                Imgproc.drawContours(warpOutputInverted, inner_contours, -1, new Scalar(255, 0, 0), 4);

            }
        }
    }


    public void detect(Mat sceneMat, Mat objectMat){
        // Detect Features
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SURF);

        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene  = new MatOfKeyPoint();

        detector.detect(objectMat, keypoints_object);
        detector.detect(sceneMat, keypoints_scene);

        // Extract Descriptors
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SURF);

        Mat descriptor_object = new Mat();
        Mat descriptor_scene = new Mat() ;

        extractor.compute(objectMat, keypoints_object, descriptor_object);
        extractor.compute(sceneMat, keypoints_scene, descriptor_scene);

        // Match Descriptors across Images
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

        List<MatOfDMatch> matches = new ArrayList<>();

        if(!matches.isEmpty()) {

            matcher.knnMatch(descriptor_object, descriptor_scene, matches, 2);

            // Filter Matches
            List<DMatch> goodMatches = new ArrayList<>();
            float nndrRatio = 0.70f;

            for (int i = 0; i < matches.size(); ++i) {
                if (matches.get(i).height() < 2)
                    continue;

                DMatch m1 = matches.get(i).toArray()[0];
                DMatch m2 = matches.get(i).toArray()[1];

                if (m1.distance <= nndrRatio * m2.distance) {
                    goodMatches.add(m1);
                }
            }

            // Extract the coordinates of the good matches
            List<Point> obj = new ArrayList<>();
            List<Point> scene = new ArrayList<>();

            List<KeyPoint> keypointsO = keypoints_object.toList();
            List<KeyPoint> keypointsS = keypoints_scene.toList();

            for (DMatch dm : goodMatches) {
                obj.add(keypointsO.get(keypointsO.indexOf(dm.queryIdx)).pt);
                scene.add(keypointsS.get(keypointsS.indexOf(dm.queryIdx)).pt);
            }

            MatOfPoint2f objPointMat = new MatOfPoint2f();
            objPointMat.fromList(obj);

            MatOfPoint2f scenePointMat = new MatOfPoint2f();
            scenePointMat.fromList(scene);

            Mat H = Calib3d.findHomography(objPointMat, scenePointMat);

            // Get corners from the image (the object to be detected)
            List<Point> objCorners = new ArrayList<>();
            objCorners.add(new Point(0, 0));
            objCorners.add(new Point(objectPoints.cols(), 0));
            objCorners.add(new Point(objectPoints.cols(), objectPoints.rows()));
            objCorners.add(new Point(0, objectPoints.rows()));

            MatOfPoint2f objCornersMat = new MatOfPoint2f();
            objCornersMat.fromList(objCorners);

            MatOfPoint2f sceneCornersMat = new MatOfPoint2f();

            Core.perspectiveTransform(objCornersMat, sceneCornersMat, H);

            line(sceneMat, new Point(sceneCornersMat.get(0, 0)), new Point(sceneCornersMat.get(1, 0)), new Scalar(125, 125, 125), 2);
            line(sceneMat, new Point(sceneCornersMat.get(1, 0)), new Point(sceneCornersMat.get(2, 0)), new Scalar(125, 125, 125), 2);
            line(sceneMat, new Point(sceneCornersMat.get(2, 0)), new Point(sceneCornersMat.get(3, 0)), new Scalar(125, 125, 125), 2);
            line(sceneMat, new Point(sceneCornersMat.get(3, 0)), new Point(sceneCornersMat.get(4, 0)), new Scalar(125, 125, 125), 2);
        } else {
            System.out.println("No matches");
        }
    }
}
