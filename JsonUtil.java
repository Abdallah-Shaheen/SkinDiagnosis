package com.graduationproject.skindiagnosis;

import java.util.ArrayList;

import org.opencv.core.Mat;


import android.renderscript.Short2;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;

public class JsonUtil {

    public static Mat jsonToMat(String json) throws JSONException{
        JSONObject object = new JSONObject(new JSONTokener(json));
        int  rows = object.getInt("rows"),
                cols = object.getInt("cols"),
                type = object.getInt("type");
        String dataString = object.getString("data");

        byte[] data = Base64.decode(dataString, Base64.DEFAULT);
        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, data);
        return mat;
    }

    public static String matToJson(Mat mat) throws JSONException {
        JSONObject object = new JSONObject();
        int  rows = mat.rows(),
                cols = mat.cols(),
                type = mat.type(),
                size = (int) mat.elemSize();
        byte[] data = new byte[cols*rows*size];
        mat.get(0, 0, data);
        String dataString = Base64.encodeToString(data, Base64.DEFAULT);
        object.put("rows", rows);
        object.put("cols", cols);
        object.put("type", type);
        object.put("data", dataString);
        String json = object.toString();
        return json;
    }


    public static  Object matListToJSONArray(ArrayList<Mat> descriptors) throws JSONException {
        ArrayList<String> jsArray = new ArrayList<>();

        for (Mat mat : descriptors) {
            String json = matToJson(mat);
            jsArray.add(json);
        }


        return  jsArray;
    }

    public static ArrayList<Mat> jsonArrayToMatList(ArrayList<String> jsonArrayString) throws JSONException {

        ArrayList<Mat> descriptors = new ArrayList<>();;

        for(int i = 0 ; i < jsonArrayString.size() ; i++) {
            try {
                descriptors.add(jsonToMat(jsonArrayString.get(i)));
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }

        return descriptors;
    }

//    public static String readFile(String filename){
//        String contents = "";
//        BufferedReader reader = null;
//        try {
//            reader = new BufferedReader(new FileReader(filename));
//            for(String line = reader.readLine(); line != null; line = reader.readLine()){
//                contents += line;
//            }
//            reader.close();
//        } catch (FileNotFoundException e) {
//            Log.e("Fatal", "File " + filename + " was not found", e);
//        } catch (IOException e) {
//            Log.e("Fatal", "An error occured while reading or closing " + filename + ".", e);
//        }
//        return contents;
//    }
}
