package com.elijah.internproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.elijah.internproject.databinding.ActivityCameraBinding;
import com.elijah.internproject.utils.CameraDeviceManager;
import com.elijah.internproject.utils.CameraSizes;

public class CameraActivity extends AppCompatActivity {
    public final String APP_TAG = "CAMERA ACTIVITY";
    private final String KEY_CURRENT_CAMERA_OF_DEVICE = "KEY_CURRENT_CAMERA_OF_DEVICE";
    private ActivityCameraBinding activityCameraBinding;
    private CameraService[] deviceCameras = null;
    private CameraManager cameraManager = null;
    private String currentIdDeviceCamera = null;
    private HandlerThread handlerThread;
    private Handler handler;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityCameraBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(activityCameraBinding.getRoot());
        super.onCreate(savedInstanceState);
        initialiseCamera(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        startBackgroundThread();
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
        activityCameraBinding.imageCameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    deviceCameras = new CameraService[CameraDeviceManager.filterCompatibleCameras(cameraManager,
                            cameraManager.getCameraIdList()).size()];
                    deviceCameras[Integer.parseInt(currentIdDeviceCamera)] = new CameraService(currentIdDeviceCamera, holder.getSurface(),
                            handler, cameraManager);
                    setPreviewSize(currentIdDeviceCamera);
                    activityCameraBinding.imageCameraSurfaceView.getRootView().post(() -> openCamera(Integer.parseInt(currentIdDeviceCamera)));
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
        activityCameraBinding.switchCameraFrontBack.setOnClickListener(l -> {
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
            Size previewSize = CameraSizes.getPreviewOutputSize(activityCameraBinding.supportFrameLayout.getWidth(),
                    activityCameraBinding.supportFrameLayout.getHeight(),
                    cameraCharacteristics);
            activityCameraBinding.imageCameraSurfaceView.setSizes(previewSize.getWidth(),
                    previewSize.getHeight(), activityCameraBinding.supportFrameLayout.getWidth(),
                    activityCameraBinding.supportFrameLayout.getHeight());
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
        super.onPause();
    }
}