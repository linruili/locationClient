package com.example.administrator.locationclient.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.administrator.locationclient.R;
import com.example.administrator.locationclient.utils.SensorUtils;
import com.example.administrator.locationclient.utils.TCPUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/12/14.
 */

public class CameraActivity extends AppCompatActivity
{
    private static final int CLIENT_PORT = 9132;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    private static final int TCP_CONNECT = 0;
    private static final int TCP_SEND = 1;
    private static final int TCP_DISCONNECT = 2;
    private SensorUtils sensorUtils;
    private TCPUtils tcpUtils;
    private double compass;
    private int frame_count = 0;

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
        {
            Log.d("MainActivity", "width: " + width);
            Log.d("MainActivity", "height: " + height);
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface)
        {
            if(mIsRecording)
            {
                frame_count += 1;
                Log.d("MainActivity", "onSurfaceTextureUpdated  " + frame_count);

//                Bitmap bmp = Bitmap.createBitmap(mTextureView.getWidth(), mTextureView.getHeight(), Bitmap.Config.ARGB_8888);
                Bitmap bmp = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
                mTextureView.getBitmap(bmp);
                compass = sensorUtils.getCompassDirection();
                Log.d("MainActivity", "value[0] = " + compass);

//                try
//                {
//                    Thread.sleep(300);
//                } catch (InterruptedException e)
//                {
//                    e.printStackTrace();
//                }

                mBackgroundHandler.post(new BitmapTransfer(TCP_SEND, bmp, compass));
            }
        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera)
        {
            mCameraDevice = camera;
            mMediaRecorder = new MediaRecorder();
            if(mIsRecording) {
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaRecorder.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChronometer.setBase(SystemClock.elapsedRealtime());
                        mChronometer.setVisibility(View.VISIBLE);
                        mChronometer.start();
                    }
                });
            } else {
                startPreview();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera)
        {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error)
        {
            camera.close();
            mCameraDevice = null;
        }
    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private String mCameraId;
    private Size mPreviewSize;
    private Size mVideoSize;
    private Size mImageSize;
    private class BitmapTransfer implements Runnable
    {
        private Bitmap mBitmap;
        private double mCompass;
        private int mState;
        public BitmapTransfer(int state)
        {
            mState = state;
        }
        public BitmapTransfer(int state, Bitmap bitmap, double compass)
        {
            mState = state;
            mBitmap = bitmap;
            mCompass = compass;
        }

        @Override
        public void run()
        {
            switch (mState)
            {
                case TCP_CONNECT:
                    tcpUtils = new TCPUtils(CLIENT_PORT);
                    break;

                case TCP_DISCONNECT:
                    tcpUtils.send_fin();
                    String result_string = tcpUtils.rec_result();
                    tcpUtils.disconnect();
                    Intent intent = new Intent(CameraActivity.this, LocalizationActivity.class);
                    intent.putExtra("data", result_string);
                    startActivity(intent);
                    break;

                case TCP_SEND:
                    try {
                        createImageFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    FileOutputStream fileOutputStream = null;
                    try {
                        tcpUtils.send_image_compass(mBitmap, mCompass);
                        Log.d("MainActivity", "saving bitmap");
//                        fileOutputStream = new FileOutputStream(mImageFileName);
//                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fileOutputStream);
//                        Thread.sleep(10000);
//                    } catch (IOException e) {
//                        Log.d("MainActivity", "save image error");
//                        e.printStackTrace();
                    } finally {
                        if(mBitmap != null)
                            mBitmap.recycle();
                        if(fileOutputStream != null)
                        {
                            try{
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                default:
            }

        }
    }

    private MediaRecorder mMediaRecorder;
    private Chronometer mChronometer;
    private int mTotalRotation;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
                {
                    super.onCaptureCompleted(session, request, result);
                }
            };
    private CameraCaptureSession mRecordCaptureSession;

    private ImageButton mRecordImageButton;
    private boolean mIsRecording = false;
    private File mVideoFolder;
    private String mVideoFileName;
    private File mImageFolder;
    private String mImageFileName;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        createVideoFolder();
        createImageFolder();
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mRecordImageButton = (ImageButton) findViewById(R.id.videoOnlineImageButton);
        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording)
                {
                    mChronometer.stop();
                    mChronometer.setVisibility(View.INVISIBLE);
                    mIsRecording = false;
                    mRecordImageButton.setImageResource(R.mipmap.btn_video_online);
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    startPreview();
                    mBackgroundHandler.post(new BitmapTransfer(TCP_DISCONNECT));
                } else
                {
                    mBackgroundHandler.post(new BitmapTransfer(TCP_CONNECT));
                    checkWriteStoragePermission();
                }

            }
        });
        sensorUtils = SensorUtils.getInstance(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        startBackgroundThread();
        sensorUtils.registSensor();

        if(mTextureView.isAvailable())
        {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT)
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(getApplicationContext(),
                        "Application will not run without camera services", Toast.LENGTH_SHORT).show();

        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                mIsRecording = true;
                mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                Toast.makeText(this, "Permission successfully granted!", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(this, "App needs to save video to run", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause()
    {
        closeCamera();
        stopBackgroundThread();
        sensorUtils.unregistSensor();

        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas)
    {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas)
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void setupCamera(int width, int height)
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation)
                {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera()
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED)
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                else
                {
                    if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA))
                        Toast.makeText(this,  "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    requestPermissions(new String[] {android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void checkWriteStoragePermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
            {
                mIsRecording = true;
                mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaRecorder.start();
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.setVisibility(View.VISIBLE);
                mChronometer.start();
            } else
            {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    Toast.makeText(this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else
        {
            mIsRecording = true;
            mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
            try {
                createVideoFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            startRecord();
            mMediaRecorder.start();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.setVisibility(View.VISIBLE);
            mChronometer.start();
        }
    }

    private void startRecord()
    {
        try {
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);//CaptureResult通过addTarget(Surface)方法附着到对应的Surface上显示
            mCaptureRequestBuilder.addTarget(recordSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session)
                        {
                            mRecordCaptureSession = session;
                            try {
                                //---------------------------------------------------------------------
//                                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//                                        CaptureRequest.CONTROL_AE_MODE_OFF);

                                //---------------------------------------------------------------------
                                mRecordCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {}
                    }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPreview()
    {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session)
                        {
                            mPreviewCaptureSession = session;
                            try {
                                //--------------------------------------
//                                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//                                        CaptureRequest.CONTROL_AE_MODE_OFF);
                                //--------------------------------------
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session)
                        {
                            Toast.makeText(getApplicationContext(),
                                    "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera()
    {
        if(mCameraDevice != null)
        {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mMediaRecorder != null)
        {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void startBackgroundThread()
    {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread()
    {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation)
    {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height)
    {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices)
        {
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0)
            return Collections.min(bigEnough, new CompareSizeByArea());
        else
            return choices[0];
    }

    private void createVideoFolder()
    {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mVideoFolder = new File(movieFile, "camera2VideoImage");
        if(!mVideoFolder.exists())
            mVideoFolder.mkdirs();
    }

    private File createVideoFileName() throws IOException
    {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    private void createImageFolder()
    {
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder = new File(imageFile, "camera2VideoImage");
        if(!mImageFolder.exists())
            mImageFolder.mkdirs();
    }

    private File createImageFileName() throws IOException
    {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void setupMediaRecorder() throws IOException
    {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }



}
