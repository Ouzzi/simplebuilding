package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crafting-Rezepte (Nachtrag 13, Besitzer 06.10.: "ein Crafting-Rezepte-Modul"): jedes Crafting-Rezept der Familie aus dem
 * Live-RecipeManager ({@link TcContext#recipes}) steht als Crafter mit seinem Muster im 3 x 3-Raster (je Zutat das erste
 * Item). Oeffnen zeigt Raster und Ergebnis, ein Redstone-Signal craftet es. Gruppen je Namensraum mit Schild; Doppelreihen
 * mit einem Gang dahinter, hoechstens {@link TestCentreLayout#MAX_ROW_WIDTH} breit. Ohne Server (Planung nur aus den
 * Registern) leer. Zaehlt nicht fuer die Abdeckung: die Zutaten liegen im Crafter, sie werden dort nicht gezeigt.
 */
public final class RecipeStation {

    public static final String SECTION = "recipes";
    /** Breite einer Reihe ohne den Rand. */
    static final int ROW_WIDTH = TestCentreLayout.MAX_ROW_WIDTH - 4;
    /** Abstand der Doppelreihen in z: zwei Crafter tief, ein Gang. */
    static final int ROW_PITCH = 3;

    private RecipeStation() {
    }

    /** Ein gezeigtes Rezept: Id, die neun Felder (leer = frei), das Ergebnis. */
    public record Entry(Identifier id, List<ItemStack> grid, ItemStack result) {
    }

    /** Die Crafting-Rezepte der Familie, die sich legen lassen, nach Namensraum, Ergebnis und Id sortiert. */
    public static List<Entry> entries(TcContext ctx) {
        List<Entry> out = new ArrayList<>();
        for (RecipeHolder<?> holder : ctx.recipes()) {
            Identifier id = holder.id().identifier();
            if (!TcContext.isFamily(id) || !(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }
            List<ItemStack> grid = grid(recipe);
            if (grid == null) {
                continue;
            }
            ItemStack result = result(recipe, grid, null);
            if (!result.isEmpty()) {
                out.add(new Entry(id, grid, result));
            }
        }
        out.sort(Comparator.comparing((Entry e) -> e.id().getNamespace())
                .thenComparing(e -> BuiltInRegistries.ITEM.getKey(e.result().getItem()).toString())
                .thenComparing(e -> e.id().toString()));
        return out;
    }

    /** Ob ein Crafting-Rezept ins Raster gelegt werden kann (Sonderrezepte wie Feuerwerk nicht). */
    public static boolean placeable(CraftingRecipe recipe) {
        PlacementInfo info = recipe.placementInfo();
        return !info.isImpossibleToPlace() && info.ingredients().size() <= 9;
    }

    /**
     * Die neun Felder eines Rezepts (Zeile fuer Zeile), oder null, wenn es sich nicht legen laesst oder eine Zutat kein Item
     * hat (leerer Tag). Geformt: das Muster oben links; sonst die Zutaten der Reihe nach.
     */
    public static List<ItemStack> grid(CraftingRecipe recipe) {
        if (!placeable(recipe)) {
            return null;
        }
        List<ItemStack> grid = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        if (recipe instanceof ShapedRecipe shaped) {
            if (shaped.getWidth() > 3 || shaped.getHeight() > 3) {
                return null;
            }
            List<Optional<Ingredient>> ingredients = shaped.getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                if (ingredients.get(i).isEmpty()) {
                    continue;
                }
                ItemStack stack = first(ingredients.get(i).get());
                if (stack.isEmpty()) {
                    return null;
                }
                grid.set(i / shaped.getWidth() * 3 + i % shaped.getWidth(), stack);
            }
            return grid;
        }
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack stack = first(ingredients.get(i));
            if (stack.isEmpty()) {
                return null;
            }
            grid.set(i, stack);
        }
        return grid;
    }

    /**
     * Das Ergebnis des Rasters, wie der Crafter es baut (leer, wenn das Muster nicht passt). Die Planung hat keine Welt
     * ({@code level} null); Rezepte, die eine brauchen, fallen dann weg - der Test ruft es mit Welt und meldet sie.
     */
    public static ItemStack result(CraftingRecipe recipe, List<ItemStack> grid, net.minecraft.world.level.Level level) {
        CraftingInput input = CraftingInput.ofPositioned(3, 3, grid).input();
        try {
            return recipe.matches(input, level) ? recipe.assemble(input) : ItemStack.EMPTY;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack first(Ingredient ingredient) {
        return ingredient.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** Der Abschnitt: je Namensraum eine Gruppe (Schild, dann die Crafter spaltenweise zu zweit), Reihen mit Gang. */
    public static TcCanvas build(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        List<Entry> entries = entries(ctx);
        if (entries.isEmpty()) {
            return c;
        }
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        for (Entry entry : entries) {
            groups.computeIfAbsent(entry.id().getNamespace(), ns -> new ArrayList<>()).add(entry);
        }
        // Front nach Norden zum Gang: ein Signal wirft das Ergebnis dorthin.
        BlockState crafter = Blocks.CRAFTER.defaultBlockState();
        int x = 0;
        int row = 0;
        c.wallSign(x, 0, 2, TcText.bold(TcText.t("section.recipes", "Crafting Recipes")),
                TcText.t("section.recipes.sub", "open a crafter"), TcText.t("section.recipes.sub2", "signal = craft"),
                Component.literal(entries.size() + " ").append(TcText.t("recipes.count", "recipes")));
        x += 2;
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            if (x + 2 > ROW_WIDTH) {
                x = 0;
                row++;
            }
            int z = 1 + row * ROW_PITCH;
            c.wallSign(x, 0, z + 1, TcText.bold(Component.literal(group.getKey())),
                    Component.literal(group.getValue().size() + " ").append(TcText.t("recipes.count", "recipes")));
            x += 1;
            for (int i = 0; i < group.getValue().size(); i++) {
                if (x + 1 > ROW_WIDTH) {
                    x = 0;
                    row++;
                    z = 1 + row * ROW_PITCH;
                }
                int dz = i % 2;
                c.place(x, 0, z + dz, crafter);
                c.contents(x, 0, z + dz, group.getValue().get(i).grid());
                if (dz == 1 || i == group.getValue().size() - 1) {
                    x++;
                }
            }
            x++;
        }
        c.anchor("recipes", 0, 0, 0);
        return c;
    }

    /** Die Crafter-Muster einer Schrittliste (fuer den Test). */
    public static List<List<ItemStack>> grids(List<TcOp> ops) {
        java.util.Set<net.minecraft.core.BlockPos> crafters = new java.util.HashSet<>();
        for (TcOp op : ops) {
            if (op instanceof TcOp.Place place && place.state().is(Blocks.CRAFTER)) {
                crafters.add(place.pos());
            }
        }
        List<List<ItemStack>> out = new ArrayList<>();
        for (TcOp op : ops) {
            if (op instanceof TcOp.Fill fill && crafters.contains(fill.pos())) {
                out.add(fill.contents());
            }
        }
        return out;
    }

    /** Text eines Rasters (Ids, leer = "-") fuer Vergleiche. */
    public static String key(List<ItemStack> grid) {
        StringBuilder out = new StringBuilder();
        for (ItemStack stack : grid) {
            out.append(stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).append(' ');
        }
        return out.toString().trim();
    }
}
