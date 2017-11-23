package com.graduationproject.skindiagnosis;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * Created by WorkingRoom on 5/13/2016.
 */
public class StorageUtility{
    public static void write(Object object, Context context, String fileName) throws IOException {
        ObjectOutput out = null;

        try {
            out = new ObjectOutputStream(new FileOutputStream(new File(context.getFilesDir(),"")+File.separator+fileName));
            out.writeObject(object);
            out.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public static Object read(Context context,  String fileName) throws Exception {
        ObjectInputStream input;

        try {
            input = new ObjectInputStream(new FileInputStream(new File(new File(context.getFilesDir(),"")+ File.separator+fileName)));
            Object object =  input.readObject();
            input.close();

            return  object;
        } catch (IOException e) {
            throw e;
        }
    }
}
