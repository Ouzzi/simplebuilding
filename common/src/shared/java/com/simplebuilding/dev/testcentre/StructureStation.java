package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Strukturen (TZ22-3, Besitzer 22.10.: "jede eigene Struktur und jedes Strukturteil einmal erzeugen, mit allen Variationen
 * … die einzelnen Doerfer, Haeuser … fuer die Crucibles … die zusaetzlichen End-Strukturen"): jedes Teil eines Template-Pools,
 * dessen Vorlage zur Familie gehoert ({@link TcContext#isFamily}), steht einmal unrotiert auf dem Boden, mit Schild davor.
 *
 * <p>Quelle sind die Register, keine Liste: alle Template-Pools (auch Vanilla-Pools, in die ein Modul Teile haengt, etwa
 * die Feldkuechen von Simple Crucibles in den Dorfhaeusern) und die Strukturen, die einen Pool als Start haben. Gruppen: je
 * Familien-Pool eine (Titel: die Struktur dazu), fremde Pools je Namensraum der Vorlage und Pool-Wurzel. In einer Gruppe
 * stehen Varianten einer Form (gleicher Name bis auf das letzte {@code _x}) in einer Reihe; die Gruppen werden dicht
 * gepackt (groesste zuerst, kleinere in Spalten daneben), hoechstens {@link TestCentreLayout#MAX_ROW_WIDTH} breit.
 */
public final class StructureStation {

    /** Abschnitt und Bezirk. */
    public static final String SECTION = "structures";
    /** Abstand zwischen Teilen einer Reihe, zwischen Reihen und zwischen Gruppen. */
    static final int GAP = 2;
    static final int GROUP_GAP = 4;
    /** Eine Reihe einer Gruppe wird hoechstens so breit, sonst bricht sie um. */
    static final int GROUP_ROW_WIDTH = 64;
    /** Schild (z 0) und Pfosten (z 1) vor jedem Teil; das Teil beginnt bei z 2. */
    static final int LABEL_DEPTH = 2;

    /** Ein Strukturteil: Vorlage, Groesse, Rahmen-Entities. */
    public record Piece(Identifier template, Vec3i size, int frames) {
    }

    /** Eine Gruppe: Schluessel (sortiert), Titel fuers Schild, Teile in Reihenfolge. */
    public record Group(String key, String title, List<Piece> pieces) {
    }

    private StructureStation() {
    }

    /** Die Vorlagen-Ids aller Familien-Teile je Pool (Pool-Id → Vorlagen, sortiert); ohne Register-Zugriff leer. */
    public static Map<Identifier, Set<Identifier>> familyPieces(HolderLookup.Provider lookup) {
        Map<Identifier, Set<Identifier>> out = new TreeMap<>();
        Optional<? extends HolderLookup.RegistryLookup<StructureTemplatePool>> pools = lookup.lookup(Registries.TEMPLATE_POOL);
        if (pools.isEmpty()) {
            return out;
        }
        pools.get().listElements().forEach(ref -> {
            // getShuffledTemplates sieht auch Teile, die ein Modul nachtraeglich einhaengt (die Rohliste ist oft unveraenderlich).
            for (StructurePoolElement element : ref.value().getShuffledTemplates(RandomSource.create(0L))) {
                if (element instanceof SinglePoolElement single && TcContext.isFamily(single.getTemplateLocation())) {
                    out.computeIfAbsent(ref.key().identifier(), key -> new TreeSet<>()).add(single.getTemplateLocation());
                }
            }
        });
        return out;
    }

    /** Familien-Strukturen und ihr Startpool (leer, wenn die Struktur kein Jigsaw ist). */
    public static Map<Identifier, Optional<Identifier>> familyStructures(HolderLookup.Provider lookup) {
        Map<Identifier, Optional<Identifier>> out = new TreeMap<>();
        Optional<? extends HolderLookup.RegistryLookup<Structure>> structures = lookup.lookup(Registries.STRUCTURE);
        if (structures.isEmpty()) {
            return out;
        }
        structures.get().listElements().forEach(ref -> {
            if (TcContext.isFamily(ref.key().identifier())) {
                out.put(ref.key().identifier(), ref.value() instanceof JigsawStructure jigsaw
                        ? jigsaw.getStartPool().unwrapKey().map(ResourceKey::identifier) : Optional.empty());
            }
        });
        return out;
    }

    /** Die Gruppen in Planungsreihenfolge; ohne Vorlagen-Zugriff (nur Register) leer. */
    public static List<Group> groups(TcContext ctx) {
        StructureTemplateManager templates = ctx.templates();
        if (templates == null) {
            return List.of();
        }
        Map<Identifier, List<Identifier>> startedBy = new TreeMap<>();
        familyStructures(ctx.lookup()).forEach((structure, pool) -> pool.ifPresent(
                id -> startedBy.computeIfAbsent(id, key -> new ArrayList<>()).add(structure)));
        Map<String, String> titles = new TreeMap<>();
        Map<String, Set<Identifier>> members = new TreeMap<>();
        familyPieces(ctx.lookup()).forEach((pool, pieces) -> {
            String key;
            String title;
            if (TcContext.isFamily(pool)) {
                key = pool.toString();
                List<Identifier> structures = startedBy.getOrDefault(pool, List.of());
                title = structures.isEmpty() ? pool.getPath() : structures.stream().map(Identifier::getPath)
                        .reduce((a, b) -> a + ", " + b).orElseThrow();
            } else {
                // Teile in fremden Pools (Dorfhaeuser): je Namensraum der Vorlage und Pool-Wurzel eine Gruppe.
                String root = pool.getPath().split("/")[0];
                key = pieces.iterator().next().getNamespace() + ":" + pool.getNamespace() + "/" + root;
                title = root;
            }
            titles.putIfAbsent(key, title);
            members.computeIfAbsent(key, k -> new TreeSet<>()).addAll(pieces);
        });
        List<Group> out = new ArrayList<>();
        members.forEach((key, ids) -> {
            List<Piece> pieces = new ArrayList<>();
            for (Identifier id : ids) {
                templates.get(id).ifPresent(template -> pieces.add(new Piece(id, template.getSize(), frameEntities(template))));
            }
            if (!pieces.isEmpty()) {
                out.add(new Group(key, titles.get(key), pieces));
            }
        });
        return out;
    }

    /** Rahmen-Entities einer Vorlage (das Wrack mit Elytra bringt einen mit). */
    static int frameEntities(StructureTemplate template) {
        CompoundTag tag = template.save(new CompoundTag());
        return (int) tag.getListOrEmpty("entities").compoundStream()
                .map(entity -> entity.getCompoundOrEmpty("nbt").getStringOr("id", ""))
                .filter(id -> id.equals("minecraft:item_frame") || id.equals("minecraft:glow_item_frame"))
                .count();
    }

    /** Bloecke einer Vorlage, die nach dem Setzen sicher nicht Luft sind (ohne Luft, Strukturleere, Jigsaw- und Strukturbloecke). */
    public static int solidBlocks(StructureTemplate template) {
        CompoundTag tag = template.save(new CompoundTag());
        net.minecraft.nbt.ListTag palette = tag.getListOrEmpty("palette");
        if (palette.size() == 0) {
            palette = tag.getListOrEmpty("palettes").size() == 0 ? palette
                    : (net.minecraft.nbt.ListTag) tag.getListOrEmpty("palettes").get(0);
        }
        Set<String> skip = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void",
                "minecraft:jigsaw", "minecraft:structure_block");
        List<Boolean> solid = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            solid.add(!skip.contains(palette.getCompoundOrEmpty(i).getStringOr("Name", "minecraft:air")));
        }
        return (int) tag.getListOrEmpty("blocks").compoundStream()
                .filter(block -> {
                    int state = block.getIntOr("state", -1);
                    return state >= 0 && state < solid.size() && solid.get(state);
                })
                .count();
    }

    /** Der Abschnitt: die Gruppen dicht gepackt; leer ohne Strukturen. */
    static TcCanvas build(TcContext ctx) {
        List<TcCanvas> groups = new ArrayList<>();
        for (Group group : groups(ctx)) {
            groups.add(group(group));
        }
        TcCanvas c = new TcCanvas();
        // Groesste Gruppe zuerst; sie bestimmt die Tiefe eines Regals, kleinere stapeln sich in Spalten daneben.
        groups.sort(Comparator.comparingInt((TcCanvas g) -> -g.depth()).thenComparingInt(g -> -g.width()));
        int shelfZ = 0;
        int shelfDepth = 0;
        int columnX = 0;
        int columnWidth = 0;
        int columnZ = 0;
        for (TcCanvas group : groups) {
            if (shelfDepth == 0) {
                shelfDepth = group.depth();
            } else if (columnZ + group.depth() <= shelfDepth && group.width() <= columnWidth) {
                // stapelt sich in der Spalte unter die vorige Gruppe
            } else if (columnX + columnWidth + GROUP_GAP + group.width() <= TestCentreLayout.MAX_ROW_WIDTH
                    && group.depth() <= shelfDepth) {
                columnX += columnWidth + GROUP_GAP;
                columnWidth = 0;
                columnZ = 0;
            } else {
                shelfZ += shelfDepth + GROUP_GAP;
                shelfDepth = group.depth();
                columnX = 0;
                columnWidth = 0;
                columnZ = 0;
            }
            c.append(group, columnX, shelfZ + columnZ);
            columnWidth = Math.max(columnWidth, group.width());
            columnZ += group.depth() + GROUP_GAP;
        }
        return c;
    }

    /**
     * Eine Gruppe: je Teil vorn ein Schild an einem Pfosten (Struktur, Variante, Namensraum, Groesse), dahinter das Teil. Eine
     * neue Reihe beginnt, wenn eine Form mit mehreren Varianten anfaengt oder endet, oder bei {@link #GROUP_ROW_WIDTH}.
     */
    static TcCanvas group(Group group) {
        TcCanvas c = new TcCanvas();
        List<List<Piece>> rows = new ArrayList<>();
        Map<String, Integer> perShape = new LinkedHashMap<>();
        group.pieces().forEach(piece -> perShape.merge(shape(piece.template()), 1, Integer::sum));
        List<Piece> row = new ArrayList<>();
        String rowShape = null;
        int rowWidth = 0;
        for (Piece piece : group.pieces()) {
            String shape = shape(piece.template());
            boolean shapeBreak = rowShape != null && !shape.equals(rowShape)
                    && (perShape.get(shape) > 1 || perShape.get(rowShape) > 1);
            boolean tooWide = !row.isEmpty() && rowWidth + GAP + piece.size().getX() > GROUP_ROW_WIDTH;
            if (shapeBreak || tooWide) {
                rows.add(row);
                row = new ArrayList<>();
                rowWidth = 0;
            }
            rowWidth += (row.isEmpty() ? 0 : GAP) + piece.size().getX();
            row.add(piece);
            rowShape = shape;
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        boolean lastSegmentShared = group.pieces().size() > 1
                && group.pieces().stream().map(piece -> last(piece.template().getPath())).distinct().count() == 1;
        int z = 0;
        for (List<Piece> pieces : rows) {
            int x = 0;
            int depth = 0;
            for (Piece piece : pieces) {
                // Schild an einem Pfosten vor dem Teil (Pfosten z + 1, Schild z, Blick nach Norden zum Gang).
                c.wallSign(x, 0, z + 1, TcText.bold(TcText.lit(TestCentreSections.pretty(group.title()))),
                        TcText.lit(variant(piece.template(), lastSegmentShared)),
                        TcText.lit(piece.template().getNamespace()),
                        TcText.lit(piece.size().getX() + "x" + piece.size().getY() + "x" + piece.size().getZ()));
                c.structure(x, 0, z + LABEL_DEPTH, piece.template(), piece.size(), piece.frames());
                x += piece.size().getX() + GAP;
                depth = Math.max(depth, piece.size().getZ());
            }
            z += LABEL_DEPTH + depth + GAP;
        }
        return c;
    }

    /** Form eines Teils: der letzte Pfadteil ohne sein letztes {@code _x} ({@code end/wreck_bow_elytra} → {@code wreck_bow}). */
    static String shape(Identifier template) {
        String name = last(template.getPath());
        int cut = name.lastIndexOf('_');
        return cut > 0 ? name.substring(0, cut) : name;
    }

    /** Was auf dem Schild als Variante steht: der letzte Pfadteil, bei gleichen letzten Teilen (Dorftypen) der davor. */
    static String variant(Identifier template, boolean lastSegmentShared) {
        String[] parts = template.getPath().split("/");
        String name = lastSegmentShared && parts.length > 1 ? parts[parts.length - 2] : parts[parts.length - 1];
        return name.replace('_', ' ');
    }

    private static String last(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
