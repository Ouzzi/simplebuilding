package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    /**
     * MC 26.2: {@code valueLookupBuilder(...)} ist entfallen (Ersatz: {@code builder(...)}) und
     * {@link net.minecraft.data.tags.TagAppender} nimmt nur noch {@link ResourceKey}s statt Block-Instanzen
     * entgegen (Vanilla nutzt dafuer die Konstanten aus {@code net.minecraft.references.BlockIds}).
     * Dieser Helfer liefert den Registry-Key zu einer Block-Instanz, damit die Tag-Inhalte
     * unveraendert bleiben.
     */
    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            for (var chest : java.util.List.of(ModBlocks.REINFORCED_TRAPPED_CHEST, ModBlocks.NETHERITE_TRAPPED_CHEST, ModBlocks.ENDERITE_TRAPPED_CHEST)) {
                builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(chest));
                builder(net.minecraft.tags.TagKey.<Block>create(net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "chests/trapped"))).add(key(chest));
                builder(net.minecraft.tags.TagKey.<Block>create(net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "chests"))).add(key(chest));
            }
        }
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            builder(BlockTags.SMALL_FLOWERS).add(key(ModBlocks.SILENT_DANDELION));
            builder(BlockTags.FLOWER_POTS).add(key(ModBlocks.POTTED_SILENT_DANDELION));
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.ASTRAL_VAULT))
                    .add(key(ModBlocks.ASTRAL_PISTON)).add(key(ModBlocks.NIHIL_PISTON));
            // Astral-/Nihil-Kolben: zusaetzlich zu ihren festen Regeln (unzerstoerbar, Block-Entity, Kolben,
            // Doppelbloecke, BLOCK/DESTROY) nie bewegt - Portalrahmen, verstaerkter Tiefenschiefer, Durchbruch-Extras.
            builder(ModTags.Blocks.END_PISTON_IMMOVABLE)
                    .add(key(Blocks.END_PORTAL_FRAME))
                    .add(key(Blocks.REINFORCED_DEEPSLATE))
                    .add(key(Blocks.BEDROCK))
                    .add(key(Blocks.ENDER_CHEST))
                    .add(key(ModBlocks.ASTRAL_VAULT))
                    .forceAddTag(ModTags.Blocks.PISTON_BREACHABLE_EXTRA);
        }
        // Dimensions-Schrott: Spitzhacke (Enderit prueft der Block selbst), immun gegen Wither und Drache.
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            for (var scrap : java.util.List.of(ModBlocks.DIMENSIONAL_SCRAP, ModBlocks.NETHER_DIMENSIONAL_SCRAP, ModBlocks.END_DIMENSIONAL_SCRAP)) {
                builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(scrap));
                builder(BlockTags.NEEDS_DIAMOND_TOOL).add(key(scrap));
                builder(BlockTags.WITHER_IMMUNE).add(key(scrap));
                builder(BlockTags.DRAGON_IMMUNE).add(key(scrap));
            }
        }
        // Weisheitserz wie Diamanterz: Spitzhacke ab Eisen.
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.IRON_ROD));
            builder(BlockTags.NEEDS_STONE_TOOL).add(key(ModBlocks.IRON_ROD));
            // Goldstab wie der Goldblock: Spitzhacke ab Eisen.
            builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.GOLD_ROD));
            builder(BlockTags.NEEDS_IRON_TOOL).add(key(ModBlocks.GOLD_ROD));
            // Netherit- und Enderitstab wie ihr Materialblock: Spitzhacke ab Diamant.
            builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.NETHERITE_ROD)).add(key(ModBlocks.ENDERITE_ROD));
            builder(BlockTags.NEEDS_DIAMOND_TOOL).add(key(ModBlocks.NETHERITE_ROD)).add(key(ModBlocks.ENDERITE_ROD));
        }
        if (com.simplebuilding.version.McVersion.AUTO_SMITHER) {
            builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.AUTO_SMITHER));
        }
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.SAGE_ORE)).add(key(ModBlocks.DEEPSLATE_SAGE_ORE));
            builder(BlockTags.NEEDS_IRON_TOOL).add(key(ModBlocks.SAGE_ORE)).add(key(ModBlocks.DEEPSLATE_SAGE_ORE));
        }
        // Aus Simple Tweaks: alle Platten mit der Spitzhacke (Abbautempo regelt sonst der Besitz).
        com.simplebuilding.tweaks.block.TweaksBlocks.all().forEach(block -> builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(block)));

        // 1. Der Block soll mit einer Spitzhacke SCHNELLER abbaubar sein
        // Das behalten wir bei.
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.CRACKED_DIAMOND_BLOCK))
                .add(key(ModBlocks.REINFORCED_HOPPER))
                .add(key(ModBlocks.NETHERITE_HOPPER))
                .add(key(ModBlocks.REINFORCED_BLAST_FURNACE))
                .add(key(ModBlocks.NETHERITE_BLAST_FURNACE))
                .add(key(ModBlocks.REINFORCED_PISTON))
                .add(key(ModBlocks.REINFORCED_STICKY_PISTON))
                .add(key(ModBlocks.NETHERITE_PISTON))
                .add(key(ModBlocks.ENDERITE_PISTON))
                .add(key(ModBlocks.REINFORCED_PISTON_HEAD))
                .add(key(ModBlocks.NETHERITE_PISTON_HEAD))
                .add(key(ModBlocks.ENDERITE_PISTON_HEAD))
                .add(key(ModBlocks.REINFORCED_FURNACE))
                .add(key(ModBlocks.NETHERITE_FURNACE))
                .add(key(ModBlocks.REINFORCED_SMOKER))
                .add(key(ModBlocks.NETHERITE_SMOKER))
                .add(key(ModBlocks.ENDERITE_HOPPER))
                .add(key(ModBlocks.ENDERITE_FURNACE))
                .add(key(ModBlocks.ENDERITE_SMOKER))
                .add(key(ModBlocks.ENDERITE_BLAST_FURNACE))
                .add(key(ModBlocks.REINFORCED_CHEST))
                .add(key(ModBlocks.NETHERITE_CHEST))
                .add(key(ModBlocks.ENDERITE_CHEST))
                // Gestufte Shulkerkisten: schneller mit der Spitzhacke, droppen aber mit jedem Werkzeug.
                .add(key(ModBlocks.REINFORCED_SHULKER_BOX))
                .add(key(ModBlocks.NETHERITE_SHULKER_BOX))
                .add(key(ModBlocks.ENDERITE_SHULKER_BOX));


        // 2. Er benötigt mindestens ein Eisenwerkzeug (wie Diamantblock)
        builder(BlockTags.NEEDS_IRON_TOOL)
                .add(key(ModBlocks.CRACKED_DIAMOND_BLOCK));


        // Pickaxe Mineable
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.NIHILITH_ORE))
                .add(key(ModBlocks.ASTRALIT_ORE))
                .add(key(ModBlocks.ENDERITE_BLOCK));

        // Quarz-Schachbretter kopieren Lapis-, Purpur-, Schwarzstein- bzw. Endstein-Eigenschaften und
        // verlangen damit das passende Werkzeug; ohne diesen Tag liessen sie sich nicht abbauen.
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.PURPUR_QUARTZ_CHECKER))
                .add(key(ModBlocks.LAPIS_QUARTZ_CHECKER))
                .add(key(ModBlocks.BLACKSTONE_QUARTZ_CHECKER))
                .add(key(ModBlocks.RESIN_QUARTZ_CHECKER))
                .add(key(ModBlocks.NIHILITH_QUARTZ_CHECKER))
                .add(key(ModBlocks.ASTRALIT_QUARTZ_CHECKER))
                .add(key(ModBlocks.ENDER_QUARTZ_CHECKER))
                .add(key(ModBlocks.POLISHED_ASTRALIT_CHECKER))
                .add(key(ModBlocks.POLISHED_NIHILITH_CHECKER))
                .add(key(ModBlocks.POLISHED_ENDER_QUARTZ_CHECKER));

        // Endstein-Familie: alle kopieren den polierten Endstein bzw. Purpur und verlangen damit
        // eine Spitzhacke fuer ihren Drop - ohne diesen Tag fiele beim Abbau nichts heraus.
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.POLISHED_END_STONE))
                .add(key(ModBlocks.ASTRAL_END_STONE))
                .add(key(ModBlocks.NIHIL_END_STONE))
                .add(key(ModBlocks.ASTRAL_PURPUR_BLOCK))
                .add(key(ModBlocks.NIHIL_PURPUR_BLOCK));

        // End-Paletten (Astralit, Nihilith, Enderquarz): alle verlangen eine Spitzhacke; Treppen,
        // Stufen und Mauern stehen in den Vanilla-Tags ihrer Form (eine Mauer ausserhalb von
        // minecraft:walls verbindet sich nicht mit ihren Nachbarn).
        for (ModBlocks.EndPalette palette : ModBlocks.END_PALETTES) {
            palette.blocks().forEach(block -> builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(block)));
            palette.stairs().forEach(block -> builder(BlockTags.STAIRS).add(key(block)));
            palette.slabs().forEach(block -> builder(BlockTags.SLABS).add(key(block)));
            palette.walls().forEach(block -> builder(BlockTags.WALLS).add(key(block)));
        }
        // Alternativbloecke der Grundbloecke (2026-10-03): Spitzhacke wie ihr Grundblock.
        for (ModBlocks.EndAlternates alternates : ModBlocks.END_ALTERNATES) {
            alternates.alternates().forEach(block -> builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(block)));
        }

        // Needs Diamond Tool (oder Netherite)
        builder(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(key(ModBlocks.NIHILITH_ORE))
                .add(key(ModBlocks.ASTRALIT_ORE))
                .add(key(ModBlocks.ENDERITE_BLOCK));

        // BEACON BASE (Wichtig für dein Feature)
        builder(BlockTags.BEACON_BASE_BLOCKS)
                .add(key(ModBlocks.ENDERITE_BLOCK));

        // Kolben-Durchbruch (PistonBreach): unzerstoerbare Bloecke, die trotzdem kein Kolben der
        // Mod schiebt oder zerstoert. Bloecke mit Block-Entity (Befehls-, Struktur-, Verbund-,
        // Testbloecke, bewegter Kolben) nimmt PistonBreach zusaetzlich auch ohne diesen Tag aus.
        builder(ModTags.Blocks.PISTON_BREACH_IMMUNE)
                .add(key(Blocks.BARRIER))
                .add(key(Blocks.LIGHT))
                .add(key(Blocks.COMMAND_BLOCK))
                .add(key(Blocks.REPEATING_COMMAND_BLOCK))
                .add(key(Blocks.CHAIN_COMMAND_BLOCK))
                .add(key(Blocks.STRUCTURE_BLOCK))
                .add(key(Blocks.JIGSAW))
                .add(key(Blocks.TEST_BLOCK))
                .add(key(Blocks.TEST_INSTANCE_BLOCK))
                .add(key(Blocks.MOVING_PISTON))
                .forceAddTag(BlockTags.PORTALS);

        // ... und abbaubare Bloecke, die trotzdem als unzerstoerbar gelten (isPushable
        // verweigert verstaerkten Tiefenschiefer beim Namen).
        builder(ModTags.Blocks.PISTON_BREACHABLE_EXTRA)
                .add(key(Blocks.REINFORCED_DEEPSLATE));
    }
}
