package com.elijah.internproject.domain;

public enum ModeAmplitude {
    FS_MODE(5), SPL_MODE(6);

    private final int number;

    ModeAmplitude(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
