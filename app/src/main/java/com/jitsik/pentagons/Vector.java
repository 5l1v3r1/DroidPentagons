package com.jitsik.pentagons;

/**
 * Created by Alex Nichol on 2/21/15.
 */
public class Vector {

    public float x = 0;
    public float y = 0;

    public Vector() {
    }

    public Vector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static float distance(Vector v1, Vector v2) {
        return (float)Math.sqrt(distanceSquared(v1, v2));
    }

    public static float distanceSquared(Vector v1, Vector v2) {
        return (float)(Math.pow(v1.x - v2.x, 2) + Math.pow(v1.y - v2.y, 2));
    }

    public void addRandom(float maxComponent) {
        this.x += (Math.random() * maxComponent * 2.0f) - maxComponent;
        this.y += (Math.random() * maxComponent * 2.0f) - maxComponent;
    }

    public void capMagnitude(float max) {
        float  mag = this.magnitude();
        if (mag > max) {
            this.scale(max/mag);
        }
    }

    public float magnitude() {
        return (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public void scale(float s) {
        x *= s;
        y *= s;
    }

}
