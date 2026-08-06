package com.smarttool.videodownloader.core.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.smarttool.videodownloader.android.R;


public class RoundedImageView extends AppCompatImageView {

    private float cornerRadiusTopLeft = 0f;
    private float cornerRadiusTopRight = 0f;
    private float cornerRadiusBottomLeft = 0f;
    private float cornerRadiusBottomRight = 0f;
    private Path path;
    private RectF rect;

    public RoundedImageView(Context context) {
        super(context);
        init(null);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        path = new Path();
        if (attrs != null) {
            // Load custom attributes
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.RoundedImageView);
            float defaultRadius = ta.getDimension(R.styleable.RoundedImageView_cornerRadius, 0f);
            cornerRadiusTopLeft = ta.getDimension(R.styleable.RoundedImageView_cornerRadiusTopLeft, defaultRadius);
            cornerRadiusTopRight = ta.getDimension(R.styleable.RoundedImageView_cornerRadiusTopRight, defaultRadius);
            cornerRadiusBottomLeft = ta.getDimension(R.styleable.RoundedImageView_cornerRadiusBottomLeft, defaultRadius);
            cornerRadiusBottomRight = ta.getDimension(R.styleable.RoundedImageView_cornerRadiusBottomRight, defaultRadius);
            ta.recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect = new RectF(0, 0, w, h);
        updatePath();
    }

    private void updatePath() {
        path.reset();
        float[] radii = {
                cornerRadiusTopLeft, cornerRadiusTopLeft,  // Top-left
                cornerRadiusTopRight, cornerRadiusTopRight, // Top-right
                cornerRadiusBottomRight, cornerRadiusBottomRight, // Bottom-right
                cornerRadiusBottomLeft, cornerRadiusBottomLeft // Bottom-left
        };
        path.addRoundRect(rect, radii, Path.Direction.CW);
        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (path != null) {
            canvas.clipPath(path);
        }
        super.onDraw(canvas);
    }
}
