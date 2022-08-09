package com.elijah.internproject.utils;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CameraDeviceManager {
    private static final String TAG = "Camera device service";

    public static List<String> filterCompatibleCameras(CameraManager cameraManager, String[] cameraIds) {
        final List<String> compatibleCameras = new ArrayList<>();

        try {
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                for (int capability : capabilities) {
                    if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                        compatibleCameras.add(id);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "FilterCompatibleCameras! Error: " + e.getMessage());
        }

        return compatibleCameras;
    }

    private static List<String> filterCameraIdsFacing(CameraManager cameraManager, List<String> cameraIds, int lensFacing) {
        final List<String> compatibleCameras = new ArrayList<>();

        try {
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing) {
                    compatibleCameras.add(id);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "FilterCameraIdsFacing! Error: " + e.getMessage());
        }

        return compatibleCameras;
    }

    public static String getInitialBackCameraId(CameraManager cameraManager) {
        String backCamera = null;
        try {
            List<String> compatibleCameraIds = filterCompatibleCameras(cameraManager, cameraManager.getCameraIdList());
            List<String> backCameras = filterCameraIdsFacing(cameraManager, compatibleCameraIds, CameraMetadata.LENS_FACING_BACK);
            backCamera = backCameras.get(0);
        } catch (Exception e) {
            Log.e(TAG, "Get initial back cameraId! Error: " + e.getMessage());
        }
        return backCamera;
    }

    public static String getNextCameraId(CameraManager cameraManager, @Nullable String currentCameraId) {
        String nextCameraId = null;

        try {
            List<String> compatibleCameraIds = filterCompatibleCameras(cameraManager, cameraManager.getCameraIdList());
            List<String> backCameras = filterCameraIdsFacing(cameraManager, compatibleCameraIds, CameraMetadata.LENS_FACING_BACK);
            List<String> frontCameras = filterCameraIdsFacing(cameraManager, compatibleCameraIds, CameraMetadata.LENS_FACING_FRONT);
            List<String> allCameras = new ArrayList<>();
            if (!backCameras.isEmpty()) allCameras.add(backCameras.get(0));
            if (!frontCameras.isEmpty()) allCameras.add(frontCameras.get(0));
            int cameraIndex = allCameras.indexOf(currentCameraId);
            if (cameraIndex == -1) {
                nextCameraId = !allCameras.isEmpty() ? allCameras.get(0) : null;
            } else {
                if (!allCameras.isEmpty()) {
                    nextCameraId = allCameras.get((cameraIndex + 1) % allCameras.size());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "GetNextCameraId! Error: " + e.getMessage());
        }

        return nextCameraId;
    }
}

