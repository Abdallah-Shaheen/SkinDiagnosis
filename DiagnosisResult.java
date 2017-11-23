package com.graduationproject.skindiagnosis;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;


import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;



public class DiagnosisResult extends AppCompatActivity {

    File imgFile;
    String imagePath;

    ArrayList<Mat> descriptors = new ArrayList<Mat>();
    public static final String TRAINING_DESCRIPTORS_FILE_NAME = "descriptors.srl";

    FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
    DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);


    Double max_dist = 0.0;
    Double min_dist = 100.0;

    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosis_result);

        textView = (TextView) findViewById(R.id.resultText);

        Log.i("RECEIVE", "1 Successfully! at " + "activity started");
        Bundle extras = getIntent().getExtras();
        Log.i("RECEIVE", "1 11! at " + "activity started");

        imagePath = extras.getString("Image_Path");

        Log.i("RECEIVE", "2 Successfully! at " + imagePath);


        imgFile = new File(imagePath);
        Log.i("RECEIVE", "3 Successfully! at " + imagePath);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.resultImage);
            myImage.setImageBitmap(myBitmap);
        }


        try {
            diagnose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void diagnose() throws Exception {
        //first image
        Mat img1 = Imgcodecs.imread(imagePath);

        Mat descriptors1 = new Mat();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();

        detector.detect(img1, keypoints1);
        descriptor.compute(img1, keypoints1, descriptors1);


        try {
            ArrayList<String> jsonString = (ArrayList<String>) StorageUtility.read(this, TRAINING_DESCRIPTORS_FILE_NAME);

            Log.i("jsonString",jsonString.toString());

            descriptors = JsonUtil.jsonArrayToMatList(jsonString);


            if (descriptors != null) {
                int dis = matchDescriptors(descriptors1, descriptors);
                setReport(dis);
            }
            else {
                //TODO:- first remove TRAINING_DESCRIPTORS_FILE_NAME if exists
                computeDescriptors();

                try {

                    StorageUtility.write(JsonUtil.matListToJSONArray(descriptors) , this, TRAINING_DESCRIPTORS_FILE_NAME);
                } catch (IOException e) {
                    throw e;
                }
                int dis = matchDescriptors(descriptors1, descriptors);
                setReport(dis);

            }
        } catch (IOException e) {
            //TODO:- first remove TRAINING_DESCRIPTORS_FILE_NAME if exists
            computeDescriptors();

            try {
                StorageUtility.write(JsonUtil.matListToJSONArray(descriptors), this, TRAINING_DESCRIPTORS_FILE_NAME);

            } catch (IOException ex) {
                throw ex;
            }
            int dis = matchDescriptors(descriptors1, descriptors);
            setReport(dis);
        }


    }

    private void computeDescriptors() {
        String trainImagePath;
        Mat descriptors2 = new Mat();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        descriptors.clear();

        for (int i = 1; i <= 20; i++) {

            File dbPath = new File(Environment.getExternalStorageDirectory(), "trainingSet/im" + i + ".jpg");
            trainImagePath = dbPath.getPath();
            Log.i("IMAGE", "4" + trainImagePath);

            //second image
            Mat img2 = Imgcodecs.imread(trainImagePath);

            detector.detect(img2, keypoints2);
            descriptor.compute(img2, keypoints2, descriptors2);

            descriptors.add(descriptors2);
            Log.i("Type an cols:", "Type:" + descriptors.get(i-1).type() + "cols:" + descriptors.get(i-1).cols());

        }

        Log.i("COMPLETE:", "computeDescriptors");

    }


    private int matchDescriptors(Mat descriptors1, ArrayList<Mat> descriptors) {

        int disease = 100;
        MatOfDMatch matches = new MatOfDMatch();
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();

        int []descriptorsGoodMatches = new int[descriptors.size()];

        for (int i = 0; i < descriptors.size(); i++) {

            Log.i("STARTMATCHING:", "descriptors" + i);
            Log.i("Type an cols:","Type:" + descriptors1.type() + "cols:" + descriptors1.cols());
            Log.i("Type an cols:","Type:" + descriptors.get(i).type() + "cols:" + descriptors.get(i).cols());


            //matcher should include 2 different image's descriptors
            if (descriptors1.type() == descriptors.get(i).type() && descriptors1.cols() == descriptors.get(i).cols()) {
                matcher.match(descriptors1, descriptors.get(i), matches);


                List<DMatch> matchesList = matches.toList();



                for (int j = 0; j < matchesList.size(); j++) {
                    Double dist = (double) matchesList.get(j).distance;
                    Log.i("Distance and minimum","" + matchesList.get(j).distance + ",,," + min_dist);
                    if (dist < min_dist)
                        min_dist = dist;
                    if (dist > max_dist)
                        max_dist = dist;
                }

                for (int j = 0; j < matchesList.size(); j++) {
                    if (matchesList.get(j).distance <=(10 * min_dist))
                        good_matches.addLast(matchesList.get(j));
                }

                descriptorsGoodMatches[i] = good_matches.size();

                Log.i("img" + i, matches.size() + " " + good_matches.size());

            }
            else {
                Log.i("TypeAndCols" + i,":not match");
            }
        }


        int maxGoodMatches = descriptorsGoodMatches[1];

        for(int i = 0; i < descriptors.size(); i++) {
            if (descriptorsGoodMatches[i] >= maxGoodMatches) {
                disease = i;
                maxGoodMatches = descriptorsGoodMatches[i];
            }
        }

        if(maxGoodMatches >= 3 * min_dist){
            return disease;
        }
        else{
            return 1000;
        }
    }


    void setReport(int disease) {


        String report = null;

        String[] result = {"disease is eczema", "disease is measles", "disease is psoriasis",
                "disease is vitiligo", "Sorry :  cannot determine the result it is out of " +
                "rang, please visit a doctor  thank you."};

        String warning = "  <br> Please, Do not rely on this result, This is not final app version";

        if (disease <= 4) {
            report = "<font color=#0000>"+result[0]+"</font> <font color=#C90E14>"+warning+"</font>";
            textView.setText(Html.fromHtml(report));
        } else if (disease <= 9) {
            report = "<font color=#0000>"+result[1]+"</font> <font color=#C90E14>"+warning+"</font>";
            textView.setText(Html.fromHtml(report));
        } else if (disease <= 14) {
            report = "<font color=#0000>"+result[2]+"</font> <font color=#C90E14>"+warning+"</font>";
            textView.setText(Html.fromHtml(report));
        } else if (disease <= 19) {
            report = "<font color=#0000>"+result[3]+"</font> <font color=#C90E14>"+warning+"</font>";
            textView.setText(Html.fromHtml(report));
        } else {
            report = "<font color=#0000>"+result[4]+"</font> <font color=#C90E14>"+warning+"</font>";
            textView.setText(Html.fromHtml(report));
        }
    }


    static {
        System.loadLibrary("opencv_java3");
    }
}
