package com.elijah.internproject.utils;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CameraSizes {
    private static class SmartSize {
        public Size size;
        public Integer longSize;
        public Integer shortSize;

        public SmartSize(Integer width, Integer height) {
            size = new Size(width, height);
            longSize = Math.max(size.getWidth(), size.getHeight());
            shortSize = Math.min(size.getWidth(), size.getHeight());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Size getPreviewOutputSize(Integer widthSupportFrameLayout,
                                            Integer heightSupportFrameLayout,
                                            CameraCharacteristics cameraCharacteristics) {
        SmartSize maxSize = new SmartSize(widthSupportFrameLayout, heightSupportFrameLayout);
        StreamConfigurationMap config = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] allSizes = config.getOutputSizes(SurfaceHolder.class);
        List<Size> sortedSizes = Arrays.asList(allSizes);
        List<SmartSize> validSizes =
                sortedSizes.stream()
                        .sorted(Comparator.comparing(s -> s.getHeight() * s.getWidth()))
                        .map(s -> new SmartSize(s.getWidth(), s.getHeight())).collect(Collectors.toList());
        Collections.reverse(validSizes);
        return validSizes.stream()
                .filter(s -> s.longSize <= maxSize.longSize && s.shortSize <= maxSize.shortSize).findFirst().get().size;
    }
}