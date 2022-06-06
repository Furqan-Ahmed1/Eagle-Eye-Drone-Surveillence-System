package com.dji.eagleseye;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.SurfaceTexture;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.thirdparty.afinal.core.AsyncTask;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;


public class CameraDisplay extends Activity implements SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();

    private objectDetectorClass detectorClass;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected TextureView mVideoSurface = null;
    private Button streamOn;
    private Button streamOff;

    private Handler handler;

    private String liveShowUrl = "";
    private EditText showUrlInputEdit;
    private static final String URL_KEY = "sp_stream_url";
    private LiveStreamManager.OnLiveChangeListener listener;
    private LiveStreamManager.LiveStreamVideoSource currentVideoSource = LiveStreamManager.LiveStreamVideoSource.Primary;

    private CameraApplication app;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    //private TextView recordingTime;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_work);

//        showToast("app context");
//        app.setContext(this);
//
//        showToast("showUrlInputEdit");
//        showUrlInputEdit.setText(liveShowUrl);
//
        showToast("handler");
        handler = new Handler();
//
////        try{
////            detectorClass = new objectDetectorClass(getAssets(),"ssd_mobilenet.tflite", "labelmap.txt", 300);
//////            detectorClass = new objectDetectorClass(getAssets(),"Gun_model.tflite","gun.txt",320);
////            showToast("Object Detection model is loaded");
////        }
////        catch (IOException e)
////        {
////            showToast("Object Detection model is not loaded");
////            e.printStackTrace();
////        }
//
        initUI();
        initListener();

        mReceivedVideoDataListener = (videoBuffer, size) -> {
            if (mCodecManager != null) {
                mCodecManager.sendDataToDecoder(videoBuffer, size);

            }
        };

    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);

        }
        streamOn = (Button) findViewById(R.id.streamBtnON);
        streamOn.setOnClickListener(this);
        streamOn.setVisibility(View.VISIBLE);
        streamOn.setEnabled(true);

        streamOff = (Button) findViewById(R.id.streamBtnOFF);
        streamOff.setOnClickListener(this);
        streamOff.setVisibility(View.INVISIBLE);
        streamOff.setEnabled(false);

        showUrlInputEdit = (EditText) findViewById(R.id.streamURL);
