package com.jitsik.learning1;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.Timer;


/**
 * A PentagonView displays nice pentagons on the screen.
 */
public class PentagonView extends View {

    public PentagonAnimation[] animations;

    private static class Pentagon {

        public float x;
        public float y;
        public float angle;
        public float radius;
        public float opacity;

        Pentagon(float x, float y, float angle, float radius, float opacity) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.radius = radius;
            this.opacity = opacity;
        }

        public void clipAngle() {
            while (angle < 0) {
                angle += Math.PI * 2;
            }
            while (angle > Math.PI * 2) {
                angle -= Math.PI * 2;
            }
        }

        public Pentagon copy() {
            return new Pentagon(x, y, angle, radius, opacity);
        }

        public void draw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int)(opacity * 255.0f), 255, 255, 255));

            Path path = new Path();
            for (int i = 0; i < 5; ++i) {
                float pointAngle = ((2.0f / 5.0f) * (float)Math.PI * (float)i) +
                        this.angle;
                float x = convertX((float)Math.cos(pointAngle) * this.radius +
                        this.x, canvas);
                float y = convertY((float)Math.sin(pointAngle) * this.radius +
                        this.y, canvas);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.close();

            canvas.drawPath(path, paint);
        }

    }

    private static class PentagonAnimation {

        public Pentagon start;
        public Pentagon end;
        public long startTime;
        public float duration;

        PentagonAnimation(Pentagon start, Pentagon end, float duration) {
            this.start = start;
            this.end = end;
            this.duration = duration;
        }

        public void begin() {
            this.startTime = SystemClock.elapsedRealtime();
        }

        public void changeEnd(Pentagon p) {
            start = end;
            end = p;
        }

        public boolean done() {
            long elapsed = SystemClock.elapsedRealtime() - this.startTime;
            float timeRun = (float)elapsed / 1000.0f;
            return timeRun >= this.duration;
        }

        public Pentagon step() {
            long elapsed = SystemClock.elapsedRealtime() - this.startTime;
            float percentage = (float)elapsed / (1000.0f * this.duration);
            if (percentage > 1) {
                percentage = 1;
            }

            float x = this.start.x + ((this.end.x - this.start.x) * percentage);
            float y = this.start.y + ((this.end.y - this.start.y) * percentage);
            float angle = ((this.end.angle - this.start.angle) * percentage) +
                    this.start.angle;
            float radius = this.start.radius +
                    ((this.end.radius - this.start.radius) * percentage);
            float opacity = this.start.opacity +
                    ((this.end.opacity - this.start.opacity) * percentage);
            return new Pentagon(x, y, angle, radius, opacity);
        }

    }

    public PentagonView(Context context) {
        super(context);
        init(null, 0);
    }

    public PentagonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PentagonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    protected static float convertX(float x, Canvas c) {
        float width = (float)c.getWidth();
        float height = (float)c.getHeight();
        if (width > height) {
            return x * width;
        } else {
            float offset = (height - width) / 2;
            return (x * height) - offset;
        }
    }

    protected static float convertY(float x, Canvas c) {
        float width = (float)c.getWidth();
        float height = (float)c.getHeight();
        if (height > width) {
            return x * height;
        } else {
            float offset = (width - height) / 2;
            return (x * width) - offset;
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        animations = new PentagonAnimation[10];
        for (int i = 0; i < animations.length; ++i) {
            float duration = (float)(Math.random() * 30 + 60);
            Pentagon start = randomPentagon();
            Pentagon end = randomPentagon();
            animations[i] = new PentagonAnimation(start, end, duration);
            animations[i].begin();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(0x34, 0x98, 0xd8));
        for (PentagonAnimation animation : animations) {
            Pentagon pentagon = animation.step();
            pentagon.draw(canvas);
            if (animation.done()) {
                randomizeAnimation(animation);
            }
        }
        postInvalidateOnAnimation();
    }

    private void randomizeAnimation(PentagonAnimation animation) {
        // TODO: use weighting function based on gravity
        animation.duration = (float)(Math.random() * 30 + 60);
        animation.changeEnd(randomPentagon());
        animation.begin();
    }

    private static Pentagon randomPentagon() {
        float x = (float)Math.random();
        float y = (float)Math.random();
        float angle = (float)(Math.random() * Math.PI * 2);
        float radius = (float)((Math.random() * 0.05) + 0.15);
        float opacity = (float)(Math.random() * 0.3);
        return new Pentagon(x, y, angle, radius, opacity);
    }

}
