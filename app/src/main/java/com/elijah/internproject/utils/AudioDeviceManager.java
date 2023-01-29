package com.elijah.internproject.utils;

import android.media.AudioManager;
import android.media.MicrophoneInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AudioDeviceManager {
    @RequiresApi(api = Build.VERSION_CODES.P)
    public static boolean isAvailableMicrophone(AudioManager audioManager) {
        try {
            List<MicrophoneInfo> list = audioManager.getMicrophones();
            Log.i("AudioDeviceManager", String.format("%s",
                    list.stream()
                            .map(MicrophoneInfo::getId)
                            .collect(Collectors.toList())));
            return !list.isEmpty();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
