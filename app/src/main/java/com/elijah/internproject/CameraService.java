package com.elijah.internproject;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.util.Collections;

public class CameraService {
    private static final String APP_TAG = "CAMERA SERVICE";
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice = null;
    private Handler handler;
    private final CameraManager cameraManager;
    private final String cameraID;
    private final Surface imageCameraSurface;

    public CameraService(String cameraID, Surface imageCameraSurface, Handler handler, CameraManager cameraManager) {
        this.cameraID = cameraID;
        this.imageCameraSurface = imageCameraSurface;
        this.handler = handler;
        this.cameraManager = cameraManager;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(APP_TAG, String.format("Camera callback cameraId! %s! error %d", camera.getId(), error));
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createCameraPreviewSession() {
        Surface surface = imageCameraSurface;
        try {
            final CaptureRequest.Builder captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequest.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try {
                                cameraCaptureSession.setRepeatingRequest(captureRequest.build(), null, handler);
                            } catch (Exception e) {
                                Log.e(APP_TAG, String.format("Camera capture Session! Error: %s", e.getMessage()));
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }
                    }, handler);
        } catch (Exception e) {
            Log.e(APP_TAG, String.format("Builder camera request! Error: %s", e.getMessage()));
        }
    }


    @SuppressLint("MissingPermission")
    public void openCamera() {
        try {
            cameraManager.openCamera(cameraID, cameraStateCallback, handler);
        } catch (Exception e) {
            Log.e(APP_TAG, String.format("Open camera! Error: %s", e.getMessage()));
        }
    }

    public boolean isOpen() {
        return cameraDevice != null;
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
