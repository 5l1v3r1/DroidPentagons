package com.jitsik.pentagons;

/**
 * This represents the state of a pentagon at a given instant.
 */
public class PentagonState {

    Vector position = new Vector(0, 0);
    float angle = 0;
    float radius = 0;
    float opacity = 0;

    public void addForceFrom(PentagonState p, Vector f) {
        // Use an inverse-square relationship to compute a force.
        float d2 = Vector.distanceSquared(this.position, p.position);
        float distance = (float)Math.sqrt(d2);

        // If we're too close, the force will be too strong.
        if (distance < 0.001f) {
            return;
        }

        // Compute the components of the force vector.
        float magnitude = 1.0f / d2;
        float xComp = (position.x - p.position.x) / distance;
        float yComp = (position.y - p.position.y) / distance;
        f.x += magnitude * xComp;
        f.y += magnitude * yComp;
    }

    public void addForceFromEdges(Vector f) {
        f.x += 1.0f / Math.pow(0.01 + position.x, 2);
        f.x -= 1.0f / Math.pow(1.01f - position.x, 2);
        f.y += 1.0f / Math.pow(0.01 + position.y, 2);
        f.y -= 1.0f / Math.pow(1.01f - position.y, 2);
    }

    public PentagonState copy() {
        PentagonState result = new PentagonState();
        result.position.x = position.x;
        result.position.y = position.y;
        result.angle = angle;
        result.radius = radius;
        result.opacity = opacity;
        return result;
    }

}
