package com.graduationproject.skindiagnosis;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

//    private static final int SELECT_PICTURE = 1;
//    private String mCurrentPhotoPath;
    private ImageView imageView;

    static Uri mCapturedImageURI;


    private String selectedImagePath = null;
    final private int PICK_IMAGE = 1;
    final private int CAPTURE_IMAGE = 2;
    final private int CROP_IMAGE = 3;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


         imageView = (ImageView)findViewById(R.id.sampleImage);

        Button btnGallery = (Button)findViewById(R.id.galleryButton);
        Button btnCapture = (Button)findViewById(R.id.cameraButton);
        Button btnDiagnose = (Button)findViewById(R.id.diagnoseButton);

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, ""), PICK_IMAGE);

                Log.i("btnGallery", "clicked");
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "Image File name");
                mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                startActivityForResult(intent, CAPTURE_IMAGE);

                Log.i("btnCapture", "clicked");
            }
        });

        btnDiagnose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImagePath != null){
                    Intent intent = new Intent(MainActivity.this, DiagnosisResult.class);

                    intent.putExtra("Image_Path", selectedImagePath);
                    startActivity(intent);
                    Log.i("btnDiagnose", selectedImagePath);
                }
                else{
                    Log.i("IMAGENOTSELECTED", " image not selected");
                    Toast.makeText(getApplicationContext(),"No Valid Image Captured or Selected",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != AppCompatActivity.RESULT_CANCELED) {
            if (requestCode == PICK_IMAGE) {
                selectedImagePath = getAbsolutePath(data.getData());
                imageView.setImageBitmap(decodeFile(selectedImagePath));
                cropImage();
            } else if (requestCode == CAPTURE_IMAGE) {
                selectedImagePath = getAbsolutePath(mCapturedImageURI);
                imageView.setImageBitmap(decodeFile(selectedImagePath));
                cropImage();
            }
            else if (requestCode == CROP_IMAGE) {
                    //Create an instance of bundle and get the returned data
                    Bundle extras = data.getExtras();
                    //get the cropped bitmap from extras
                    Bitmap thePic = extras.getParcelable("data");

                try{
                    File file = new File(selectedImagePath);
                    FileOutputStream fOut = new FileOutputStream(file);
                    thePic.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                    fOut.flush();
                    fOut.close();
                    Log.i("SAVE", "Saved Successfully! at " + selectedImagePath);
                }
                catch (Exception e) {
                    Log.i("SAVE", "Save file error!");
                }

                //set image bitmap to image view
                    imageView.setImageBitmap(thePic);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    public void cropImage() {
        try {
				/*the user's device may not support cropping*/
            Log.i("cropImage",selectedImagePath);
            File file = new File(selectedImagePath);
            cropCapturedImage(Uri.fromFile(file));
        }
        catch(ActivityNotFoundException aNFE){
            //display an error message if user device doesn't support
            String errorMessage = "Sorry - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public Bitmap decodeFile(String path) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to scale to
            final int REQUIRED_SIZE = 70;
            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;
            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAbsolutePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }


    public Uri setImageUri() {
        // Store image in dcim
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "image" + new Date(System.currentTimeMillis()).getTime() + ".jpeg");
        Uri imgUri = Uri.fromFile(file);
        this.selectedImagePath = file.getAbsolutePath();
        Log.i("set AbsolutePath",this.selectedImagePath);
        return imgUri;
    }


    public void cropCapturedImage(Uri picUri){
        //call the standard crop action intent
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //indicate image type and Uri of image
        cropIntent.setDataAndType(picUri, "image/*");
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        //indicate output X and Y
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        //retrieve data on return
        cropIntent.putExtra("return-data", true);
        //start the activity - we handle returning in onActivityResult
        startActivityForResult(cropIntent, CROP_IMAGE);
    }
}
