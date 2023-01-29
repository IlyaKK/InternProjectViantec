package com.elijah.internproject.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.elijah.internproject.domain.FFTTransformer;
import com.elijah.internproject.domain.ModeAmplitude;
import com.elijah.internproject.libs.fftpack.Complex1D;
import com.elijah.internproject.libs.fftpack.ComplexDoubleFFT;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FFTControl {
    private final Window window = new Window();
    private static final double SPL = 2.0 * Math.pow(10, -5);

    public void setModeAmplitude(ModeAmplitude modeAmplitude) {
        this.modeAmplitude = modeAmplitude;
    }

    private ModeAmplitude modeAmplitude;
    private Double coefficientForMode = 1.0;

    public FFTControl(ModeAmplitude modeAmplitude) {
        this.modeAmplitude = modeAmplitude;
    }

    public double[][] transform(short[] audioData) {
        switch (modeAmplitude) {
            case FS_MODE: {
                coefficientForMode = 1.0;
                break;
            }
            case SPL_MODE:
                coefficientForMode = SPL;
                break;
        }
        int sizeData = audioData.length;
        double[][] spectorAudio = new double[sizeData / 2][2];
        ComplexDoubleFFT complexDoubleFFT = new ComplexDoubleFFT(sizeData);
        Complex1D X = new Complex1D();
        X.x = new double[sizeData];
        X.y = new double[sizeData];
        for (int k = 0; k < sizeData; k++) {
            double windowGausse = window.gausseFrame(k, sizeData);
            X.x[k] = ((double) audioData[k] / (double) Short.MAX_VALUE) * windowGausse;
        }
        complexDoubleFFT.ft(X);
        for (int i = 0; i < sizeData / 2; i++) {
            double x = X.x[i];
            double y = X.y[i];
            double amplitude = 20.0 * Math.log10((Math.sqrt(x * x + y * y) /
                    (double) sizeData) / coefficientForMode);

            double frequency = i * ((double) AudioRecordController.FREQUENCY / (double) AudioRecordController.BLOCK_SIZE);
            spectorAudio[i][0] = amplitude;
            spectorAudio[i][1] = frequency;
        }
        return spectorAudio;
    }

    public Future<?> transformAudioData(AudioRecordController audioRecordController, ExecutorService executorService, FFTTransformer fftTransformer) {
        Runnable runnable = () -> {
            while (audioRecordController.isAudioRecording()) {
                short[] data = audioRecordController.getAudioData();
                double[][] spectorData = transform(data);
                fftTransformer.setAudioSpector(spectorData);
            }
        };
        return executorService.submit(runnable);
    }
}
