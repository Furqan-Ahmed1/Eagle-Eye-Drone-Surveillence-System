package com.dji.eagleseye;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dji.thirdparty.io.reactivex.internal.functions.ObjectHelper;

public class objectDetectorClass {

    private Interpreter interpreter;
    private List<String> labelList;
    private int INPUT_SIZE;
    private int PIXEL_SIZE =3; //for rgb
    private int IMAGE_MEAN = 0;
    private float IMAGE_STD = 255.0f;

    private GpuDelegate gpuDelegate;
    private int height = 0;
    private int width = 0;

    objectDetectorClass(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException {
        INPUT_SIZE = inputSize;

        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);

        //loading model
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath),options);

        //loading Loadmap
        labelList = loadLabelList(assetManager,labelPath);

    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException{

        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;

        while((line = reader.readLine()) != null)
        {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException{

        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long StartOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,StartOffset,declaredLength);
    }

    public void recognizeImage(Bitmap bitmap,TextView textView, TextView scoreText) throws IOException
    {
        height = bitmap.getHeight();
        width = bitmap.getWidth();

//        textView.setText("scaledBitmap");
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);

//        textView.setText("byteBuffer");
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap,0);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        //treemap of three array (boxes,score,classes)
        Map<Integer,Object> output_map = new TreeMap<>();

        //top 10 detected objects
        // 4: there coordinate in image
        float[][][] boxes = new float[1][10][4];

        float[][] classes = new float[1][10];

        float[][] scores = new float[1][10];

        output_map.put(0, boxes);
//        textView.setText("boxes");
        output_map.put(1, classes);
//        textView.setText("classes");
        output_map.put(2, scores);
//        textView.setText("scores");

        interpreter.runForMultipleInputsOutputs(input, output_map);
//        textView.setText("interpreter");

        Object value = output_map.get(0);
        Object Object_class = output_map.get(1);
        Object score = output_map.get(2);

//        textView.setText("score_value");
        float class_value = (float) Array.get(Array.get(Object_class, 0), 0);
        float score_value = (float) Array.get(Array.get(score, 0), 0);

        scoreText.setText(Float.toString(score_value));
        if (score_value > 0.5) {
////            Object box1 = Array.get(Array.get(value,0),0);
            String object_name = labelList.get((int) class_value);
//
            textView.setText(object_name);
        }
        //textView.setText("end");


//        for(int i = 0;i<10;i++)
//        {
//            float class_value = (float) Array.get(Array.get(Object_class,0),i);
//            float score_value = (float) Array.get(Array.get(score,0),i);
//
//            textView.setText(Float.toString(score_value));
//            if(score_value > 0.5)
//            {

//                Object box1 = Array.get(Array.get(value,0),i);
//
//                float top = (float) Array.get(box1,0) * height;
//                float left = (float) Array.get(box1,1) * width;
//                float bottom = (float) Array.get(box1,2) * height;
//                float right = (float) Array.get(box1,3) * width;

//                String object_name = labelList.get((int)class_value);

//                textView.setText(object_name);

//            }
//
//        }

    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap,int quant) {

        ByteBuffer byteBuffer;


        int size_image = INPUT_SIZE;
        if(quant == 0)
        {
            byteBuffer = ByteBuffer.allocateDirect(size_image * size_image * 3);
        }
        else{
            byteBuffer = ByteBuffer.allocateDirect(4 * size_image * size_image * 3);
        }

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_image*size_image];
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        int pixel=0;

        for(int i =0; i<size_image; ++i)
        {
            for(int j = 0;j<size_image; ++j)
            {
                final int val = intValues[pixel++];
                if(quant == 0)
                {
                    byteBuffer.put((byte) ((val>>16)&0xFF));
                    byteBuffer.put((byte) ((val>>8)&0xFF));
                    byteBuffer.put((byte) (val&0xFF));
                }
                else{
                    byteBuffer.putFloat(((val>>16) & 0xFF)/255.0F);
                    byteBuffer.putFloat(((val>>8) & 0xFF)/255.0F);
                    byteBuffer.putFloat(( val & 0xFF)/255.0F);
                }
            }
        }
        return byteBuffer;
    }
}
