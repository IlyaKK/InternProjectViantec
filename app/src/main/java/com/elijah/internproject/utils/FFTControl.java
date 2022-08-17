package com.elijah.internproject.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.elijah.internproject.domain.FFTTransformer;
import com.elijah.internproject.libs.fftpack.Complex1D;
import com.elijah.internproject.libs.fftpack.ComplexDoubleFFT;

public class FFTControl {

    public double[][] transform(short[] audioData) {
        int sizeData = audioData.length;
        double[][] spectorAudio = new double[sizeData / 2][2];
        ComplexDoubleFFT complexDoubleFFT = new ComplexDoubleFFT(sizeData);
        Complex1D X = new Complex1D();
        X.x = new double[sizeData];
        X.y = new double[sizeData];
        for (int k = 0; k < sizeData; k++) {
            X.x[k] = (double) audioData[k] / (double) Short.MAX_VALUE;
        }
        complexDoubleFFT.ft(X);
        for (int i = 0; i < sizeData / 2; i++) {
            double x = X.x[i];
            double y = X.y[i];
            double amplitude = 20.0 * Math.log10(Math.sqrt(x * x + y * y) / (double) sizeData);
            double frequency = i * ((double) AudioRecordController.FREQUENCY / (double) AudioRecordController.BLOCK_SIZE);
            spectorAudio[i][0] = amplitude;
            spectorAudio[i][1] = frequency;
        }
        return spectorAudio;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void transformAudioData(AudioRecordController audioRecordController, FFTTransformer fftTransformer) {
        new Thread(() -> {
            while (audioRecordController.isAudioRecording()) {
                short[] data = audioRecordController.getAudioData();
                double[][] spectorData = transform(data);
                fftTransformer.setAudioSpector(spectorData);
            }
        }).start();
    }
}
