package com.example.krisma.architek.aws;

import android.content.Context;
import android.graphics.Bitmap;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.gms.maps.model.LatLng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by smp on 14/08/15.
 */
public class AWSClient {

    private static final String RAW_FLOORPLAN_BUCKET_KEY = "unprocessedfloorplans";

    private static final Logger log = LoggerFactory.getLogger(AWSClient.class);

    private final Context context;
    private final TransferUtility transferUtility;
    private CognitoCachingCredentialsProvider credentialsProvider;

    public AWSClient(Context context){
        this.context = context;

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                "eu-west-1:eb639395-cace-4218-bf1e-1e2b91d7c9e7", // Identity Pool ID
                Regions.EU_WEST_1 // Region
        );

        // Create an S3 client
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);

        // Set the region of your S3 bucket
        s3.setRegion(Region.getRegion(Regions.EU_WEST_1));

        transferUtility = new TransferUtility(s3, context);

    }



    public void uploadRawFloorplan(LatLng location, int floor, Bitmap floorplan){

        final String OBJECT_KEY = location.latitude + ";" + location.longitude + ";" + floor + ".PNG";
        FileOutputStream outStream;
        try {
            File imageFile = new File(context.getCacheDir().getPath() + UUID.randomUUID());
            outStream = new FileOutputStream(imageFile);

            floorplan.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

            TransferObserver observer = transferUtility.upload(
                    RAW_FLOORPLAN_BUCKET_KEY,   /* The bucket to upload to */
                    OBJECT_KEY,                 /* The key for the uploaded object */
                    imageFile                   /* The file where the data to upload exists */
            );
            observer.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    switch (state){
                        case COMPLETED:
                            log.info("Completed Upload.");
                            break;
                        case FAILED:
                            log.warn("Upload Failed!");
                            break;
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                }

                @Override
                public void onError(int id, Exception ex) {
                    log.warn(ex.getMessage());
                }
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
