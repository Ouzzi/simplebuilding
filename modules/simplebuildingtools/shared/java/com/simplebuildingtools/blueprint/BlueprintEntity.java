package com.simplebuildingtools.blueprint;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * N77: eine Entity der Blaupause - Item-Frame/Leucht-Rahmen, Ruestungsstaender oder KI-loser Mob - als eine Zeile der Bausprache
 * (Spezifikation {@code docs/BLUEPRINT.md} 1.10, Plan {@code docs/ai/PLAN-N77-SCAN-ENTITIES-2026-11-16.md}):
 * <pre>
 * &#64;item_frame 1,1,2 north diamond_sword[enchantments={sharpness:5},custom_name="Klinge"] rot=2 invisible fixed
 * &#64;armor_stand 3.5,1,3.5 yaw=90 arms head=iron_helmet head_pose=-10,0,0
 * &#64;zombie 0.5,1,3.5 yaw=180 noai silent name="Bob"
 * </pre>
 * Items stehen in der Befehlssyntax von {@code /give} ({@code id[komponente=SNBT,!komponente]*anzahl}); geschrieben ueber den
 * Komponenten-Codec mit Registry-Kontext, gelesen beim Bauen mit {@link ItemParser} und den Registries der Welt. Der Parser der
 * Bausprache (ohne Welt) prueft Ids, Komponenten-Namen und SNBT-Syntax. Felder sind kanonisch: {@link #line()} ist eindeutig.
 *
 * @param type     Entity-Typ
 * @param x        Position im Raster der Blaupause (Item-Frame: Blockstelle des Frames, ganzzahlig)
 * @param facing   Richtung des Frames, sonst {@code null}
 * @param item     Item im Frame (Befehlssyntax) oder {@code null}
 * @param rotation Drehung des Frame-Items 0..7
 * @param flags    Schalter aus {@link #FLAGS}, in deren Reihenfolge
 * @param values   {@code name}, Ausruestung und Posen (Schluessel aus {@link #KEYS})
 */
public record BlueprintEntity(Identifier type, Kind kind, double x, double y, double z, @Nullable Direction facing,
                              @Nullable String item, int rotation, double yaw, List<String> flags, Map<String, String> values) {

    /** Hoechstens so viele Entities in einem Code (Scan und Parser). */
    public static final int MAX_ENTITIES = 4096;
    /** Laengster Item- oder Namenstext einer Entity-Zeile in Zeichen. */
    public static final int MAX_VALUE_CHARS = 8192;
    /** Hoechste Stueckzahl eines Items ({@code *n}). */
    public static final int MAX_COUNT = 99;

    public enum Kind { FRAME, STAND, MOB }

    /** Schalter in Schreib-Reihenfolge; daneben der NBT-Schluessel, mit dem Vanilla ihn speichert. */
    public static final List<String> FLAGS = List.of("invisible", "fixed", "invulnerable", "silent", "nogravity", "noai",
            "small", "arms", "noplate", "marker", "showname");
    private static final Map<String, String> FLAG_TAGS = Map.ofEntries(Map.entry("invisible", "Invisible"), Map.entry("fixed", "Fixed"),
            Map.entry("invulnerable", "Invulnerable"), Map.entry("silent", "Silent"), Map.entry("nogravity", "NoGravity"),
            Map.entry("noai", "NoAI"), Map.entry("small", "Small"), Map.entry("arms", "ShowArms"), Map.entry("noplate", "NoBasePlate"),
            Map.entry("marker", "Marker"), Map.entry("showname", "CustomNameVisible"));
    private static final List<String> FRAME_FLAGS = List.of("invisible", "fixed", "invulnerable", "silent");
    /** Ruestungsstaender und Mobs teilen die Schalter (ein fremder Schalter wird beim Laden einfach nicht gelesen). */
    private static final List<String> LIVING_FLAGS = List.of("invisible", "invulnerable", "silent", "nogravity", "noai", "small", "arms",
            "noplate", "marker", "showname");

    /** Ausruestungsplaetze in Schreib-Reihenfolge. */
    public static final List<String> SLOTS = List.of("head", "chest", "legs", "feet", "hand", "offhand");
    private static final EquipmentSlot[] SLOT_TYPES = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    /** Posen des Ruestungsstaenders in Schreib-Reihenfolge (nur abweichend vom Standard geschrieben). */
    public static final List<String> POSES = List.of("head_pose", "body_pose", "left_arm_pose", "right_arm_pose", "left_leg_pose",
            "right_leg_pose");
    /** Alle {@code key=wert}-Schluessel ausser {@code rot}/{@code yaw}, in Schreib-Reihenfolge. */
    public static final List<String> KEYS;

    static {
        List<String> keys = new ArrayList<>();
        keys.add("name");
        keys.addAll(SLOTS);
        keys.addAll(POSES);
        KEYS = List.copyOf(keys);
    }

    public BlueprintEntity {
        flags = List.copyOf(flags);
        values = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    // =====================================================================================
    // ART UND RASTER
    // =====================================================================================

    /** Arten, die an echten Entities gesehen wurden (Scan, Bau) - genauer als die Vermutung aus der Schreibweise. */
    private static final Map<EntityType<?>, Kind> SEEN = new java.util.concurrent.ConcurrentHashMap<>();

    /** Art einer echten Entity, oder {@code null} (nicht unterstuetzt). */
    public static @Nullable Kind kindOf(Entity entity) {
        Kind kind = entity instanceof ItemFrame ? Kind.FRAME : entity instanceof ArmorStand ? Kind.STAND
                : entity instanceof Mob ? Kind.MOB : null;
        if (kind != null) {
            SEEN.put(entity.getType(), kind);
        }
        return kind;
    }

    /**
     * Art eines Typs ohne Welt (der Parser kann keine Entity erzeugen, {@code EntityType#getBaseClass} sagt auf 26.3 nichts):
     * schon gesehen, sonst die Vanilla-Typen, sonst nach Schreibweise - mit Richtung ein Frame, ein Modul-Typ der Kategorie
     * MISC ein Stand, sonst ein Mob. Der Bau prueft an der echten Entity nach (Frame gegen Nicht-Frame).
     */
    static Kind guessKind(EntityType<?> type, boolean hasDirection) {
        Kind seen = SEEN.get(type);
        if (seen != null) {
            return seen;
        }
        if (type == net.minecraft.world.entity.EntityTypes.ITEM_FRAME || type == net.minecraft.world.entity.EntityTypes.GLOW_ITEM_FRAME
                || hasDirection) {
            return Kind.FRAME;
        }
        if (type == net.minecraft.world.entity.EntityTypes.ARMOR_STAND || (!"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())
                && type.getCategory() == net.minecraft.world.entity.MobCategory.MISC)) {
            return Kind.STAND;
        }
        return Kind.MOB;
    }

    public int cellX() {
        return Mth.floor(x);
    }

    public int cellY() {
        return Mth.floor(y);
    }

    public int cellZ() {
        return Mth.floor(z);
    }

    public BlueprintEntity shifted(int dx, int dy, int dz) {
        return new BlueprintEntity(type, kind, x + dx, y + dy, z + dz, facing, item, rotation, yaw, flags, values);
    }

    /** Gleiche Entities, egal in welcher Reihenfolge (Modellvergleich). */
    static boolean sameSet(Collection<BlueprintEntity> a, Collection<BlueprintEntity> b) {
        if (a.size() != b.size()) {
            return false;
        }
        return sortedLines(a).equals(sortedLines(b));
    }

    static List<String> sortedLines(Collection<BlueprintEntity> entities) {
        List<String> lines = new ArrayList<>();
        for (BlueprintEntity e : entities) {
            lines.add(e.line());
        }
        lines.sort(Comparator.naturalOrder());
        return lines;
    }

    private static List<String> flagsOf(Kind kind) {
        return kind == Kind.FRAME ? FRAME_FLAGS : LIVING_FLAGS;
    }

    // =====================================================================================
    // SCHREIBEN
    // =====================================================================================

    /** Die kanonische Zeile ({@code @typ pos ...}). */
    public String line() {
        StringBuilder sb = new StringBuilder("@").append(shortId(type));
        if (kind == Kind.FRAME) {
            sb.append(' ').append(cellX()).append(',').append(cellY()).append(',').append(cellZ());
            sb.append(' ').append(facing == null ? "up" : facing.getSerializedName());
            if (item != null) {
                sb.append(' ').append(item);
            }
            if (rotation != 0) {
                sb.append(" rot=").append(rotation);
            }
        } else {
            sb.append(' ').append(num(x)).append(',').append(num(y)).append(',').append(num(z));
            if (yaw != 0) {
                sb.append(" yaw=").append(num(yaw));
            }
        }
        for (String key : KEYS) {
            String value = values.get(key);
            if (value != null) {
                sb.append(' ').append(key).append('=').append(value);
            }
        }
        for (String flag : FLAGS) {
            if (flags.contains(flag)) {
                sb.append(' ').append(flag);
            }
        }
        return sb.toString();
    }

    static String shortId(Identifier id) {
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    /** Zahl mit hoechstens 3 Nachkommastellen, ohne nachlaufende Nullen ({@code 4.5}, {@code -10}, {@code 0.125}). */
    static String num(double v) {
        double r = round(v);
        if (r == Math.rint(r)) {
            return Long.toString((long) r);
        }
        return BigDecimal.valueOf(r).stripTrailingZeros().toPlainString();
    }

    static double round(double v) {
        double r = Math.round(v * 1000.0) / 1000.0;
        return r == 0 ? 0 : r; // keine -0
    }

    /**
     * Ein Item in Befehlssyntax: {@code id[komponente=SNBT,...,!komponente]*anzahl}, Komponenten sortiert; {@code null} fuer ein
     * leeres Item. Wirft {@link IllegalArgumentException}, wenn sich die Komponenten nicht schreiben lassen oder der Text zu lang wird.
     */
    public static @Nullable String itemSpec(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(shortId(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        DataComponentPatch patch = stack.getComponentsPatch().forget(DataComponentType::isTransient);
        if (!patch.isEmpty()) {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            Tag encoded = DataComponentPatch.CODEC.encodeStart(ops, patch).getOrThrow(IllegalArgumentException::new);
            if (!(encoded instanceof CompoundTag compound)) {
                throw new IllegalArgumentException("components are no compound: " + encoded);
            }
            List<String> keys = new ArrayList<>(compound.keySet());
            keys.sort(Comparator.naturalOrder());
            List<String> parts = new ArrayList<>();
            for (String key : keys) {
                if (key.startsWith("!")) {
                    parts.add("!" + shortId(key.substring(1)));
                } else {
                    parts.add(shortId(key) + "=" + compound.get(key));
                }
            }
            sb.append('[').append(String.join(",", parts)).append(']');
        }
        if (stack.getCount() != 1) {
            sb.append('*').append(stack.getCount());
        }
        if (sb.length() > MAX_VALUE_CHARS) {
            throw new IllegalArgumentException("item text longer than " + MAX_VALUE_CHARS + " characters");
        }
        return sb.toString();
    }

    /** Ein Name (Textkomponente) als SNBT. */
    static String nameSpec(Component name, HolderLookup.Provider registries) {
        Tag tag = ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), name)
                .getOrThrow(IllegalArgumentException::new);
        String text = tag.toString();
        if (text.length() > MAX_VALUE_CHARS) {
            throw new IllegalArgumentException("name longer than " + MAX_VALUE_CHARS + " characters");
        }
        return text;
    }

    // =====================================================================================
    // SCANNEN
    // =====================================================================================

    /** Nimmt der Scan diese Entity auf? Item-Frames, Ruestungsstaender und Mobs ohne KI (Spieler nie). */
    public static boolean scannable(Entity entity) {
        if (entity instanceof Player || entity.isRemoved()) {
            return false;
        }
        return entity instanceof ItemFrame || entity instanceof ArmorStand || (entity instanceof Mob mob && mob.isNoAi());
    }

    /** Die Blockstelle, an der die Entity steht (Frame: die Stelle, die er belegt). */
    public static BlockPos cellOf(Entity entity) {
        return entity instanceof ItemFrame frame ? frame.getPos() : entity.blockPosition();
    }

    /**
     * Die Entity relativ zu {@code origin} (Ecke der Scan-Box), oder ein Text, warum sie nicht aufgenommen wird
     * (der Scan schreibt ihn als Kommentar in den Code).
     */
    public static Object scan(Entity entity, BlockPos origin, HolderLookup.Provider registries) {
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Kind kind = kindOf(entity);
        if (kind == null || !scannable(entity)) {
            return "unsupported entity " + type;
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        entity.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        List<String> flags = new ArrayList<>();
        for (String flag : flagsOf(kind)) {
            if (saved.getBooleanOr(FLAG_TAGS.get(flag), false)) {
                flags.add(flag);
            }
        }
        Map<String, String> values = new LinkedHashMap<>();
        try {
            if (entity.getCustomName() != null) {
                values.put("name", nameSpec(entity.getCustomName(), registries));
            }
            if (entity instanceof ItemFrame frame) {
                BlockPos cell = frame.getPos().subtract(origin);
                String item = itemSpec(frame.getItem(), registries);
                return new BlueprintEntity(type, kind, cell.getX(), cell.getY(), cell.getZ(), frame.getDirection(), item,
                        Math.floorMod(frame.getRotation(), ItemFrame.NUM_ROTATIONS), 0, flags, values);
            }
            LivingEntity living = (LivingEntity) entity;
            for (int i = 0; i < SLOTS.size(); i++) {
                String spec = itemSpec(living.getItemBySlot(SLOT_TYPES[i]), registries);
                if (spec != null) {
                    values.put(SLOTS.get(i), spec);
                }
            }
            if (entity instanceof ArmorStand stand) {
                ArmorStand.ArmorStandPose pose = stand.getArmorStandPose();
                Rotations[] now = {pose.head(), pose.body(), pose.leftArm(), pose.rightArm(), pose.leftLeg(), pose.rightLeg()};
                Rotations[] def = {ArmorStand.DEFAULT_HEAD_POSE, ArmorStand.DEFAULT_BODY_POSE, ArmorStand.DEFAULT_LEFT_ARM_POSE,
                        ArmorStand.DEFAULT_RIGHT_ARM_POSE, ArmorStand.DEFAULT_LEFT_LEG_POSE, ArmorStand.DEFAULT_RIGHT_LEG_POSE};
                for (int i = 0; i < POSES.size(); i++) {
                    String text = rotations(now[i]);
                    if (!text.equals(rotations(def[i]))) {
                        values.put(POSES.get(i), text);
                    }
                }
            }
        } catch (RuntimeException e) {
            return type + " at " + cellOf(entity).subtract(origin).toShortString() + ": " + e.getMessage();
        }
        Vec3 p = entity.position();
        return new BlueprintEntity(type, kind, round(p.x - origin.getX()), round(p.y - origin.getY()), round(p.z - origin.getZ()), null,
                null, 0, round(Mth.wrapDegrees(entity.getYRot())), flags, values);
    }

    private static String rotations(Rotations r) {
        return num(r.x()) + "," + num(r.y()) + "," + num(r.z());
    }

    // =====================================================================================
    // PARSEN (ohne Welt)
    // =====================================================================================

    /** Meldungen und Einfaerbung des Bausprachen-Parsers. */
    interface Sink {
        void problem(int start, int end, String key, String... args);

        void style(int start, int end, byte style);
    }

    /** Beginn eines Kommentars ({@code #} ausserhalb von Anfuehrungszeichen) in {@code [from, to)}, sonst -1. */
    static int commentStart(String code, int from, int to) {
        char quote = 0;
        for (int i = from; i < to; i++) {
            char c = code.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '#') {
                return i;
            }
        }
        return -1;
    }

    /** Woerter an Leerzeichen ausserhalb von Klammern und Anfuehrungszeichen, als {@code [start, end)}. */
    static List<int[]> tokens(String code, int from, int to) {
        List<int[]> out = new ArrayList<>();
        int i = from;
        while (i < to) {
            while (i < to && Character.isWhitespace(code.charAt(i))) {
                i++;
            }
            if (i >= to) {
                break;
            }
            int start = i;
            int depth = 0;
            char quote = 0;
            while (i < to) {
                char c = code.charAt(i);
                if (quote != 0) {
                    if (c == '\\') {
                        i++;
                    } else if (c == quote) {
                        quote = 0;
                    }
                } else if (c == '"' || c == '\'') {
                    quote = c;
                } else if (c == '[' || c == '{' || c == '(') {
                    depth++;
                } else if (c == ']' || c == '}' || c == ')') {
                    depth = Math.max(0, depth - 1);
                } else if (depth == 0 && Character.isWhitespace(c)) {
                    break;
                }
                i++;
            }
            out.add(new int[]{start, Math.min(i, to)});
        }
        return out;
    }

    /** Eine {@code @}-Zeile in {@code [from, to)} (ohne Kommentar); {@code null} nach gemeldetem Fehler. */
    static @Nullable BlueprintEntity parse(String code, int from, int to, Sink sink) {
        List<int[]> tokens = tokens(code, from, to);
        int[] head = tokens.get(0);
        String idText = code.substring(head[0] + 1, head[1]);
        sink.style(head[0], head[0] + 1, BlueprintCode.STYLE_OPERATOR);
        Identifier id = idText.isEmpty() ? null : Identifier.tryParse(idText);
        Optional<EntityType<?>> type = id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (type.isEmpty()) {
            sink.problem(head[0], head[1], "entity_unknown", idText);
            return null;
        }
        int colon = idText.indexOf(':');
        if (colon >= 0) {
            sink.style(head[0] + 1, head[0] + 2 + colon, BlueprintCode.STYLE_NAMESPACE);
        }
        sink.style(head[0] + 2 + colon, head[1], BlueprintCode.STYLE_BLOCK);
        boolean direction = tokens.size() > 2 && Direction.byName(code.substring(tokens.get(2)[0], tokens.get(2)[1])) != null;
        Kind kind = guessKind(type.get(), direction);
        if (tokens.size() < (kind == Kind.FRAME ? 3 : 2)) {
            sink.problem(head[0], to, "entity_usage", kind == Kind.FRAME ? "@" + idText + " x,y,z <facing> [item]" : "@" + idText + " x,y,z");
            return null;
        }
        boolean failed = false;
        int[] posToken = tokens.get(1);
        String posText = code.substring(posToken[0], posToken[1]);
        sink.style(posToken[0], posToken[1], BlueprintCode.STYLE_NUMBER);
        double[] pos = numbers(posText, kind == Kind.FRAME);
        if (pos == null) {
            sink.problem(posToken[0], posToken[1], "entity_position", posText, String.valueOf(BlueprintCode.GRID - 1));
            return null;
        }
        int next = 2;
        Direction facing = null;
        if (kind == Kind.FRAME) {
            int[] t = tokens.get(2);
            String text = code.substring(t[0], t[1]);
            facing = Direction.byName(text);
            if (facing == null) {
                sink.problem(t[0], t[1], "entity_facing", text);
                return null;
            }
            sink.style(t[0], t[1], BlueprintCode.STYLE_VALUE);
            next = 3;
        }
        String item = null;
        int rotation = 0;
        double yaw = 0;
        List<String> flags = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (int k = next; k < tokens.size(); k++) {
            int[] t = tokens.get(k);
            String text = code.substring(t[0], t[1]);
            int eq = text.indexOf('=');
            int bracket = text.indexOf('[');
            if (eq > 0 && (bracket < 0 || eq < bracket)) {
                String key = text.substring(0, eq);
                String value = text.substring(eq + 1);
                int vs = t[0] + eq + 1;
                sink.style(t[0], t[0] + eq, BlueprintCode.STYLE_PROPERTY);
                sink.style(t[0] + eq, vs, BlueprintCode.STYLE_OPERATOR);
                sink.style(vs, t[1], BlueprintCode.STYLE_VALUE);
                String error = null;
                if (key.equals("rot") && kind == Kind.FRAME) {
                    if (!value.matches("[0-7]")) {
                        error = "entity_value";
                    } else {
                        rotation = Integer.parseInt(value);
                    }
                } else if (key.equals("yaw") && kind != Kind.FRAME) {
                    double[] v = value.matches("-?\\d{1,4}(\\.\\d{1,3})?") ? new double[]{Double.parseDouble(value)} : null;
                    if (v == null || Math.abs(v[0]) > 3600) {
                        error = "entity_value";
                    } else {
                        yaw = round(v[0]);
                    }
                } else if (key.equals("name")) {
                    String[] bad = checkSnbt(value);
                    if (bad != null) {
                        sink.problem(vs, t[1], bad[0], bad[1]);
                        failed = true;
                        continue;
                    }
                    values.put(key, value);
                } else if (SLOTS.contains(key) && kind != Kind.FRAME) {
                    String[] bad = checkItem(value);
                    if (bad != null) {
                        sink.problem(vs, t[1], bad[0], bad[1]);
                        failed = true;
                        continue;
                    }
                    values.put(key, canonicalItem(value));
                } else if (POSES.contains(key) && kind != Kind.FRAME) {
                    double[] v = numbers(value, false, 3600);
                    if (v == null) {
                        error = "entity_value";
                    } else {
                        values.put(key, num(v[0]) + "," + num(v[1]) + "," + num(v[2]));
                    }
                } else {
                    sink.problem(t[0], t[1], "entity_bad_token", text, "@" + idText);
                    failed = true;
                    continue;
                }
                if (error != null) {
                    sink.problem(vs, t[1], error, value, key);
                    failed = true;
                }
                continue;
            }
            if (FLAGS.contains(text)) {
                if (!flagsOf(kind).contains(text)) {
                    sink.problem(t[0], t[1], "entity_bad_token", text, "@" + idText);
                    failed = true;
                } else if (!flags.contains(text)) {
                    flags.add(text);
                    sink.style(t[0], t[1], BlueprintCode.STYLE_PROPERTY);
                }
                continue;
            }
            if (kind == Kind.FRAME && item == null) {
                String[] bad = checkItem(text);
                if (bad != null) {
                    sink.problem(t[0], t[1], bad[0], bad[1]);
                    failed = true;
                    continue;
                }
                int idEnd = bracket < 0 ? t[1] : t[0] + bracket;
                sink.style(t[0], idEnd, BlueprintCode.STYLE_BLOCK);
                sink.style(idEnd, t[1], BlueprintCode.STYLE_VALUE);
                item = canonicalItem(text);
                continue;
            }
            sink.problem(t[0], t[1], "entity_bad_token", text, "@" + idText);
            failed = true;
        }
        if (failed) {
            return null;
        }
        flags.sort(Comparator.comparingInt(FLAGS::indexOf));
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String key : KEYS) {
            if (values.containsKey(key)) {
                ordered.put(key, values.get(key));
            }
        }
        return new BlueprintEntity(id, kind, pos[0], pos[1], pos[2], facing, item, rotation, yaw, flags, ordered);
    }

    /** {@code x,y,z} im Raster ({@code 0 <= v < 256}); ganzzahlig, wenn {@code integers}. */
    private static double @Nullable [] numbers(String text, boolean integers) {
        double[] v = numbers(text, integers, BlueprintCode.GRID);
        if (v == null) {
            return null;
        }
        for (double d : v) {
            if (d < 0 || d >= BlueprintCode.GRID) {
                return null;
            }
        }
        return v;
    }

    private static double @Nullable [] numbers(String text, boolean integers, double limit) {
        String[] parts = text.split(",", -1);
        if (parts.length != 3) {
            return null;
        }
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            if (!parts[i].matches(integers ? "-?\\d{1,4}" : "-?\\d{1,4}(\\.\\d{1,3})?")) {
                return null;
            }
            out[i] = round(Double.parseDouble(parts[i]));
            if (Math.abs(out[i]) > limit) {
                return null;
            }
        }
        return out;
    }

    /** Teilt {@code *n} ab: {@code {basis, n}}; {@code n} = 1 ohne Angabe, -1 bei falscher Zahl. */
    private static Object[] splitCount(String spec) {
        int close = spec.lastIndexOf(']');
        int star = spec.indexOf('*', close < 0 ? 0 : close);
        if (star < 0) {
            return new Object[]{spec, 1};
        }
        String n = spec.substring(star + 1);
        int count = n.matches("\\d{1,2}") ? Integer.parseInt(n) : -1;
        return new Object[]{spec.substring(0, star), count >= 1 && count <= MAX_COUNT ? count : -1};
    }

    /** Kanonische Schreibweise eines (gueltigen) Items: {@code *1} faellt weg. */
    static String canonicalItem(String spec) {
        Object[] split = splitCount(spec);
        return (int) split[1] == 1 ? (String) split[0] : spec;
    }

    /** Prueft ein Item in Befehlssyntax ohne Welt: {@code null} = in Ordnung, sonst {@code {schluessel, wert}}. */
    static String @Nullable [] checkItem(String spec) {
        if (spec.length() > MAX_VALUE_CHARS) {
            return new String[]{"entity_too_long", String.valueOf(MAX_VALUE_CHARS)};
        }
        Object[] split = splitCount(spec);
        if ((int) split[1] < 0) {
            return new String[]{"entity_item_syntax", spec};
        }
        StringReader reader = new StringReader((String) split[0]);
        try {
            Identifier id = Identifier.read(reader);
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                return new String[]{"entity_item_unknown", id.toString()};
            }
            if (!reader.canRead()) {
                return null;
            }
            reader.expect('[');
            TagParser<Tag> snbt = TagParser.create(NbtOps.INSTANCE);
            while (reader.canRead() && reader.peek() != ']') {
                reader.skipWhitespace();
                boolean removed = reader.canRead() && reader.peek() == '!';
                if (removed) {
                    reader.skip();
                }
                Identifier component = Identifier.read(reader);
                if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(component)) {
                    return new String[]{"entity_component_unknown", component.toString()};
                }
                if (!removed) {
                    reader.skipWhitespace();
                    reader.expect('=');
                    reader.skipWhitespace();
                    snbt.parseAsArgument(reader);
                }
                reader.skipWhitespace();
                if (!reader.canRead() || reader.peek() != ',') {
                    break;
                }
                reader.skip();
            }
            reader.expect(']');
            if (reader.canRead()) {
                return new String[]{"entity_item_syntax", spec};
            }
            return null;
        } catch (CommandSyntaxException e) {
            return new String[]{"entity_item_syntax", e.getMessage()};
        }
    }

    private static String @Nullable [] checkSnbt(String text) {
        if (text.length() > MAX_VALUE_CHARS) {
            return new String[]{"entity_too_long", String.valueOf(MAX_VALUE_CHARS)};
        }
        try {
            TagParser.create(NbtOps.INSTANCE).parseFully(text);
            return null;
        } catch (CommandSyntaxException e) {
            return new String[]{"entity_item_syntax", e.getMessage()};
        }
    }

    // =====================================================================================
    // BAUEN (Server)
    // =====================================================================================

    /** Zaehler der gesetzten Entities fuer das Bau-Ergebnis. */
    public static final class Counts {
        public int placed;
        public int already;
        public int blocked;
        public int missing;
    }

    /** Ein Item aus Befehlssyntax mit den Registries der Welt; {@code null}, wenn es sich nicht lesen laesst. */
    public static @Nullable ItemStack decodeItem(String spec, HolderLookup.Provider registries) {
        try {
            Object[] split = splitCount(spec);
            if ((int) split[1] < 0) {
                return null;
            }
            return new ItemParser(registries).parse(new StringReader((String) split[0])).createItemStack((int) split[1]);
        } catch (CommandSyntaxException | RuntimeException e) {
            com.simplebuildingtools.SimpleBuildingTools.LOG.warn("Blueprint: cannot read item {}: {}", spec, e.getMessage());
            return null;
        }
    }

    static @Nullable Component decodeName(String snbt, HolderLookup.Provider registries) {
        try {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            return ComponentSerialization.CODEC.parse(ops, TagParser.create(NbtOps.INSTANCE).parseFully(snbt)).result().orElse(null);
        } catch (CommandSyntaxException | RuntimeException e) {
            return null;
        }
    }

    private static int steps(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
    }

    /**
     * Drehung des Frame-Items nach einer Drehung des Baus: an der Wand dreht das Item mit dem Frame, am Boden und an der Decke
     * liegt seine Ausrichtung fest in der Welt - je Viertel im Uhrzeigersinn (von oben) +2 (Boden) bzw. -2 (Decke).
     */
    public static int rotatedItemRotation(int rotation, Direction facing, Rotation buildRotation) {
        int quarter = steps(buildRotation);
        return switch (facing) {
            case UP -> Math.floorMod(rotation + 2 * quarter, ItemFrame.NUM_ROTATIONS);
            case DOWN -> Math.floorMod(rotation - 2 * quarter, ItemFrame.NUM_ROTATIONS);
            default -> rotation;
        };
    }

    /**
     * Setzt diese Entity in die Welt. Kreativ: alles gratis und mit allen Schaltern/Posen/Namen. Ueberleben: Frame/Staender kosten
     * ihr Item aus dem Baustab-Vorrat, ein Inhalt (Frame-Item, Ausruestung) wird nur gesetzt, wenn genau dieses Item im Inventar
     * liegt (es wird verbraucht); Schalter, Posen und Namen gibt es nur kreativ (in Vanilla nur per Befehl), Mobs nur kreativ.
     */
    public void place(ServerLevel level, Player player, ItemStack wand, boolean creative, BlueprintBuilder.ModelLayout layout, Counts out) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
        Entity entity = entityType == null ? null : entityType.create(level, EntitySpawnReason.COMMAND);
        Kind actual = entity == null ? null : kindOf(entity);
        if (actual == null || (actual == Kind.FRAME) != (kind == Kind.FRAME)) {
            if (entity != null) {
                entity.discard();
            }
            out.blocked++;
            return;
        }
        if (actual == Kind.MOB && !creative) {
            entity.discard();
            out.missing++;
            return;
        }
        Rotation turn = layout.rotation();
        BlockPos cell;
        Vec3 pos;
        Direction dir = null;
        float worldYaw = (float) Mth.wrapDegrees(yaw + 90.0 * steps(turn));
        if (kind == Kind.FRAME) {
            cell = layout.cellPos(cellX(), cellY(), cellZ());
            pos = Vec3.atCenterOf(cell);
            dir = turn.rotate(facing == null ? Direction.UP : facing);
        } else {
            pos = layout.worldPos(x, y, z);
            cell = BlockPos.containing(pos);
        }
        if (!BlueprintBuilder.mayBuildAt(level, player, cell, Direction.UP, wand)) {
            entity.discard();
            out.blocked++;
            return;
        }
        Direction frameDir = dir;
        List<Entity> there = level.getEntities((Entity) null, new AABB(cell).inflate(0.5), other -> other.getType() == entityType
                && (frameDir != null ? other instanceof ItemFrame f && f.getPos().equals(cell) && f.getDirection() == frameDir
                : other.position().distanceToSqr(pos) < 0.01));
        if (!there.isEmpty()) {
            entity.discard();
            out.already++;
            return;
        }
        if (kind == Kind.FRAME) {
            entity.setPos(pos.x, pos.y, pos.z);
        } else {
            entity.snapTo(pos.x, pos.y, pos.z, worldYaw, 0.0F);
        }
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(saved);
        CompoundTag tag = saved.buildResult();
        if (kind == Kind.FRAME) {
            tag.putByte("Facing", (byte) dir.get3DDataValue());
            tag.putByte("ItemRotation", (byte) rotatedItemRotation(rotation, dir, turn));
        }
        if (creative) {
            for (String flag : flagsOf(actual)) {
                tag.putBoolean(FLAG_TAGS.get(flag), flags.contains(flag));
            }
        }
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        if (entity instanceof LivingEntity living) {
            living.setYBodyRot(worldYaw);
            living.setYHeadRot(worldYaw);
        }
        if (entity instanceof ItemFrame frame && !frame.survives()) {
            entity.discard();
            out.blocked++;
            return;
        }
        if (!creative) {
            ItemStack cost = entity.getPickResult();
            if (cost == null || cost.isEmpty() || !BlueprintBuilder.realSupply(player, wand).take(cost.getItem(), 1)) {
                entity.discard();
                out.missing++;
                return;
            }
        }
        if (creative && values.containsKey("name")) {
            Component name = decodeName(values.get("name"), level.registryAccess());
            if (name != null) {
                entity.setCustomName(name);
            }
        }
        if (entity instanceof ItemFrame frame && item != null) {
            ItemStack stack = content(item, level, player, creative, out);
            if (stack != null) {
                frame.setItem(stack, false);
            }
        }
        if (entity instanceof LivingEntity living) {
            for (int i = 0; i < SLOTS.size(); i++) {
                String spec = values.get(SLOTS.get(i));
                if (spec != null) {
                    ItemStack stack = content(spec, level, player, creative, out);
                    if (stack != null) {
                        living.setItemSlot(SLOT_TYPES[i], stack);
                    }
                }
            }
        }
        if (creative && entity instanceof ArmorStand stand) {
            stand.setArmorStandPose(new ArmorStand.ArmorStandPose(pose("head_pose", ArmorStand.DEFAULT_HEAD_POSE),
                    pose("body_pose", ArmorStand.DEFAULT_BODY_POSE), pose("left_arm_pose", ArmorStand.DEFAULT_LEFT_ARM_POSE),
                    pose("right_arm_pose", ArmorStand.DEFAULT_RIGHT_ARM_POSE), pose("left_leg_pose", ArmorStand.DEFAULT_LEFT_LEG_POSE),
                    pose("right_leg_pose", ArmorStand.DEFAULT_RIGHT_LEG_POSE)));
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        if (!level.addFreshEntity(entity)) {
            out.blocked++;
            return;
        }
        out.placed++;
        if (!creative) {
            wand.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    private Rotations pose(String key, Rotations fallback) {
        String text = values.get(key);
        double[] v = text == null ? null : numbers(text, false, 3600);
        return v == null ? fallback : new Rotations((float) v[0], (float) v[1], (float) v[2]);
    }

    /**
     * Inhalt eines Frames oder Ausruestungsplatzes: kreativ einfach erzeugt; im Ueberleben nur, wenn genau dieses Item (gleiche
     * Komponenten, genug Stueck in einem Platz) im Inventar liegt - es wird abgebucht. Fehlt es, zaehlt die Stelle als Fehlstelle.
     */
    private static @Nullable ItemStack content(String spec, ServerLevel level, Player player, boolean creative, Counts out) {
        ItemStack stack = decodeItem(spec, level.registryAccess());
        if (stack == null || stack.isEmpty()) {
            out.missing++;
            return null;
        }
        if (creative) {
            return stack;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack have = inventory.getItem(slot);
            if (have.getCount() >= stack.getCount() && ItemStack.isSameItemSameComponents(have, stack)) {
                have.shrink(stack.getCount());
                return stack;
            }
        }
        out.missing++;
        return null;
    }
}
