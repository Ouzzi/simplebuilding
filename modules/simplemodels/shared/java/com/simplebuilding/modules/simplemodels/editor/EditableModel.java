package com.simplebuilding.modules.simplemodels.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Editor stage 1 (b-models12): one block/item model as the vanilla 26.3 cuboid format describes it
 * ({@code CuboidModel}, {@code CuboidModelElement}, {@code CuboidFace}, {@code ItemTransform}). Immutable records, so an
 * undo step is just the previous value ({@link EditHistory}). No client or Minecraft imports: the same data model runs in
 * server gametests and, later, in the editor screen. Reading/writing/validating: {@link ModelJson}.
 *
 * @param parent       {@code parent} model id or {@code null}
 * @param textures     texture variables ({@code side -> minecraft:block/oak_planks} or {@code #other}), insertion order kept
 * @param elements     cuboids in draw order
 * @param display      display transforms by vanilla slot name ({@link #DISPLAY_SLOTS})
 * @param ambientOcclusion {@code ambientocclusion} or {@code null} (inherit)
 * @param guiLight     {@code gui_light} ({@code front}/{@code side}) or {@code null} (inherit)
 */
public record EditableModel(String parent, Map<String, String> textures, List<Element> elements,
                            Map<String, Transform> display, Boolean ambientOcclusion, String guiLight) {
    /** Face order of the vanilla {@code Direction} enum; also the write order. */
    public static final List<String> DIRECTIONS = List.of("down", "up", "north", "south", "west", "east");
    public static final List<String> AXES = List.of("x", "y", "z");
    public static final List<String> DISPLAY_SLOTS = List.of("thirdperson_righthand", "thirdperson_lefthand",
            "firstperson_righthand", "firstperson_lefthand", "head", "gui", "ground", "fixed", "on_shelf");
    public static final EditableModel EMPTY = new EditableModel("minecraft:block/block", Map.of(), List.of(), Map.of(), null, null);

    public EditableModel {
        textures = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(textures));
        elements = List.copyOf(elements);
        display = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(display));
    }

    public record Vec(float x, float y, float z) {
        public static final Vec ZERO = new Vec(0, 0, 0), ONE = new Vec(1, 1, 1);
        public float get(int axis) { return axis == 0 ? x : axis == 1 ? y : z; }
        public Vec add(Vec o) { return new Vec(x + o.x, y + o.y, z + o.z); }
    }

    /**
     * Element rotation around {@code origin} (block pixels): single axis ({@code axis} + {@code angle}, any angle in 26.x)
     * or, when {@code euler} is set, vanilla {@code x}/{@code y}/{@code z} degrees ({@code EulerXYZRotation}).
     */
    public record Rotation(Vec origin, String axis, float angle, Vec euler, boolean rescale) {
        public static Rotation single(Vec origin, String axis, float angle, boolean rescale) { return new Rotation(origin, axis, angle, null, rescale); }
        public static Rotation euler(Vec origin, Vec degrees, boolean rescale) { return new Rotation(origin, null, 0, degrees, rescale); }
        public boolean isEuler() { return euler != null; }
    }

    /** UV rectangle in texture pixels 0..16 (min may exceed max: vanilla mirrors). */
    public record Uv(float u1, float v1, float u2, float v2) {}

    /**
     * One face. {@code uv == null} = vanilla auto-UV from the element bounds; {@code cullface}/{@code tintIndex -1} = none.
     */
    public record Face(String texture, Uv uv, int rotation, String cullface, int tintIndex) {
        public static Face of(String texture) { return new Face(texture, null, 0, null, -1); }
    }

    /**
     * One cuboid. {@code name} is editor data (Blockbench writes the same field; vanilla ignores it).
     * {@code faces} keyed by {@link #DIRECTIONS} name, 1..6 entries.
     */
    public record Element(String name, Vec from, Vec to, Rotation rotation, String shadeDirection, int lightEmission,
                          Map<String, Face> faces) {
        public Element {
            var ordered = new LinkedHashMap<String, Face>();
            for (String d : DIRECTIONS) if (faces.containsKey(d)) ordered.put(d, faces.get(d));
            faces.forEach(ordered::putIfAbsent);
            faces = java.util.Collections.unmodifiableMap(ordered);
        }
        /** A plain cube with all six faces on one texture variable. */
        public static Element cube(String name, Vec from, Vec to, String texture) {
            var faces = new LinkedHashMap<String, Face>();
            for (String d : DIRECTIONS) faces.put(d, Face.of(texture));
            return new Element(name, from, to, null, null, 0, faces);
        }
        public Element withBounds(Vec from, Vec to) { return new Element(name, from, to, rotation, shadeDirection, lightEmission, faces); }
        public Element withRotation(Rotation r) { return new Element(name, from, to, r, shadeDirection, lightEmission, faces); }
        public Element withName(String n) { return new Element(n, from, to, rotation, shadeDirection, lightEmission, faces); }
        /** This element with face {@code direction} replaced ({@code null} removes it). */
        public Element withFace(String direction, Face face) {
            var map = new LinkedHashMap<>(faces);
            if (face == null) map.remove(direction); else map.put(direction, face);
            return new Element(name, from, to, rotation, shadeDirection, lightEmission, map);
        }
    }

    /** Display transform; {@code translation} in JSON pixels (vanilla scales by 1/16 and clamps to ±80 px). */
    public record Transform(Vec rotation, Vec translation, Vec scale, Vec rightRotation) {
        public static final Transform IDENTITY = new Transform(Vec.ZERO, Vec.ZERO, Vec.ONE, Vec.ZERO);
    }

    public EditableModel withElements(List<Element> list) { return new EditableModel(parent, textures, list, display, ambientOcclusion, guiLight); }
    public EditableModel withElement(int index, Element element) {
        var list = new ArrayList<>(elements); list.set(index, element); return withElements(list);
    }
    public EditableModel addElement(Element element) {
        var list = new ArrayList<>(elements); list.add(element); return withElements(list);
    }
    public EditableModel removeElement(int index) {
        var list = new ArrayList<>(elements); list.remove(index); return withElements(list);
    }
    public EditableModel withTexture(String variable, String value) {
        var map = new LinkedHashMap<>(textures);
        if (value == null) map.remove(variable); else map.put(variable, value);
        return new EditableModel(parent, map, elements, display, ambientOcclusion, guiLight);
    }
}
