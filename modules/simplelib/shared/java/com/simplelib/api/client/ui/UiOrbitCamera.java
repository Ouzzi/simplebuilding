package com.simplelib.api.client.ui;

import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Orbit camera of a 3D view drawn into the GUI (blueprint editor, Simple Models editor; b-models13 moved it here from
 * {@code BlueprintViewControls} so modules need not depend on each other). Turntable rotation while dragging, pan,
 * mouse-wheel zoom, fixed views, and the orthographic projection both ways (model point to screen, screen point to a ray
 * in model space) for picking and gizmos.
 *
 * <p>Pure logic: only JOML (part of Minecraft on both sides), no client classes, so the whole camera is tested in a
 * server gametest ({@code orbit_camera}). The view looks along -z: after {@link #rotation()} a point with a larger z lies
 * nearer to the viewer; screen x grows to the right, screen y downwards (model y up).
 */
public class UiOrbitCamera {
    /** Radians per GUI pixel while dragging: horizontal turns about the model's up axis, vertical tilts about the screen's x axis. */
    public static final float DRAG_RAD_PER_PIXEL = 0.012f;
    /** Zoom factor per mouse-wheel step. */
    public static final double ZOOM_STEP = 1.15;
    /** Zoom limits: at the bottom the model still fits, at the top it stays recognisable. */
    public static final double ZOOM_MIN = 0.2;
    public static final double ZOOM_MAX = 12.0;
    /** Largest pan in GUI pixels so the view cannot wander out of its window. */
    public static final float PAN_LIMIT = 96f;
    /** Tilt and turn of the default view: slightly from above, front right onto the south side. */
    public static final float DEFAULT_PITCH = (float) Math.toRadians(28);
    public static final float DEFAULT_YAW = (float) Math.toRadians(-35);
    /** Tilt stays between straight from above and straight from below, so nothing flips over. */
    public static final float PITCH_LIMIT = (float) (Math.PI / 2);

    /** Fixed views (Blender/Blockbench number pad 1/3/7): front looks at the south face, right at the east face. */
    public enum View {
        DEFAULT(DEFAULT_PITCH, DEFAULT_YAW), FRONT(0f, 0f), RIGHT(0f, (float) (-Math.PI / 2)), TOP(PITCH_LIMIT, 0f);
        public final float pitch, yaw;
        View(float pitch, float yaw) { this.pitch = pitch; this.yaw = yaw; }
    }

    /** A ray in model space (relative to the view centre): {@code origin + t * direction}, direction normalised. */
    public record Ray(Vector3f origin, Vector3f direction) {}

    private float pitch = DEFAULT_PITCH;
    private float yaw = DEFAULT_YAW;
    private final Quaternionf rotation = defaultRotation();
    private float zoom = 1f;
    private float panX;
    private float panY;

    /** Default view; the single source for this angle. */
    public static Quaternionf defaultRotation() {
        return orbit(DEFAULT_PITCH, DEFAULT_YAW);
    }

    /** First turn about the model's up axis, then tilt about the screen's x axis. */
    public static Quaternionf orbit(float pitch, float yaw) {
        return new Quaternionf().rotateX(pitch).rotateY(yaw);
    }

    public Quaternionf rotation() { return rotation; }
    public float zoom() { return zoom; }
    public float panX() { return panX; }
    public float panY() { return panY; }
    public float pitch() { return pitch; }
    public float yaw() { return yaw; }

    /**
     * Drag: a turntable, not a ball (blueprint N40). Horizontal turns only about the model's up axis, vertical only tilts;
     * both angles are kept separately, so small jitter never adds up to a roll about the view axis.
     */
    public void drag(double dx, double dy) {
        yaw = (float) ((yaw + dx * DRAG_RAD_PER_PIXEL) % (2 * Math.PI));
        pitch = Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, (float) (pitch + dy * DRAG_RAD_PER_PIXEL)));
        rotation.set(orbit(pitch, yaw));
    }

    /** Pan: the view follows the pointer; the rotation stays. */
    public void pan(double dx, double dy) {
        panX = clamp(panX + (float) dx);
        panY = clamp(panY + (float) dy);
    }

    /** Mouse wheel over the view: up zooms in, down out. */
    public void zoom(double scrollY) {
        zoom = (float) Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * Math.pow(ZOOM_STEP, scrollY)));
    }

    /** Jumps to a fixed view; zoom and pan stay. */
    public void view(View view) {
        pitch = view.pitch;
        yaw = view.yaw;
        rotation.set(orbit(pitch, yaw));
    }

    /** Rotation, zoom and pan back to the start. */
    public void reset() {
        view(View.DEFAULT);
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    /** Whether the view already stands exactly as it starts. */
    public boolean isDefault() {
        return rotation.equals(defaultRotation()) && zoom == 1f && panX == 0f && panY == 0f;
    }

    /** GUI pixels per model unit when a sphere of {@code radius} fills the {@code w x h} view at zoom 1. */
    public float scale(int w, int h, float radius) {
        return zoom * Math.min(w, h) * 0.5f / Math.max(1.0e-4f, radius);
    }

    /** Screen position of the model point {@code p} (relative to the view centre) in the view {@code x,y,w,h}. */
    public Vector2f project(Vector3fc p, int x, int y, int w, int h, float radius) {
        Vector3f v = rotation.transform(new Vector3f(p));
        float s = scale(w, h, radius);
        return new Vector2f(x + w / 2f + panX + v.x * s, y + h / 2f + panY - v.y * s);
    }

    /** Depth of {@code p} after the rotation: larger = nearer to the viewer. */
    public float depth(Vector3fc p) {
        return rotation.transform(new Vector3f(p)).z;
    }

    /**
     * The ray under the screen point {@code sx, sy} in model space (relative to the view centre). Orthographic: every ray
     * runs along the view axis; it starts in front of everything ({@code 4 * radius} towards the viewer).
     */
    public Ray ray(double sx, double sy, int x, int y, int w, int h, float radius) {
        float s = scale(w, h, radius);
        float vx = (float) ((sx - (x + w / 2f + panX)) / s);
        float vy = (float) (-(sy - (y + h / 2f + panY)) / s);
        Quaternionf inverse = new Quaternionf(rotation).conjugate();
        Vector3f origin = inverse.transform(new Vector3f(vx, vy, 4 * radius));
        Vector3f direction = inverse.transform(new Vector3f(0, 0, -1)).normalize();
        return new Ray(origin, direction);
    }

    /** Screen direction (GUI pixels) of one model unit along {@code axis}; drags project onto it (gizmo). */
    public Vector2f screenAxis(Vector3fc axis, int w, int h, float radius) {
        Vector3f v = rotation.transform(new Vector3f(axis));
        float s = scale(w, h, radius);
        return new Vector2f(v.x * s, -v.y * s);
    }

    private static float clamp(float value) {
        return Math.max(-PAN_LIMIT, Math.min(PAN_LIMIT, value));
    }
}
