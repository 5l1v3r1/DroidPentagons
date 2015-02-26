package com.jitsik.pentagons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * A PentagonView displays an ambient background animation of floating
 * pentagons.
 */
public class PentagonView extends View {

    private Pentagon[] pentagons = new Pentagon[10];
    private Bitmap pentagonImage = null;
    private float scale = 0;
    private float xTranslate = 0;
    private float yTranslate = 0;
    private Matrix matrix = new Matrix();
    private Paint paint = new Paint();

    public PentagonView(Context context) {
        super(context);
        init();
    }

    public PentagonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PentagonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // Generate the starting state for each pentagon.
        for (int i = 0; i < pentagons.length; ++i) {
            PentagonState start = new PentagonState();
            start.position.x = (float)Math.random();
            start.position.y = (float)Math.random();
            start.angle = (float)(Math.random() * Math.PI * 2);
            start.radius = (float)((Math.random() * 0.05) + 0.15);
            start.opacity = (float)(Math.random() * 0.2) + 0.05f;
            pentagons[i] = new Pentagon(start);
        }

        // Start the animation for each pentagon.
        for (Pentagon p : pentagons) {
            generateNextPosition(p);
        }

        updateTranslation();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(Color.TRANSPARENT);

        if (pentagonImage == null) {
            generatePentagonImage();
        }

        // Draw each pentagon.
        for (Pentagon p : pentagons) {
            drawPentagon(c, p.frame());
        }

        // Start new animations for the pentagons that need it.
        for (Pentagon p : pentagons) {
            if (p.done()) {
                generateNextPosition(p);
            }
        }

        // Draw again on the next frame.
        postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateTranslation();
    }

    private static float cap(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    private void drawPentagon(Canvas c, PentagonState p) {
        float x = p.position.x * scale + xTranslate;
        float y = p.position.y * scale + yTranslate;
        float r = p.radius * scale;

        matrix.reset();
        matrix.postScale(2.0f * r / (float)pentagonImage.getWidth(),
                2.0f * r / (float)pentagonImage.getHeight());
        matrix.postTranslate(-r, -r);
        matrix.postRotate(p.angle * 180.0f / (float)Math.PI);
        matrix.postTranslate(x, y);

        paint.setAlpha(Math.round(p.opacity * 255.0f));
        c.drawBitmap(pentagonImage, matrix, paint);
    }

    private void generateNextPosition(Pentagon p) {
        // Compute the total net force on the pentagon.
        Vector f = new Vector();
        PentagonState frame = p.frame();
        frame.addForceFromEdges(f);
        for (Pentagon aPentagon : pentagons) {
            if (aPentagon == p) {
                continue;
            }
            frame.addForceFrom(aPentagon.frame(), f);
        }

        // Manipulate the force to be used as displacement.
        f.scale(0.05f);
        f.capMagnitude(0.3f);
        f.addRandom(0.2f);

        // Adjust the current frame to be a destination frame.
        frame.angle += (float)(Math.random() * 2.0f) - 1.0f;
        frame.radius = cap(frame.radius + (float)(Math.random() * 0.2f) -
                0.1f, 0.15f, 0.2f);
        frame.opacity = cap(frame.opacity - 0.05f +
                (float)(Math.random() * 0.1f), 0.0f, 0.25f);
        frame.position.x = cap(frame.position.x + f.x, 0.0f, 1.0f);
        frame.position.y = cap(frame.position.y + f.y, 0.0f, 1.0f);

        // Start the new animation.
        float duration = (float)(Math.random() * 30 + 60);
        p.begin(frame, duration);
    }

    private void generatePentagonImage() {
        // Generate the smallest power of two that's bigger than the largest
        // possible pentagon.
        int maximumSize = Math.max(this.getWidth(), this.getHeight()) / 3;
        int bitmapSize = 1;
        while (bitmapSize < maximumSize) {
            bitmapSize *= 2;
        }

        // Create the bitmap and canvas.
        pentagonImage = Bitmap.createBitmap(bitmapSize, bitmapSize,
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(pentagonImage);

        // Trace the shape of a pentagon.
        Path p = new Path();
        float r = (float)bitmapSize / 2.0f;
        for (int i = 0; i < 5; ++i) {
            float angle = 0.4f * (float)i * (float)Math.PI;
            float x = r + (float)Math.cos(angle) * r;
            float y = r + (float)Math.sin(angle) * r;
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.close();

        // Draw the pentagon in white.
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        c.drawPath(p, paint);
    }

    private void updateTranslation() {
        float width = (float)this.getWidth();
        float height = (float)this.getHeight();
        if (width > height) {
            scale = width;
            xTranslate = 0;
            yTranslate = -(width - height) / 2.0f;
        } else {
            scale = height;
            xTranslate = -(height - width) / 2.0f;
            yTranslate = 0;
        }
    }

}