//        showUrlInputEdit.setText(liveShowUrl);


        //recordingTime.setVisibility(View.INVISIBLE);
    }

    private void initListener() {
        showUrlInputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                liveShowUrl = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        listener = new LiveStreamManager.OnLiveChangeListener() {
            @Override
            public void onStatusChanged(int i) {
                showToast("status changed : " + i);
            }
        };
    }

    protected void onProductChange() {
        initPreviewer();
        //loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initPreviewer() {

        BaseProduct product = CameraApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }

    }

    private void uninitPreviewer() {
        Camera camera = CameraApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(CameraDisplay.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        Camera camera = CameraApplication.getCameraInstance();
        if (mCodecManager == null && surface != null && camera != null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        final Bitmap image = mVideoSurface.getBitmap();
//        TextView textView = (TextView) findViewById(R.id.detection);
//        TextView scoretext = (TextView) findViewById(R.id.score);
//        textView.setText("Analysis");
//        try {
//            detectorClass.recognizeImage(image, textView, scoretext);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

//    protected fun updateTextureViewSize(width: Int, height: Int) {
//        requireActivity().runOnUiThread {
//            val textureView = getTextureView()
//            if (textureView.width != width) {
//                textureView.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                    this.width = width
//                    this.height = height
//                }
//            }
//        }
//    }

//    private long analysisInterval = 20L;
//    private long curTime = 0L;
//
//    private void analysis(byte[] bytes, int width, int height) throws IOException {
//
//        YuvImage yuvImage = new YuvImage(bytes, 17, width, height, (int[])null);
//        ByteArrayOutputStream op = new ByteArrayOutputStream();
//        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 20, (OutputStream)op);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(op.toByteArray(), 0, op.size());
////        MlImage image = (new BitmapMlImageBuilder(bitmap)).build();
//        op.close();
//
////        TextView textView = (TextView) findViewById(R.id.detection);
////        textView.setText("Analysis");
////        if (analysisInterval < System.currentTimeMillis() - curTime) {
////            curTime = System.currentTimeMillis();
////            try {
////                detectorClass.recognizeImage(bitmap, textView);
////            }
////            catch (IOException e){
////                String exp = e.toString();
////                showToast(exp);
////                e.printStackTrace();
////            }
////        }
//
//
//    }
//
//    private void enablePoseDetect() {
//        if (mCodecManager != null) {
//            mCodecManager.cleanSurface();
//            showToast("mCodecmanager is not null");
//        }
//        else {
//            showToast("mCodecmanager is null");
//        }
//
//        if (mCodecManager != null) {
//            mCodecManager.enabledYuvData(true);
//        }
//
//        if (mCodecManager != null) {
//            mCodecManager.setYuvDataCallback((DJICodecManager.YuvDataCallback)(new DJICodecManager.YuvDataCallback() {
//                public final void onYuvDataReceived(final MediaFormat format, ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
////                    updateTextureViewSize(width, height);
//                    final byte[] bytes = new byte[dataSize];
//                    yuvFrame.get(bytes);
//                    AsyncTask.execute((Runnable)(new Runnable() {
//                        @Override
//                        public void run() {
//                            // two samples here, it may has other color format.
//                            int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
//                            switch (colorFormat) {
//                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
//                                    //NV12
//                                    if (Build.VERSION.SDK_INT <= 23) {
//                                        try {
//
//                                            oldSaveYuvDataToJPEG(bytes, width, height);
//                                        }
//                                        catch (IOException e)
//                                        {
//                                            e.printStackTrace();
//                                        }
//                                    } else {
//                                        try {
//                                            newSaveYuvDataToJPEG(bytes, width, height);
//                                        }
//                                        catch (IOException e)
//                                        {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                    break;
//                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
//                                    //YUV420P
//                                    try {
//                                        newSaveYuvDataToJPEG420P(bytes, width, height);
//                                    }
//                                    catch (IOException e)
//                                    {
//                                        e.printStackTrace();
//                                    }
//                                    break;
//                                default:
//                                    break;
//                            }
//
//                        }
//                    }));
//                }
//            }));
//        }
//
//    }
//
//    private void oldSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height) throws IOException {
//        if (yuvFrame.length < width * height) {
//            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
//            return;
//        }
//
//        byte[] y = new byte[width * height];
//        byte[] u = new byte[width * height / 4];
//        byte[] v = new byte[width * height / 4];
//        byte[] nu = new byte[width * height / 4]; //
//        byte[] nv = new byte[width * height / 4];
//
//        System.arraycopy(yuvFrame, 0, y, 0, y.length);
//        for (int i = 0; i < u.length; i++) {
//            v[i] = yuvFrame[y.length + 2 * i];
//            u[i] = yuvFrame[y.length + 2 * i + 1];
//        }
//        int uvWidth = width / 2;
//        int uvHeight = height / 2;
//        for (int j = 0; j < uvWidth / 2; j++) {
//            for (int i = 0; i < uvHeight / 2; i++) {
//                byte uSample1 = u[i * uvWidth + j];
//                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
//                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
//                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
//                nu[2 * (i * uvWidth + j)] = uSample1;
//                nu[2 * (i * uvWidth + j) + 1] = uSample1;
//                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
//                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
//                nv[2 * (i * uvWidth + j)] = vSample1;
//                nv[2 * (i * uvWidth + j) + 1] = vSample1;
//                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
//                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
//            }
//        }
//        //nv21test
//        byte[] bytes = new byte[yuvFrame.length];
//        System.arraycopy(y, 0, bytes, 0, y.length);
//        for (int i = 0; i < u.length; i++) {
//            bytes[y.length + (i * 2)] = nv[i];
//            bytes[y.length + (i * 2) + 1] = nu[i];
//        }
//
//            handle(bytes, width, height);
//
//    }
//
//    private void handle(byte[] bytes, int width, int height) throws IOException {
//        analysis(bytes, width, height);
//
//    }
//
//    private void newSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height) throws IOException {
//        if (yuvFrame.length >= width * height) {
//            int length = width * height;
//            byte[] u = new byte[width * height / 4];
//            byte[] v = new byte[width * height / 4];
//
//            for(int i = 0; i < u.length; i++) {
//                v[i] = yuvFrame[length + 2 * i];
//                u[i] = yuvFrame[length + 2 * i + 1];
//            }
//
//            for(int i = 0; i < u.length; i++) {
//                yuvFrame[length + 2 * i] = u[i];
//                yuvFrame[length + 2 * i + 1] = v[i];
//            }
//
//            handle(yuvFrame, width, height);
//        }
//    }
//
//    private void newSaveYuvDataToJPEG420P(byte[] yuvFrame, int width, int height) throws IOException {
//        if (yuvFrame.length >= width * height) {
//            int length = width * height;
//            byte[] u = new byte[width * height / 4];
//            byte[] v = new byte[width * height / 4];
//
//            for(int i = 0; i < u.length; i++) {
//                u[i] = yuvFrame[length + i];
//                v[i] = yuvFrame[length + u.length + i];
//            }
//
//            for(int i = 0; i < u.length; i++) {
//                yuvFrame[length + 2 * i] = v[i];
//                yuvFrame[length + 2 * i + 1] = u[i];
//            }
//
//            handle(yuvFrame, width, height);
//        }
//    }

    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            showToast("No live stream manager!");
            return false;
        }
        return true;
    }

    void startLiveShow() {
        showToast("Start Live Show");
        if (!isLiveStreamManagerOn()) {
            return;
        }
        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
            showToast("already started!");
            return;
        }
        new Thread() {
            @Override
            public void run() {
                
                DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
                int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
                getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(),
                        Context.MODE_PRIVATE).edit().putString(URL_KEY,liveShowUrl).commit();

                showToast("startLive:" + result +
                        "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                        "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled());
            }
        }.start();
    }

    private void stopLiveShow() {
        if (!isLiveStreamManagerOn()) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        showToast("Stop Live Show");
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.streamBtnON:
                startLiveShow();
                streamOn.setVisibility(View.INVISIBLE);
                streamOn.setEnabled(false);
                streamOff.setVisibility(View.VISIBLE);
                streamOff.setEnabled(true);
                break;
            case R.id.streamBtnOFF:
                stopLiveShow();
                streamOff.setVisibility(View.INVISIBLE);
                streamOff.setEnabled(false);
                streamOn.setVisibility(View.VISIBLE);
                streamOn.setEnabled(true);
                break;
        }
    }
}
