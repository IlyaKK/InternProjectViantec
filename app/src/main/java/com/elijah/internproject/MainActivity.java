package com.elijah.internproject;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.elijah.internproject.domain.ModeAmplitude;
import com.elijah.internproject.utils.AudioDeviceManager;
import com.elijah.internproject.utils.AudioRecordController;
import com.elijah.internproject.utils.CameraDeviceManager;
import com.elijah.internproject.utils.CameraSizes;
import com.elijah.internproject.utils.FFTControl;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.BaseSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_MODE_Y_AXIS = "KEY_MODE_Y_AXIS";
    private final String KEY_CURRENT_CAMERA_OF_DEVICE = "KEY_CURRENT_CAMERA_OF_DEVICE";
    public final String APP_TAG = "CAMERA ACTIVITY";
    private ActivityMainBinding activityMainBinding;
    private CameraService[] deviceCameras = null;
    private CameraManager cameraManager = null;
    private String currentIdDeviceCamera = null;
    private HandlerThread handlerThread;
    private Handler handlerCamera;
    private AudioRecordController audioRecordController = null;
    private BaseSeries<DataPoint> pointAmplitudeFrequencyBaseGraphSeries = null;
    private ModeAmplitude currentModeAmplitude = null;
    private FFTControl fftControl = null;
    private final ExecutorService executorServiceForFFT = Executors.newSingleThreadExecutor();
    private Future<?> taskAudioFFT;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        super.onCreate(savedInstanceState);
        initialiseCamera(savedInstanceState);
        initialiseAudioRecording(savedInstanceState);
    }

    @Override
    protected void onResume() {
        startBackgroundThread();
        if (audioRecordController != null) {
            startAudioRecording();
        }
        super.onResume();
    }

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
                            handlerCamera, cameraManager);
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

    private void initialiseSwitchCamera(Surface surface) {
        activityMainBinding.switchCameraFrontBackButton.setOnClickListener(l -> {
            String nextIdDeviceCamera = CameraDeviceManager.getNextCameraId(cameraManager, currentIdDeviceCamera);
            if (deviceCameras[Integer.parseInt(nextIdDeviceCamera)] == null) {
                deviceCameras[Integer.parseInt(nextIdDeviceCamera)] = new CameraService(nextIdDeviceCamera, surface, handlerCamera, cameraManager);
            }
            deviceCameras[Integer.parseInt(currentIdDeviceCamera)].closeCamera();
            setPreviewSize(nextIdDeviceCamera);
            l.post(() -> openCamera(Integer.parseInt(nextIdDeviceCamera)));
            currentIdDeviceCamera = nextIdDeviceCamera;
        });
    }

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
        handlerCamera = new Handler(handlerThread.getLooper());
        if (deviceCameras != null) {
            for (CameraService cameraService : deviceCameras) {
                if (cameraService != null) {
                    cameraService.setHandler(handlerCamera);
                }
            }
        }
    }

    private void stopBackgroundThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handlerCamera = null;
        } catch (Exception e) {
            Log.e(APP_TAG, String.format("Stop background thread for cameraId %s! error %s",
                    currentIdDeviceCamera, e.getMessage()));
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.P)
    private void initialiseAudioRecording(Bundle savedInstanceState) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (AudioDeviceManager.isAvailableMicrophone(audioManager)) {
            audioRecordController = new AudioRecordController();
            initialiseGraphView(savedInstanceState);
        } else {
            Toast.makeText(this, "На устройстве нет микрофона", Toast.LENGTH_LONG).show();
        }
    }

    private void startAudioRecording() {
        audioRecordController.start();
        fftControl = new FFTControl(currentModeAmplitude);
        initialiseSwitchModeAmplitudeAxis();
        taskAudioFFT = fftControl.transformAudioData(audioRecordController, executorServiceForFFT, audioSpector -> {
            DataPoint[] dataAmpFreqPoint = new DataPoint[audioSpector.length];
            for (int i = 0; i < audioSpector.length; i++) {
                dataAmpFreqPoint[i] = new DataPoint(audioSpector[i][1], audioSpector[i][0]);
            }
            runOnUiThread(() -> pointAmplitudeFrequencyBaseGraphSeries.resetData(dataAmpFreqPoint));
        });
    }

    private void initialiseSwitchModeAmplitudeAxis() {
        activityMainBinding.radioGroupSwitchDB.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.db_fs_radio_button:
                    activityMainBinding.graphView.getSeries().clear();
                    pointAmplitudeFrequencyBaseGraphSeries = new LineGraphSeries<>();
                    pointAmplitudeFrequencyBaseGraphSeries.setColor(getResources().getColor(R.color.light_green));
                    activityMainBinding.graphView.addSeries(pointAmplitudeFrequencyBaseGraphSeries);
                    currentModeAmplitude = ModeAmplitude.FS_MODE;
                    fftControl.setModeAmplitude(currentModeAmplitude);
                    activityMainBinding.graphView.getViewport().setMaxY(0);
                    activityMainBinding.graphView.getViewport().setMinY(-120);
                    activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitle(getString(R.string.hg_axis_title));
                    activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitle(getString(R.string.dB_fs_axis_title));
                    break;
                case R.id.db_spl_radio_button:
                    activityMainBinding.graphView.getSeries().clear();
                    pointAmplitudeFrequencyBaseGraphSeries = new BarGraphSeries<>();
                    pointAmplitudeFrequencyBaseGraphSeries.setColor(getResources().getColor(R.color.light_green));
                    activityMainBinding.graphView.addSeries(pointAmplitudeFrequencyBaseGraphSeries);
                    currentModeAmplitude = ModeAmplitude.SPL_MODE;
                    fftControl.setModeAmplitude(currentModeAmplitude);
                    activityMainBinding.graphView.getViewport().setMaxY(120);
                    activityMainBinding.graphView.getViewport().setMinY(0);
                    activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitle(getString(R.string.hg_axis_title));
                    activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitle(getString(R.string.db_spl_axis_title));
                    break;
            }
        });
    }

    private void initialiseGraphView(Bundle savedInstanceState) {
        activityMainBinding.graphView.getViewport().setYAxisBoundsManual(true);
        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(KEY_MODE_Y_AXIS) == ModeAmplitude.FS_MODE.getNumber()) {
                currentModeAmplitude = ModeAmplitude.FS_MODE;
                activityMainBinding.dbFsRadioButton.setChecked(true);
                pointAmplitudeFrequencyBaseGraphSeries = new LineGraphSeries<>();
                activityMainBinding.graphView.getViewport().setMaxY(0);
                activityMainBinding.graphView.getViewport().setMinY(-120);
                activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitle(getString(R.string.hg_axis_title));
                activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitle(getString(R.string.dB_fs_axis_title));
            } else {
                currentModeAmplitude = ModeAmplitude.SPL_MODE;
                activityMainBinding.dbSplRadioButton.setChecked(true);
                pointAmplitudeFrequencyBaseGraphSeries = new BarGraphSeries<>();
                activityMainBinding.graphView.getViewport().setMaxY(120);
                activityMainBinding.graphView.getViewport().setMinY(0);
                activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitle(getString(R.string.hg_axis_title));
                activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitle(getString(R.string.db_spl_axis_title));
            }
        } else {
            currentModeAmplitude = ModeAmplitude.FS_MODE;
            activityMainBinding.dbFsRadioButton.setChecked(true);
            pointAmplitudeFrequencyBaseGraphSeries = new LineGraphSeries<>();
            activityMainBinding.graphView.getViewport().setMaxY(0);
            activityMainBinding.graphView.getViewport().setMinY(-120);
            activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitle(getString(R.string.hg_axis_title));
            activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitle(getString(R.string.dB_fs_axis_title));
        }
        pointAmplitudeFrequencyBaseGraphSeries.setColor(getResources().getColor(R.color.light_green));
        activityMainBinding.graphView.addSeries(pointAmplitudeFrequencyBaseGraphSeries);
        activityMainBinding.graphView.setBackgroundColor(getResources().getColor(R.color.black));

        activityMainBinding.graphView.getViewport().setXAxisBoundsManual(true);
        activityMainBinding.graphView.getViewport().setScalable(true);
        activityMainBinding.graphView.getViewport().setMaxX(8000);
        activityMainBinding.graphView.getViewport().setMinX(0);

        activityMainBinding.graphView.getGridLabelRenderer().setNumHorizontalLabels(4);
        activityMainBinding.graphView.getGridLabelRenderer().setGridColor(getResources().getColor(R.color.white));
        activityMainBinding.graphView.getGridLabelRenderer().setHorizontalAxisTitleColor(getResources().getColor(R.color.white));
        activityMainBinding.graphView.getGridLabelRenderer().setVerticalAxisTitleColor(getResources().getColor(R.color.white));
        activityMainBinding.graphView.getGridLabelRenderer().setHorizontalLabelsColor(getResources().getColor(R.color.white));
        activityMainBinding.graphView.getGridLabelRenderer().setVerticalLabelsColor(getResources().getColor(R.color.white));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_MODE_Y_AXIS, currentModeAmplitude.getNumber());
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
        taskAudioFFT.cancel(true);
        super.onPause();
    }
}