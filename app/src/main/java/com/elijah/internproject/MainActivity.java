package com.elijah.internproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.elijah.internproject.databinding.ActivityMainBinding;
import com.elijah.internproject.libs.fftpack.ComplexDoubleFFT;
import com.elijah.internproject.utils.AudioDeviceManager;
import com.elijah.internproject.utils.AudioRecordController;
import com.elijah.internproject.utils.CameraDeviceManager;
import com.elijah.internproject.utils.CameraSizes;
import com.elijah.internproject.utils.FFTControl;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

public class MainActivity extends AppCompatActivity {
    public final String APP_TAG = "CAMERA ACTIVITY";
    private final String KEY_CURRENT_CAMERA_OF_DEVICE = "KEY_CURRENT_CAMERA_OF_DEVICE";
    private ActivityMainBinding activityMainBinding;
    private CameraService[] deviceCameras = null;
    private CameraManager cameraManager = null;
    private String currentIdDeviceCamera = null;
    private HandlerThread handlerThread;
    private Handler handler;
    private AudioRecordController audioRecordController;
    private LineGraphSeries<DataPoint> pointAmplitudeFrequencyLineGraphSeries = null;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        super.onCreate(savedInstanceState);
        initialiseCamera(savedInstanceState);
        initialiseAudioRecording();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        startBackgroundThread();
        if (audioRecordController != null) {
            startAudioRecording();
        }
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialiseCamera(Bundle savedInstanceState) {
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (savedInstanceState == null) {
            currentIdDeviceCamera = CameraDeviceManager.getInitialBackCameraId(cameraManager);
        } else {
            currentIdDeviceCamera = savedInstanceState.getString(KEY_CURRENT_CAMERA_OF_DEVICE);
        }
        activityMainBinding.imageCameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    deviceCameras = new CameraService[CameraDeviceManager.filterCompatibleCameras(cameraManager,
                            cameraManager.getCameraIdList()).size()];
                    deviceCameras[Integer.parseInt(currentIdDeviceCamera)] = new CameraService(currentIdDeviceCamera, holder.getSurface(),
                            handler, cameraManager);
                    setPreviewSize(currentIdDeviceCamera);
                    activityMainBinding.imageCameraSurfaceView.getRootView().post(() -> openCamera(Integer.parseInt(currentIdDeviceCamera)));
                    initialiseSwitchCamera(holder.getSurface());
                } catch (Exception e) {
                    Log.e(APP_TAG, String.format("Create surface! from cameraId %s error %s", currentIdDeviceCamera, e.getMessage()));
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialiseSwitchCamera(Surface surface) {
        activityMainBinding.switchCameraFrontBack.setOnClickListener(l -> {
            String nextIdDeviceCamera = CameraDeviceManager.getNextCameraId(cameraManager, currentIdDeviceCamera);
            if (deviceCameras[Integer.parseInt(nextIdDeviceCamera)] == null) {
                deviceCameras[Integer.parseInt(nextIdDeviceCamera)] = new CameraService(nextIdDeviceCamera, surface, handler, cameraManager);
            }
            deviceCameras[Integer.parseInt(currentIdDeviceCamera)].closeCamera();
            setPreviewSize(nextIdDeviceCamera);
            l.post(() -> openCamera(Integer.parseInt(nextIdDeviceCamera)));
            currentIdDeviceCamera = nextIdDeviceCamera;
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPreviewSize(String cameraId) {
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            Size previewSize = CameraSizes.getPreviewOutputSize(activityMainBinding.supportFrameLayout.getWidth(),
                    activityMainBinding.supportFrameLayout.getHeight(),
                    cameraCharacteristics);
            activityMainBinding.imageCameraSurfaceView.setSizes(previewSize.getWidth(),
                    previewSize.getHeight(), activityMainBinding.supportFrameLayout.getWidth(),
                    activityMainBinding.supportFrameLayout.getHeight());
        } catch (Exception e) {
            Log.e(APP_TAG, "Set PreviewSize! camera id: " + cameraId + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void openCamera(int camera) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (deviceCameras[camera] != null) {
                if (!deviceCameras[camera].isOpen()) {
                    deviceCameras[camera].openCamera();
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    private void startBackgroundThread() {
        handlerThread = new HandlerThread("CameraBackground");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        if (deviceCameras != null) {
            for (CameraService cameraService : deviceCameras) {
                if (cameraService != null) {
                    cameraService.setHandler(handler);
                }
            }
        }
    }

    private void stopBackgroundThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (Exception e) {
            Log.e(APP_TAG, String.format("Stop background thread for cameraId %s! error %s",
                    currentIdDeviceCamera, e.getMessage()));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void initialiseAudioRecording() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (AudioDeviceManager.checkAvailableMicrophone(audioManager)) {
            audioRecordController = new AudioRecordController();
            initialiseGraphView();
        } else {
            Toast.makeText(this, "На устройстве нет микрофона", Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startAudioRecording() {
        audioRecordController.start();
        FFTControl fftControl = new FFTControl();
        fftControl.transformAudioData(audioRecordController, audioSpector -> {
            DataPoint[] dataAmpFreqPoint = new DataPoint[audioSpector.length];
            for (int i = 0; i < audioSpector.length; i++) {
                dataAmpFreqPoint[i] = new DataPoint(audioSpector[i][1], audioSpector[i][0]);
            }
            runOnUiThread(() -> pointAmplitudeFrequencyLineGraphSeries.resetData(dataAmpFreqPoint));
        });
    }

    private void initialiseGraphView() {
        pointAmplitudeFrequencyLineGraphSeries = new LineGraphSeries<>();
        pointAmplitudeFrequencyLineGraphSeries.setColor(Color.RED);
        activityMainBinding.graphView.getViewport().setMaxY(0);
        activityMainBinding.graphView.getViewport().setMinY(-120);
        activityMainBinding.graphView.getViewport().setYAxisBoundsManual(true);
        activityMainBinding.graphView.getViewport().setMaxX(8000);
        activityMainBinding.graphView.getViewport().setMinX(0);
        activityMainBinding.graphView.getViewport().setXAxisBoundsManual(true);
        activityMainBinding.graphView.addSeries(pointAmplitudeFrequencyLineGraphSeries);
        activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitle("Гц");
        activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitle("Дб");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(KEY_CURRENT_CAMERA_OF_DEVICE, currentIdDeviceCamera);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        for (CameraService cameraService : deviceCameras) {
            if (cameraService != null && cameraService.isOpen()) {
                cameraService.closeCamera();
            }
        }
        stopBackgroundThread();
        if (audioRecordController.isAudioRecording()) {
            audioRecordController.stop();
        }
        super.onPause();
    }
}