package com.jitsik.pentagons;

import android.os.SystemClock;

/**
 * This represents a moving pentagon.
 */
public class Pentagon {

    private PentagonState start;
    private PentagonState end;
    private PentagonState tempState = new PentagonState();
    private long startTime = 0;
    private long duration = 0;

    /**
     * Create a new animation with a starting state.
     * @param initial The starting state.
     */
    public Pentagon(PentagonState initial) {
        start = initial.copy();
        end = initial.copy();
    }

    /**
     * Begin the next animation.
     * @param next The state to animate to.
     * @param seconds The animation's duration, in seconds.
     */
    public void begin(PentagonState next, float seconds) {
        duration = (long)(seconds * 1000.0f);
        startTime = SystemClock.elapsedRealtime();
        start = end;
        end = next.copy();
    }

    /**
     * Tell if the current animation is done.
     * @return true if the animation is done, false otherwise.
     */
    public boolean done() {
        long elapsed = SystemClock.elapsedRealtime() - startTime;
        return elapsed >= duration;
    }

    /**
     * Get the current frame.
     * @return This returns a temporary PentagonState which represents the
     * current spot in the current animation.
     */
    public PentagonState frame() {
        // Compute the fraction of the animation that has passed.
        float percentage = 1.0f;
        long elapsed = SystemClock.elapsedRealtime() - this.startTime;
        if (elapsed < duration) {
            percentage = (float)elapsed / (float)this.duration;
        }

        // Take the intermediate value of every attribute.
        tempState.position.x = start.position.x +
                ((end.position.x - start.position.x) * percentage);
        tempState.position.y = start.position.y +
                ((end.position.y - start.position.y) * percentage);
        tempState.angle = ((this.end.angle - this.start.angle) * percentage) +
                this.start.angle;
        tempState.radius = this.start.radius +
                ((this.end.radius - this.start.radius) * percentage);
        tempState.opacity = this.start.opacity +
                ((this.end.opacity - this.start.opacity) * percentage);

        if (tempState.angle < 0) {
            tempState.angle += Math.PI * 2;
        } else if (tempState.angle > Math.PI * 2) {
            tempState.angle -= Math.PI * 2;
        }

        return tempState;
    }

}
