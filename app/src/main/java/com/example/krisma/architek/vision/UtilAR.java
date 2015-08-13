package com.example.krisma.architek.vision;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

/**
 * Version 1.2
 */
public class UtilAR {

	//Camera tracking
	private static Mat intrinsicParams = new Mat(3,3,CvType.CV_64F);
	private static MatOfDouble distCoeffs = new MatOfDouble();
	private static Mat camRotation = new Mat(3,3,CvType.CV_32F);
	private static double[] tvecArray = new double[3];
	private static double[] rotArray = new double[9];
	private static double[] homLine = {0,0,0,1};

	/**
	 * Returns a camera matrix with default intrinsic parameters based on the given camera resolution.
	 */
	public static Mat getDefaultIntrinsicMatrix(int camResX,int camResY) {
		intrinsicParams.put(0,0, 670 * (float)camResX/640,0,camResX/2, 0,670 * (float)camResY/480,camResY/2, 0,0,1);
		return intrinsicParams;
	}

	/**
	 * Returns default distortion coefficients.
	 */
	public static MatOfDouble getDefaultDistortionCoefficients() {
		distCoeffs.put(0,0, -0.027, -0.0171, -0.0042, 0.0047, 0.063);
		return distCoeffs;
	}

	private static void composeMatrix(Mat rvec, Mat tvec, Mat target) {
		Calib3d.Rodrigues(rvec,camRotation);

		tvec.get(0,0, tvecArray);
		camRotation.get(0,0, rotArray);

		for(int row=0; row<3; ++row)
		{
		   for(int col=0; col<3; ++col)
		   {
			   target.put(row,col, rotArray[row*3+col]);
		   }
		   target.put(row,3, tvecArray[row]);
		}
		target.put(3,0, homLine);
	}
}
