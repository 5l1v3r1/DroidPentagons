package com.jitsik.learning1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;


/**
 * A PentagonView displays nice pentagons on the screen.
 */
public class PentagonView extends View {

    private PentagonAnimation[] animations = new PentagonAnimation[10];
    private Coords coordinateSystem = new Coords();
    private Bitmap pentagonImage;

    /**
     * Coords converts coordinate systems.
     */
    private static class Coords {

        private float scale = 0;
        private float xTranslate = 0;
        private float yTranslate = 0;

        public float convertX(float x) {
            return (x * scale) + xTranslate;
        }

        public float convertY(float y) {
            return (y * scale) + yTranslate;
        }

        public void setSize(int width, int height) {
            if (width > height) {
                scale = (float)width;
                xTranslate = 0;
                yTranslate = -(float)(width - height) / 2;
            } else {
                scale = (float)height;
                xTranslate = -(float)(height - width) / 2;
                yTranslate = 0;
            }
        }

    }

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

        public void draw(Canvas canvas, Bitmap bmp, Coords coords) {
            float realX = coords.convertX(x);
            float realY = coords.convertY(y);
            float r = coords.scale * (float)radius;
            Matrix matrix = new Matrix();
            matrix.postScale(2 * r / (float)bmp.getWidth(),
                    2 * r / (float)bmp.getHeight());
            matrix.postTranslate(-r, -r);
            matrix.postRotate(this.angle * 180.0f / (float)Math.PI);
            matrix.postTranslate(realX, realY);

            Paint paint = new Paint();
            paint.setAlpha((int)Math.round(opacity * 255.0f));
            canvas.drawBitmap(bmp, matrix, paint);
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

    private void init(AttributeSet attrs, int defStyle) {
        // Generate the pentagon bitmap.
        int bitmapSize = 512;
        Bitmap b = Bitmap.createBitmap(bitmapSize, bitmapSize,
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
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
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        c.drawPath(p, paint);
        pentagonImage = b;

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
        coordinateSystem.setSize(canvas.getWidth(), canvas.getHeight());

        // Draw each PentagonAnimation.
        for (PentagonAnimation animation : animations) {
            Pentagon pentagon = animation.step();
            pentagon.draw(canvas, pentagonImage, coordinateSystem);
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
