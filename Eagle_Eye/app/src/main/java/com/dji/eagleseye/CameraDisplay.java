package com.dji.eagleseye;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
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


public class CameraDisplay extends Activity implements SurfaceTextureListener {

    private static final String TAG = MainActivity.class.getName();

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected TextureView mVideoSurface = null;
    private Handler handler;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    //private TextView recordingTime;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_work);

        handler = new Handler();

        initUI();

        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        //recordingTime.setVisibility(View.INVISIBLE);
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
        if (mCodecManager == null) {
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

    private final void analysis(byte[] bytes, int width, int height) throws IOException {

        YuvImage yuvImage = new YuvImage(bytes, 17, width, height, (int[])null);
        ByteArrayOutputStream op = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 20, (OutputStream)op);
        Bitmap bitmap = BitmapFactory.decodeByteArray(op.toByteArray(), 0, op.size());
//        MlImage image = (new BitmapMlImageBuilder(bitmap)).build();
        op.close();




    }

    private final void enablePoseDetect() {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
        }

        if (mCodecManager != null) {
            mCodecManager.enabledYuvData(true);
        }

        if (mCodecManager != null) {
            mCodecManager.setYuvDataCallback((DJICodecManager.YuvDataCallback)(new DJICodecManager.YuvDataCallback() {
                public final void onYuvDataReceived(final MediaFormat format, ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
//                    updateTextureViewSize(width, height);
                    final byte[] bytes = new byte[dataSize];
                    yuvFrame.get(bytes);
                    AsyncTask.execute((Runnable)(new Runnable() {
                        public final void run() {
                            switch(format.getInteger("color-format")) {
                                case 19:
                                    try {
                                        newSaveYuvDataToJPEG420P(bytes, width, height);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                case 20:
                                default:
                                    break;
                                case 21:
                                    if (Build.VERSION.SDK_INT <= 23) {
                                        try {
                                            oldSaveYuvDataToJPEG(bytes, width, height);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        try {
                                            newSaveYuvDataToJPEG(bytes, width, height);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                            }

                        }
                    }));
                }
            }));
        }

    }

    private final void oldSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height) throws IOException {
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }

        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];

        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + 2 * i];
            u[i] = yuvFrame[y.length + 2 * i + 1];
        }
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }
        //nv21test
        byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = nv[i];
            bytes[y.length + (i * 2) + 1] = nu[i];
        }

            handle(bytes, width, height);

    }

    private final void handle(byte[] bytes, int width, int height) throws IOException {
        analysis(bytes, width, height);

    }

    private final void newSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height) throws IOException {
        if (yuvFrame.length >= width * height) {
            int length = width * height;
            byte[] u = new byte[width * height / 4];
            byte[] v = new byte[width * height / 4];

            for(int i = 0; i < u.length; i++) {
                v[i] = yuvFrame[length + 2 * i];
                u[i] = yuvFrame[length + 2 * i + 1];
            }

            for(int i = 0; i < u.length; i++) {
                yuvFrame[length + 2 * i] = u[i];
                yuvFrame[length + 2 * i + 1] = v[i];
            }

            handle(yuvFrame, width, height);
        }
    }

    private final void newSaveYuvDataToJPEG420P(byte[] yuvFrame, int width, int height) throws IOException {
        if (yuvFrame.length >= width * height) {
            int length = width * height;
            byte[] u = new byte[width * height / 4];
            byte[] v = new byte[width * height / 4];

            for(int i = 0; i < u.length; i++) {
                u[i] = yuvFrame[length + i];
                v[i] = yuvFrame[length + u.length + i];
            }

            for(int i = 0; i < u.length; i++) {
                yuvFrame[length + 2 * i] = v[i];
                yuvFrame[length + 2 * i + 1] = u[i];
            }

            handle(yuvFrame, width, height);
        }
    }
}
