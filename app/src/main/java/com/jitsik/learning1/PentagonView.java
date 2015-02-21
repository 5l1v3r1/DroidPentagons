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


/**
 * A PentagonView displays nice pentagons on the screen.
 */
public class PentagonView extends View {

    public PentagonAnimation[] animations;

    /**
     * A force is two-component vector.
     */
    private static class Force {

        public float x;
        public float y;

        public Force() {
            this.x = 0;
            this.y = 0;
        }

        public Force(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void addRandom(float max) {
            this.x += (Math.random() * max * 2) - max;
            this.y += (Math.random() * max * 2) - max;
        }

        public void cap(float max) {
            float mag = magnitude();
            if (mag > max) {
                scale(max/mag);
            }
        }

        public float magnitude() {
            return (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        }

        public void scale(float factor) {
            x *= factor;
            y *= factor;
        }

    }

    /**
     * A pentagon has a center, an angle, a radius, and an opacity.
     */
    private static class Pentagon {

        public float x;
        public float y;
        public float angle;
        public float radius;
        public float opacity;
        private Paint paintCache;

        Pentagon(float x, float y, float angle, float radius, float opacity) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.radius = radius;
            this.opacity = opacity;

            paintCache = new Paint();
            paintCache.setFlags(Paint.ANTI_ALIAS_FLAG);
            paintCache.setStyle(Paint.Style.FILL);
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
            paintCache.setColor(Color.argb((int)(opacity * 255.0f), 255, 255,
                    255));

            // Draw a pentagon using some basic trig.
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

            canvas.drawPath(path, paintCache);
        }

        public void forceFrom(Pentagon p, Force addTo) {
            // Use an inverse-square relationship to compute a force.
            float d2 = (float)(Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2));
            float distance = (float)Math.sqrt(d2);
            if (distance < 0.001f) {
                return;
            }
            float magnitude = 1.0f / d2;
            float xComp = (x - p.x) / distance;
            float yComp = (y - p.y) / distance;
            addTo.x += magnitude * xComp;
            addTo.y += magnitude * yComp;
        }

        public void forceFromEdges(Force addTo) {
            addTo.x += 1.0f / Math.pow(0.01 + x, 2);
            addTo.x -= 1.0f / Math.pow(1.01f - x, 2);
            addTo.y += 1.0f / Math.pow(0.01 + y, 2);
            addTo.y -= 1.0f / Math.pow(1.01f - y, 2);
        }

    }

    /**
     * An animation keeps track of a start and end pentagon state.
     */
    private static class PentagonAnimation {

        private Pentagon reuse;
        public Pentagon start;
        public Pentagon end;
        public long startTime;
        public float duration;

        PentagonAnimation(Pentagon start, Pentagon end, float duration) {
            reuse = start.copy();
            this.start = start;
            this.end = end;
            this.duration = duration;
        }

        public void begin() {
            this.startTime = SystemClock.elapsedRealtime();
        }

        public void changeEnd(float x, float y, float angle, float radius,
                              float opacity) {
            Pentagon temp = start;
            start = end;
            start.clipAngle();
            end = temp;
            end.x = x;
            end.y = y;
            end.angle = angle;
            end.radius = radius;
            end.opacity = opacity;
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

            // Update the reuse Pentagon
            reuse.x = this.start.x + ((this.end.x - this.start.x) * percentage);
            reuse.y = this.start.y + ((this.end.y - this.start.y) * percentage);
            reuse.angle = ((this.end.angle - this.start.angle) * percentage) +
                    this.start.angle;
            reuse.radius = this.start.radius +
                    ((this.end.radius - this.start.radius) * percentage);
            reuse.opacity = this.start.opacity +
                    ((this.end.opacity - this.start.opacity) * percentage);
            reuse.clipAngle();

            return reuse;
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

    private static float capValue(float val, float min, float max) {
        return Math.max(Math.min(val, max), min);
    }

    /**
     * Convert a relative coordinate from 0 to 1 into a view coordinate.
     * @param x The relative coordinate
     * @param c The canvas (whose dimensions are used for scaling)
     * @return A scaled coordinate
     */
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

    /**
     * Convert a relative coordinate from 0 to 1 into a view coordinate.
     * @param y The relative coordinate
     * @param c The canvas (whose dimensions are used for scaling)
     * @return A scaled coordinate
     */
    protected static float convertY(float y, Canvas c) {
        float width = (float)c.getWidth();
        float height = (float)c.getHeight();
        if (height > width) {
            return y * height;
        } else {
            float offset = (width - height) / 2;
            return (y * width) - offset;
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        animations = new PentagonAnimation[10];
        for (int i = 0; i < animations.length; ++i) {
            Pentagon start = randomPentagon();
            animations[i] = new PentagonAnimation(start, start.copy(), 0);
            animations[i].begin();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(0x34, 0x98, 0xd8));

        // Draw each PentagonAnimation.
        for (PentagonAnimation animation : animations) {
            Pentagon pentagon = animation.step();
            pentagon.draw(canvas);
            if (animation.done()) {
                randomizeAnimation(animation);
            }
        }

        // Redraw on the next frame.
        postInvalidateOnAnimation();
    }

    private void randomizeAnimation(PentagonAnimation animation) {
        // Compute the total net force from every other pentagon and from the
        // edges of the frame.
        Force f = new Force();
        Pentagon current = animation.step();
        current.forceFromEdges(f);
        for (PentagonAnimation x : animations) {
            if (x == animation) {
                continue;
            }
            Pentagon state = x.step();
            current.forceFrom(state, f);
        }

        // Scale the force to be used as a displacement, cap it, and randomize
        // it a bit.
        f.scale(0.05f);
        f.cap(0.3f);
        f.addRandom(0.2f);

        // Generate each element of the new pentagon using information from the
        // current pentagon.
        float angle = current.angle + (float)(Math.random() * 2.0f) - 1.0f;
        float radius = capValue(current.radius + (float)(Math.random() * 0.2f) -
                0.1f, 0.15f, 0.2f);
        float opacity = capValue(current.opacity - 0.05f +
                (float)(Math.random() * 0.1f), 0.0f, 0.25f);
        float x = capValue(current.x + f.x, 0.0f, 1.0f);
        float y = capValue(current.y + f.y, 0.0f, 1.0f);

        animation.duration = (float)(Math.random() * 30 + 60);
        animation.changeEnd(x, y, angle, radius, opacity);
        animation.begin();
    }

    private static Pentagon randomPentagon() {
        float x = (float)Math.random();
        float y = (float)Math.random();
        float angle = (float)(Math.random() * Math.PI * 2);
        float radius = (float)((Math.random() * 0.05) + 0.15);
        float opacity = (float)(Math.random() * 0.2) + 0.05f;
        return new Pentagon(x, y, angle, radius, opacity);
    }

}
