package com.simplebuilding.tweaks.datagen;

import com.simplebuilding.tweaks.block.CopperPressurePlateBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.client.renderer.item.properties.numeric.Damage;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Modelle des Simple-Tweaks-Teils (Simple Tweaks: ModModelProvider): alle Pads flach wie eine
 * Vanilla-Druckplatte ({@code pressure_plate_up}) mit ihrer eigenen Textur, die echten Druckplatten
 * zusaetzlich gedrueckt ({@code pressure_plate_down} bei {@code powered=true}), die Items flach. Der Echo-Kompass hat eigene Bilder (32 Nadelstellungen, zeigt zum verknuepften Leitstein) und
 * drei Riss-Stufen, solange er nicht voll repariert ist.
 */
public final class TweaksModelGen {
    private TweaksModelGen() {
    }

    public static void blocks(BlockModelGenerators generator) {
        for (Block block : TweaksBlocks.all()) {
            // Flypads I-III (seit 2026-09-27 aus Enderit) tragen neue Texturen <id>_ender; die alten
            // flypad.png, reinforced_flypad.png und stellar_flypad.png bleiben liegen (Vorlage der
            // Trank-Pads in der Netherit-Palette, tools/textures/potion_pad_textures.py).
            boolean enderFlypad = block == TweaksBlocks.FLYPAD || block == TweaksBlocks.REINFORCED_FLYPAD || block == TweaksBlocks.STELLAR_FLYPAD;
            if (isPlate(block)) {
                plate(generator, block);
                continue;
            }
            TextureMapping texture = enderFlypad ? TextureMapping.defaultTexture(TextureMapping.getBlockTexture(block, "_ender"))
                    : TextureMapping.defaultTexture(block);
            Identifier model = ModelTemplates.PRESSURE_PLATE_UP.create(block, texture, generator.modelOutput);
            generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model)));
            generator.registerSimpleItemModel(block, model);
        }
        blazeHead(generator);
    }

    /**
     * Lohenkopf wie Vanillas {@code BlockModelGenerators#createHead}: beide Bloecke zeigen nur die
     * Partikel von {@code block/skull} (gezeichnet wird der Kopf vom SkullBlockRenderer), das Item ist
     * ein {@code minecraft:head}-Sondermodell auf {@code item/template_skull}.
     */
    private static void blazeHead(BlockModelGenerators generator) {
        net.minecraft.client.data.models.MultiVariant skull =
                BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("skull"));
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(TweaksBlocks.BLAZE_HEAD, skull));
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(TweaksBlocks.BLAZE_WALL_HEAD, skull));
        generator.itemModelOutput.accept(TweaksItems.BLAZE_HEAD, ItemModelUtils.specialModel(
                ModelLocationUtils.decorateItemModelLocation("template_skull"), SKULL_TRANSFORM,
                new net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked(com.simplebuilding.tweaks.block.BlazeHeadType.BLAZE)));
    }

    /** Wie BlockModelGenerators.SKULL_TRANSFORM (dort privat). */
    private static final com.mojang.math.Transformation SKULL_TRANSFORM = new com.mojang.math.Transformation(
            new org.joml.Vector3f(0.5F, 0.0F, 0.5F), new org.joml.Quaternionf().rotationX((float) Math.PI), null, null);
    /** Echte Druckplatten (nicht die Pads): sinken gedrueckt ein wie Vanilla-Platten. */
    private static boolean isPlate(Block block) {
        return block instanceof PressurePlateBlock || block instanceof CopperPressurePlateBlock;
    }

    /**
     * Wie Vanillas createPressurePlate: {@code powered=false} zeigt {@code pressure_plate_up},
     * {@code powered=true} {@code pressure_plate_down}. Gewachste Kupferplatten nutzen wie
     * Vanilla-Kupfer die Modelle (und damit die Textur) ihrer ungewachsten Stufe.
     */
    private static void plate(BlockModelGenerators generator, Block block) {
        Identifier up;
        Identifier down;
        if (block instanceof CopperPressurePlateBlock copper && copper.isWaxed()) {
            Block unwaxed = CopperPressurePlateBlock.stages().get(copper.getAge().ordinal());
            up = ModelLocationUtils.getModelLocation(unwaxed);
            down = ModelLocationUtils.getModelLocation(unwaxed, "_down");
        } else {
            TextureMapping texture = TextureMapping.defaultTexture(block);
            up = ModelTemplates.PRESSURE_PLATE_UP.create(block, texture, generator.modelOutput);
            down = ModelTemplates.PRESSURE_PLATE_DOWN.create(block, texture, generator.modelOutput);
        }
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.POWERED)
                .select(true, BlockModelGenerators.plainVariant(down))
                .select(false, BlockModelGenerators.plainVariant(up))));
        generator.registerSimpleItemModel(block, up);
    }

    public static void items(ItemModelGenerators generator) {
        generator.generateFlatItem(TweaksItems.SPAWN_ELYTRA, ModelTemplates.FLAT_ITEM);
        // Ein Stock ist ein Stock: das Vanilla-Bild, in der Hand gehalten wie ein Werkzeug.
        Item stick = com.simplebuilding.tweaks.easter.EasterEggs.funnyStick();
        generator.itemModelOutput.accept(stick, ItemModelUtils.plainModel(ModelTemplates.FLAT_HANDHELD_ITEM.create(stick,
                TextureMapping.layer0(net.minecraft.world.item.Items.STICK), generator.modelOutput)));
        laserLens(generator);
        // Voll repariert: 32 eigene Nadelbilder, zeigt zum verknuepften Leitstein. Nicht voll repariert
        // (Schaden > 0): drei Riss-Stufen nach Anteil des Schadens, die Nadel steht still.
        generator.itemModelOutput.accept(TweaksItems.ECHO_COMPASS, ItemModelUtils.conditional(new Damaged(),
                ItemModelUtils.rangeSelect(new Damage(true), echoCompassCrackedModels(generator)),
                ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F,
                        echoCompassModels(generator))));
    }

    /**
     * Amethystlinse ({@code laser_pointer}): leer (Schaden = Haltbarkeit, normiert 1,0) zeigt
     * {@code item/laser_pointer_empty}. Solange dieses Bild noch nicht gezeichnet ist, nimmt das
     * Leer-Modell das normale Bild, damit nie die Fehltextur erscheint - nach dem Zeichnen reicht ein
     * neuer Datagen-Lauf.
     */
    private static void laserLens(ItemModelGenerators generator) {
        Identifier full = ModelTemplates.FLAT_ITEM.create(TweaksItems.LASER_POINTER, TextureMapping.layer0(TweaksItems.LASER_POINTER), generator.modelOutput);
        Identifier emptyTexture = ModelLocationUtils.getModelLocation(TweaksItems.LASER_POINTER, "_empty");
        Identifier empty = ModelTemplates.FLAT_ITEM.create(emptyTexture,
                TextureMapping.layer0(textureExists(emptyTexture) ? new Material(emptyTexture) : TextureMapping.getItemTexture(TweaksItems.LASER_POINTER)),
                generator.modelOutput);
        generator.itemModelOutput.accept(TweaksItems.LASER_POINTER, ItemModelUtils.rangeSelect(new Damage(true),
                ItemModelUtils.plainModel(full), ItemModelUtils.override(ItemModelUtils.plainModel(empty), 1.0F)));
    }

    /** Sucht {@code textures/<pfad>.png} in den src/main/resources ueber dem Datagen-Ausgabeordner. */
    private static boolean textureExists(Identifier texture) {
        String relative = "src/main/resources/assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png";
        String start = System.getProperty("fabric-api.datagen.output-dir", System.getProperty("user.dir"));
        for (java.io.File dir = new java.io.File(start).getAbsoluteFile(); dir != null; dir = dir.getParentFile()) {
            if (new java.io.File(dir, relative).isFile()) {
                return true;
            }
        }
        return false;
    }

    /** Wie ItemModelGenerators#createCompassModels, mit den Bildern echo_compass_00..31 aus generate_textures.py. */
    private static List<RangeSelectItemModel.Entry> echoCompassModels(ItemModelGenerators generator) {
        List<RangeSelectItemModel.Entry> overrides = new ArrayList<>();
        ItemModel.Unbaked base = ItemModelUtils.plainModel(flat(generator, TweaksItems.ECHO_COMPASS, "_16"));
        overrides.add(ItemModelUtils.override(base, 0.0F));
        for (int i = 1; i < 32; i++) {
            int index = Mth.positiveModulo(i - 16, 32);
            overrides.add(ItemModelUtils.override(ItemModelUtils.plainModel(
                    flat(generator, TweaksItems.ECHO_COMPASS, String.format(Locale.ROOT, "_%02d", index))), i - 0.5F));
        }
        overrides.add(ItemModelUtils.override(base, 31.5F));
        return overrides;
    }

    /** Riss-Stufen nach Schadensanteil: bis 1/3 fast repariert, bis 2/3 halb, darueber leer. */
    private static List<RangeSelectItemModel.Entry> echoCompassCrackedModels(ItemModelGenerators generator) {
        List<RangeSelectItemModel.Entry> stages = new ArrayList<>();
        stages.add(ItemModelUtils.override(ItemModelUtils.plainModel(flat(generator, TweaksItems.ECHO_COMPASS, "_cracked_2")), 0.0F));
        stages.add(ItemModelUtils.override(ItemModelUtils.plainModel(flat(generator, TweaksItems.ECHO_COMPASS, "_cracked_1")), 1.0F / 3.0F));
        stages.add(ItemModelUtils.override(ItemModelUtils.plainModel(flat(generator, TweaksItems.ECHO_COMPASS, "_cracked_0")), 2.0F / 3.0F));
        return stages;
    }

    private static Identifier flat(ItemModelGenerators generator, Item item, String suffix) {
        return ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item, suffix),
                TextureMapping.layer0(TextureMapping.getItemTexture(item, suffix)), generator.modelOutput);
    }
}
