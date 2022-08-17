package com.elijah.internproject.utils;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class AudioRecordController {
    private AudioRecord audioRecorder;
    public final static int FREQUENCY = 32000;
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public final static int BLOCK_SIZE = 768;
    int minBufferSize;

    public AudioRecordController() {
        minBufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL_CONFIG, AUDIO_FORMAT);
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (!isAudioRecording()) {
            audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
            audioRecorder.startRecording();
        }
    }

    public void stop() {
        if (audioRecorder != null) {
            audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
        }
    }

    public boolean isAudioRecording() {
        return audioRecorder != null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public short[] getAudioData() {
        short[] audioData = new short[BLOCK_SIZE];
        audioRecorder.read(audioData, 0, BLOCK_SIZE);
        return audioData;
    }
}
