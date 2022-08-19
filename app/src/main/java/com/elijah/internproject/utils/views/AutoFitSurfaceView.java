package com.elijah.internproject.utils.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceView;

public class AutoFitSurfaceView extends SurfaceView {
    private Integer previewSizeWidth = 1;
    private Integer previewSizeHeight = 1;
    private Integer widthContainerPreview = 1;
    private Integer heightContainerPreview = 1;

    public AutoFitSurfaceView(Context context) {
        super(context);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSizes(Integer previewSizeWidth, Integer previewSizeHeight,
                         Integer widthContainerPreview,
                         Integer heightContainerPreview) {
        this.previewSizeWidth = previewSizeWidth;
        this.previewSizeHeight = previewSizeHeight;
        this.widthContainerPreview = widthContainerPreview;
        this.heightContainerPreview = heightContainerPreview;
        getHolder().setFixedSize(previewSizeWidth, previewSizeHeight);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        @SuppressLint("DrawAllocation") RectF rectContainer = new RectF();
        @SuppressLint("DrawAllocation") RectF rectPreview = new RectF();
        rectContainer.set(0, 0, widthContainerPreview, heightContainerPreview);
        if (getDisplay().getRotation() == Surface.ROTATION_0 ||
                getDisplay().getRotation() == Surface.ROTATION_180) {
            if (previewSizeHeight <= previewSizeWidth) {
                rectPreview.set(0, 0, previewSizeHeight, previewSizeWidth);
            } else {
                rectPreview.set(0, 0, previewSizeWidth, previewSizeHeight);
            }
        } else {
            if (previewSizeHeight <= previewSizeWidth) {
                rectPreview.set(0, 0, previewSizeWidth, previewSizeHeight);
            } else {
                rectPreview.set(0, 0, previewSizeHeight, previewSizeWidth);
            }
        }
        @SuppressLint("DrawAllocation") Matrix matrix = new Matrix();
        matrix.setRectToRect(rectPreview, rectContainer,
                Matrix.ScaleToFit.START);
        matrix.mapRect(rectPreview);
        setMeasuredDimension(Math.round(rectPreview.right), Math.round(rectPreview.bottom));
    }
}
