package com.simplebuilding.clienttest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoublePredicate;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.util.GlowingTrimUtils;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.phys.AABB;

/**
 * Covers everything the mod does to the way <em>items</em> look on the client: the enchanted book
 * model that is swapped per enchantment, the two armour tooltips the mod adds, the first person
 * chisel tilt and the glowing armour trim.
 *
 * <p>Four independent claims are proven here, in this order. The first two observe client state,
 * the last two are screenshot difference tests in the style of the other renderer tests in this
 * package (noise floor, trigger, control step).
 *
 * <h2>1. Enchanted book model per enchantment</h2>
 * {@code EnchantmentModelProperty} maps a stored enchantment to a case name and
 * {@code assets/minecraft/items/enchanted_book.json} maps that name to its own model. The test
 * asserts the mapping in both halves: the property returns the expected name for each of the
 * nineteen mod enchantments, and the client's own {@code ItemModelResolver} really resolves each
 * of them to a <em>different</em> baked model, while a plain book and a book with a vanilla
 * enchantment both land on the shared fallback. It also asserts that the model and texture files
 * the item definition points at are in the jar at all.
 *
 * <p>Pixels are deliberately not used here: an enchanted book carries the animated enchantment
 * glint, whose texture matrix is driven by wall clock time, so two screenshots of the same book
 * are never identical and no noise floor could be established. The resolved model is the exact
 * thing the swap is about, so it is observed directly - the same reasoning as in
 * {@link MultiBlockBreakingClientGameTest}.
 *
 * <p>What breaks it: renaming or removing a case in the item definition, changing a return value
 * in {@code EnchantmentModelProperty}, dropping the property registration (every book then falls
 * back to one model), or a generated model or texture that never reached the jar.
 *
 * <h2>2. Radiance, glow and armour trim tooltips</h2>
 * {@code ItemMixin} appends the radiance and glow lines and a client side estimate of the trim
 * bonuses. The test reads the tooltip through {@code ItemStack.getTooltipLines}, so the mixin has
 * to be reached through the real vanilla path, and pins text, colour and the computed percentages.
 *
 * <p>What breaks it: dropping the mixin, changing a translation key or its English text, changing
 * a colour, changing a base percentage, or changing the client side multiplier.
 *
 * <h2>3. First person chisel tilt</h2>
 * {@code HeldItemRendererMixin} rotates the held chisel while the aimed at block is convertible,
 * gated by {@code tools.enableToolAnimations && tools.enableChiselAnimation}. The trigger used
 * here is the config pair, because it is the only switch that changes nothing else on screen:
 * the world, the camera, the HUD and the held stack are all identical between the two shots.
 * The second half of the branch - that the tilt needs a convertible target - is covered by
 * running the same config flip against a bedrock target, where the image must not move.
 *
 * <p>The HUD is switched back on for this part. Hiding it also removes the first person hand (see
 * {@link RendererTestScene}), which is exactly the thing under test. Nothing in the HUD reacts to
 * the config flip, and the held stack is equipped well before the baseline, so the item name
 * overlay has faded by then.
 *
 * <p>Bringing the HUD back also brings back the two things on it that move without anyone touching
 * them, and both are taken out before the baseline is measured - see {@link #freezeHudOverlays} and
 * {@link #clearChat} for the measurements behind that. They are overlays drawn on top of the
 * finished world image, so removing them cannot hide a hand that moved.
 *
 * <p>What breaks it: removing the mixin, losing the injection point on
 * {@code ItemInHandRenderer.submitArmWithItem}, dropping the {@code canChisel} guard (the bedrock
 * control then moves), or wiring the tilt to only one of the two config switches.
 *
 * <h2>4. Glowing armour trim</h2>
 * {@code EquipmentRendererMixin} replaces the light coordinates of the trim submit: glow level 1
 * is a constant full bright, level 2 pulses along a sine with a 500 ms divisor. Proven in a sealed,
 * unlit stone room where the difference between light 0 and full bright is the whole signal:
 * <ul>
 *   <li>level 1 must change the picture against the unlit trim, and must produce the
 *       <em>same</em> picture at two opposite points of the sine, which is what separates a
 *       constant from a pulse;</li>
 *   <li>level 2 must produce different pictures at the peak and at the trough of that same sine,
 *       and must come back at the following peak.</li>
 * </ul>
 *
 * <p>All three of those comparisons report exactly the same number of changed pixels within one
 * run - around 7 % of the frame; the absolute figure moves a little between runs, the equality
 * does not. That is not a coincidence and is worth writing down: the level 1 shot and the level 2
 * peak shot are byte identical, because both branches end up packing light 15, and the level 2 trough shot is
 * indistinguishable from the unlit one, because light 1 on a trim that stands in light 0 at
 * gamma 0 renders the same. So the run really only sees two distinct pictures here. The
 * assertions still bite: with the level 2 branch deleted, or turned into a constant, both level 2
 * comparisons collapse to zero changed pixels and fail.
 * The armour hangs on an armour stand rather than on the player: the vanilla humanoid model bobs
 * its arms with {@code ageInTicks}, so a player (or a chestplate on the stand, whose sleeves
 * follow those arms) would never hold still between two screenshots. An armour stand hides its
 * arms by default, and helmet, leggings and boots hang on parts that do not move.
 *
 * <p>What breaks it: removing the mixin or its injection point, letting level 1 pulse or level 2
 * stand still, or changing the sine divisor far enough that peak and trough no longer line up with
 * the 500 ms period this test waits for.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The sledgehammer half of the tool animation.</b> {@code HeldItemRendererMixin} tilts a
 *       sledgehammer when {@code getTransformationState} is non null. That state depends on the
 *       exact hit vector inside the block face, which the fixed scene of {@link RendererTestScene}
 *       cannot vary without moving the camera, and moving the camera changes every pixel. The
 *       shared config gate is covered through the chisel; the sledgehammer specific condition is
 *       not.</li>
 *   <li><b>The pulse waveform.</b> Only "bright at the peak, dim at the trough, bright again"
 *       is asserted. The exact brightness the mixin packs at a given moment is a local variable
 *       inside a vanilla method and is not observable from the outside, so the shape of the curve
 *       and the exact clamp bounds cannot be pinned without also pinning the defect below. The
 *       trough in particular is not distinguishable from an override that never happened at all:
 *       as measured above it renders exactly like the unlit trim. What is proven is that the trim
 *       light changes with wall clock time, not what the low end of its range is.</li>
 *   <li><b>The remaining eighteen trim material and pattern tooltip branches.</b> Two of them are
 *       pinned by value. The others are the same expression with a different constant; listing
 *       them all here would restate {@code ItemMixin} rather than test it.</li>
 * </ul>
 *
 * <h2>Known defect</h2>
 * <ul>
 *   <li><b>The armour tooltip numbers are not the numbers the server uses.</b>
 *       {@code TrimEffectUtil.getGlobalMultiplier} returns the real, progress dependent multiplier
 *       only for a {@code ServerPlayer}; on the client it always returns the fixed 0.2. On top of
 *       that the tooltip carries its own base values, which are not the ones the damage code
 *       applies - it advertises 2.5 % for sentry while {@code TrimEffectUtil} reduces with a base
 *       of 5 %. The displayed percentage therefore has no relation to the effect in play. The test
 *       pins what the client actually prints today and does not endorse it.</li>
 *   <li><b>Those tooltip numbers are formatted with the default locale.</b> {@code ItemMixin} calls
 *       {@code String.format} without a {@code Locale}, so the same item reads "0.3" on an English
 *       client and "0,3" on a German one. The test formats its expectation the same way, so it is
 *       locale independent - it cannot catch this one.</li>
 *   <li><b>The pulse never reaches the low end of its own range.</b>
 *       {@code simplebuilding$calculatePulsingLight} interpolates towards {@code maxLight = 20.0}
 *       and then clamps to 15, so roughly a third of every cycle sits flat at full bright, and the
 *       {@code lightValue < 0} clamp below it is unreachable because {@code minLight} is 1. The
 *       comment above the code says 1 to 15 and a divisor of 150; the code uses 20.0 and 500.0.
 *       Nothing here asserts the plateau, so fixing the bound will not turn this test red.</li>
 * </ul>
 */
public final class ItemRenderingClientGameTest implements FabricClientGameTest {

    /** Wall material for the chisel part: bedrock is in none of the four conversion tables. */
    private static final String INERT_WALL = "minecraft:bedrock";

    /** Target material for the chisel part: stone is in the forward table of every chisel tier. */
    private static final String CONVERTIBLE_TARGET = "minecraft:stone";

    /**
     * Ticks to let the first person hand settle. The mixin smooths its tilt by 15 % per rendered
     * frame, and the item name overlay under the hotbar fades after 40 ticks; 60 ticks clears both.
     */
    private static final int HAND_SETTLE_TICKS = 60;

    /** Mirrors the divisor in {@code EquipmentRendererMixin#simplebuilding$calculatePulsingLight}. */
    private static final double PULSE_DIVISOR_MILLIS = 500.0;

    /** Where the armour stand hangs in the dark room, three blocks in front of the camera. */
    private static final double STAND_X = 10.5;
    private static final double STAND_Y = 0.0;
    private static final double STAND_Z = 19.5;

    /** The client side multiplier {@code TrimEffectUtil.getGlobalMultiplier} falls back to. */
    private static final float CLIENT_TRIM_FACTOR = 0.2f;

    /** Base percentages {@code ItemMixin} prints before that factor is applied. */
    private static final float DIAMOND_MATERIAL_BASE_PERCENT = 1.5f;
    private static final float SENTRY_PATTERN_BASE_PERCENT = 2.5f;

    /**
     * Every case {@code EnchantmentModelProperty} can return, paired with the enchantment that
     * has to produce it. The names are also the suffixes of the model and texture files and the
     * case labels in {@code assets/minecraft/items/enchanted_book.json}.
     */
    private static final List<BookCase> BOOK_CASES = List.of(
            new BookCase(ModEnchantments.FAST_CHISELING, "fast_chiseling"),
            new BookCase(ModEnchantments.CONSTRUCTORS_TOUCH, "constructors_touch"),
            new BookCase(ModEnchantments.COLOR_PALETTE, "color_palette"),
            new BookCase(ModEnchantments.MASTER_BUILDER, "master_builder"),
            new BookCase(ModEnchantments.BREAK_THROUGH, "break_through"),
            new BookCase(ModEnchantments.RADIUS, "radius"),
            new BookCase(ModEnchantments.COVER, "cover"),
            new BookCase(ModEnchantments.BRIDGE, "bridge"),
            new BookCase(ModEnchantments.LINEAR, "linear"),
            new BookCase(ModEnchantments.VEIN_MINER, "vein_miner"),
            new BookCase(ModEnchantments.DEEP_POCKETS, "deep_pockets"),
            new BookCase(ModEnchantments.STRIP_MINER, "strip_miner"),
            new BookCase(ModEnchantments.VERSATILITY, "versatility"),
            new BookCase(ModEnchantments.DRAWER, "drawer"),
            new BookCase(ModEnchantments.KINETIC_PROTECTION, "kinetic_protection"),
            new BookCase(ModEnchantments.DOUBLE_JUMP, "double_jump"),
            new BookCase(ModEnchantments.OVERRIDE, "override"),
            new BookCase(ModEnchantments.FUNNEL, "funnel"),
            new BookCase(ModEnchantments.RANGE, "range"));

    /** One enchantment and the model case it has to select. */
    private record BookCase(ResourceKey<Enchantment> enchantment, String modelCase) {
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, INERT_WALL, "creative");

            try {
                testEnchantedBookModelPerEnchantment(context);
                testRadianceAndGlowTooltips(context);
                testArmourTrimTooltipNumbers(context);
                testChiselHandAnimation(context, singleplayer);
                testGlowingArmourTrim(context, singleplayer);
            } finally {
                restoreToolAnimationDefaults(context);
                restoreVignette(context);
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    // =====================================================================================
    // 1. ENCHANTED BOOK MODELS
    // =====================================================================================

    /**
     * Proves that every mod enchantment picks its own enchanted book model, and that a book
     * without one falls back to the vanilla model.
     *
     * <p>Runs entirely inside the client thread, because the model resolver, the resource manager
     * and the client registries all have to be touched from there.
     */
    private void testEnchantedBookModelPerEnchantment(ClientGameTestContext context) {
        String problem = context.computeOnClient(client -> {
            if (client.level == null) {
                return "no client level, so the property cannot look up the enchantment registry";
            }

            String missingAssets = missingBookAssets(client);

            if (missingAssets != null) {
                return missingAssets;
            }

            EnchantmentModelProperty property = new EnchantmentModelProperty();
            Map<String, List<Object>> identities = new LinkedHashMap<>();

            for (BookCase bookCase : BOOK_CASES) {
                ItemStack book = enchantedBook(client, bookCase.enchantment());

                String actual = property.get(book, client.level, client.player, 0, ItemDisplayContext.GUI);

                if (!bookCase.modelCase().equals(actual)) {
                    return "EnchantmentModelProperty returned " + actual + " for "
                            + bookCase.enchantment().identifier() + ", expected " + bookCase.modelCase();
                }

                identities.put(bookCase.modelCase(), modelIdentity(client, book));
            }

            List<Object> plain = modelIdentity(client, new ItemStack(Items.ENCHANTED_BOOK));

            if (plain.isEmpty()) {
                return "the plain enchanted book resolved to no model layers at all, so nothing "
                        + "below can tell two models apart";
            }

            List<Object> vanillaEnchanted = modelIdentity(client, enchantedBook(client, Enchantments.UNBREAKING));

            if (!plain.equals(vanillaEnchanted)) {
                return "a book with a vanilla enchantment did not resolve to the same fallback model "
                        + "as a plain enchanted book, so the mod's select property is grabbing books "
                        + "it has no case for";
            }

            for (Map.Entry<String, List<Object>> entry : identities.entrySet()) {
                if (entry.getValue().equals(plain)) {
                    return "the book for case " + entry.getKey() + " resolved to the same model as a "
                            + "plain enchanted book, so its case in the item definition never matched";
                }
            }

            List<String> names = new ArrayList<>(identities.keySet());

            for (int i = 0; i < names.size(); i++) {
                for (int j = i + 1; j < names.size(); j++) {
                    if (identities.get(names.get(i)).equals(identities.get(names.get(j)))) {
                        return "the cases " + names.get(i) + " and " + names.get(j)
                                + " resolved to the very same baked model, so they do not have "
                                + "separate models after all";
                    }
                }
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Enchanted book model swap: " + problem);
        }

        System.out.println("[simplebuilding-test] " + BOOK_CASES.size()
                + " enchanted book cases each resolve to their own baked model");
    }

    /**
     * The item definition and every model and texture it points at have to be readable through the
     * client resource manager. A generated file that never reached the jar would otherwise only
     * show up as a black and purple book on screen.
     */
    private static String missingBookAssets(Minecraft client) {
        ResourceManager resources = client.getResourceManager();
        List<String> missing = new ArrayList<>();

        Identifier definition = Identifier.withDefaultNamespace("items/enchanted_book.json");

        if (resources.getResource(definition).isEmpty()) {
            missing.add(definition.toString());
        }

        for (BookCase bookCase : BOOK_CASES) {
            Identifier model = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID,
                    "models/item/enchanted_book_" + bookCase.modelCase() + ".json");
            Identifier texture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID,
                    "textures/item/enchanted_book_" + bookCase.modelCase() + ".png");

            if (resources.getResource(model).isEmpty()) {
                missing.add(model.toString());
            }

            if (resources.getResource(texture).isEmpty()) {
                missing.add(texture.toString());
            }
        }

        return missing.isEmpty() ? null : "these assets are not in the client resources: " + missing;
    }

    /**
     * Resolves an item the way the client does before drawing it and returns the identity of the
     * models that went into it. {@code TrackingItemStackRenderState} exists for exactly this: every
     * {@code ItemModel} that contributes appends itself, so two stacks that end up on the same
     * baked model produce equal lists and two that do not, do not.
     */
    private static List<Object> modelIdentity(Minecraft client, ItemStack stack) {
        TrackingItemStackRenderState state = new TrackingItemStackRenderState();
        client.getItemModelResolver()
                .updateForTopItem(state, stack, ItemDisplayContext.GUI, client.level, client.player, 0);
        return new ArrayList<>((List<?>) state.getModelIdentity());
    }

    /** An enchanted book carrying exactly one stored enchantment at level 1. */
    private static ItemStack enchantedBook(Minecraft client, ResourceKey<Enchantment> key) {
        var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(registry.getOrThrow(key), 1);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    // =====================================================================================
    // 2. TOOLTIPS
    // =====================================================================================

    /**
     * Pins the two lines {@code ItemMixin} adds for the dynamic light upgrades, including the
     * negative case: a plain chestplate must carry neither of them.
     */
    private void testRadianceAndGlowTooltips(ClientGameTestContext context) {
        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            ItemStack radiant = new ItemStack(Items.DIAMOND_CHESTPLATE);

            for (int step = 0; step < 3; step++) {
                GlowingTrimUtils.incrementEmissionLevel(radiant);
            }

            String issue = requireTooltipLine(client, radiant, "Radiance Level: 3/5", TextColor.GOLD);

            if (issue != null) {
                return issue;
            }

            ItemStack glowing = new ItemStack(Items.DIAMOND_CHESTPLATE);
            GlowingTrimUtils.setGlowLevel(glowing, 1);
            issue = requireTooltipLine(client, glowing, "Glowing", TextColor.AQUA);

            if (issue != null) {
                return issue;
            }

            ItemStack overcharged = new ItemStack(Items.DIAMOND_CHESTPLATE);
            GlowingTrimUtils.setGlowLevel(overcharged, 2);
            issue = requireTooltipLine(client, overcharged, "Glowing II", TextColor.AQUA);

            if (issue != null) {
                return issue;
            }

            List<String> plain = tooltipTexts(client, new ItemStack(Items.DIAMOND_CHESTPLATE));

            for (String line : plain) {
                if (line.startsWith("Radiance Level") || line.equals("Glowing") || line.equals("Glowing II")) {
                    return "a plain diamond chestplate already carries the line " + line
                            + ", so the lines above prove nothing about the upgrades";
                }
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Radiance and glow tooltips: " + problem);
        }

        System.out.println("[simplebuilding-test] radiance and glow tooltip lines are as declared");
    }

    /**
     * Pins the numbers the armour tooltip prints. Both are the mixin's own base value scaled by the
     * client side fallback multiplier; see the Known defect note in the class javadoc for what
     * those numbers are worth.
     */
    private void testArmourTrimTooltipNumbers(ClientGameTestContext context) {
        String expectedMaterialLine = String.format("Material: Hard Shell (-%.1f%% Dmg)",
                DIAMOND_MATERIAL_BASE_PERCENT * CLIENT_TRIM_FACTOR);
        String expectedPatternLine = String.format("Trim Bonus: Projectile Dampening (-%.1f%%)",
                SENTRY_PATTERN_BASE_PERCENT * CLIENT_TRIM_FACTOR);

        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            ItemStack trimmed = new ItemStack(Items.DIAMOND_CHESTPLATE);
            trimmed.set(DataComponents.TRIM, trim(client, TrimMaterials.DIAMOND, TrimPatterns.SENTRY));

            String issue = requireTooltipLine(client, trimmed, expectedMaterialLine, TextColor.AQUA);

            if (issue != null) {
                return issue;
            }

            issue = requireTooltipLine(client, trimmed, expectedPatternLine, TextColor.BLUE);

            if (issue != null) {
                return issue;
            }

            for (String line : tooltipTexts(client, new ItemStack(Items.DIAMOND_CHESTPLATE))) {
                if (line.startsWith("Material: ") || line.startsWith("Trim Bonus: ")) {
                    return "an untrimmed diamond chestplate already carries the line " + line;
                }
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Armour trim tooltip: " + problem);
        }

        System.out.println("[simplebuilding-test] armour trim tooltip prints " + expectedMaterialLine
                + " and " + expectedPatternLine);
    }

    private static ArmorTrim trim(Minecraft client, ResourceKey<TrimMaterial> material,
                                  ResourceKey<TrimPattern> pattern) {
        var materials = client.level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        var patterns = client.level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        return new ArmorTrim(materials.getOrThrow(material), patterns.getOrThrow(pattern));
    }

    /**
     * Asserts that the tooltip of {@code stack} contains {@code text} in {@code colour}. Returns a
     * description of what went wrong, or null. The tooltip is read through the vanilla entry point,
     * so a mixin that stopped being applied shows up here rather than nowhere.
     */
    private static String requireTooltipLine(Minecraft client, ItemStack stack, String text, TextColor colour) {
        for (Component line : stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player,
                TooltipFlag.NORMAL)) {
            if (!line.getString().equals(text)) {
                continue;
            }

            TextColor actual = line.getStyle().getColor();

            if (actual == null || actual.getValue() != colour.getValue()) {
                return "the tooltip line " + text + " is drawn in " + actual + " instead of " + colour;
            }

            return null;
        }

        return "the tooltip has no line " + text + "; it reads " + tooltipTexts(client, stack);
    }

    private static List<String> tooltipTexts(Minecraft client, ItemStack stack) {
        List<String> texts = new ArrayList<>();

        for (Component line : stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player,
                TooltipFlag.NORMAL)) {
            texts.add(line.getString());
        }

        return texts;
    }

    // =====================================================================================
    // 3. CHISEL HAND ANIMATION
    // =====================================================================================

    /**
     * Screenshot difference test on the first person chisel tilt. The only thing that ever changes
     * between a compared pair is a boolean in the config, so anything that moves is the mixin.
     */
    private void testChiselHandAnimation(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        // The hand only renders while the HUD is up; see the class javadoc.
        RendererTestScene.showHudAgain(context);
        freezeHudOverlays(context);

        setToolAnimationSwitches(context, false, false);
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_chisel");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(HAND_SETTLE_TICKS);

        assertChiselTriggerConditions(context, false);

        clearChat(context);
        Path inert = context.takeScreenshot("chisel-a-inert-target");
        // Measured across the same interval that separates every compared pair below. A shorter
        // one would understate anything that drifts slowly, and with the HUD up things do.
        context.waitTicks(HAND_SETTLE_TICKS);
        clearChat(context);
        Path inertAgain = context.takeScreenshot("chisel-b-inert-target-again");

        ScreenshotDiff.Diff noiseFloor =
                ScreenshotDiff.compare("noise floor (chisel in hand, animation off)", inert, inertAgain);
        ScreenshotDiff.assertUnchanged(noiseFloor);

        // Control: with the animation switched on but the target not convertible, canChisel is
        // false and the hand has to stay exactly where it was.
        setToolAnimationSwitches(context, true, true);
        context.waitTicks(HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(context, false);

        clearChat(context);
        Path inertAnimated = context.takeScreenshot("chisel-c-inert-target-animated");
        assertReturnedToBaseline(noiseFloor,
                ScreenshotDiff.compare("control (animation on, target not convertible)", inert, inertAnimated),
                "switching the chisel animation on moved the hand even though the aimed at block is "
                        + "not convertible, so the canChisel guard in HeldItemRendererMixin is gone");

        // Now the same flip against a target the chisel can actually convert.
        singleplayer.getServer().runCommand("setblock "
                + RendererTestScene.TARGET.getX() + " " + RendererTestScene.TARGET.getY() + " "
                + RendererTestScene.TARGET.getZ() + " " + CONVERTIBLE_TARGET);
        singleplayer.getConnection().waitForClientboundPackets();
        setToolAnimationSwitches(context, false, false);
        context.waitTicks(HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(context, true);

        clearChat(context);
        Path live = context.takeScreenshot("chisel-d-live-target");

        setToolAnimationSwitches(context, true, true);
        context.waitTicks(HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(context, true);

        clearChat(context);
        Path liveAnimated = context.takeScreenshot("chisel-e-live-target-animated");
        ScreenshotDiff.Diff signal =
                ScreenshotDiff.compare("chisel tilt on a convertible target", live, liveAnimated);
        ScreenshotDiff.assertDrew("HeldItemRendererMixin (chisel tilt)", noiseFloor, signal);

        // Control: switching it off again has to bring the hand back.
        setToolAnimationSwitches(context, false, false);
        context.waitTicks(HAND_SETTLE_TICKS);

        clearChat(context);
        Path liveOffAgain = context.takeScreenshot("chisel-f-live-target-animation-off");
        assertReturnedToBaseline(noiseFloor,
                ScreenshotDiff.compare("control (chisel animation off again)", live, liveOffAgain),
                "switching the chisel animation off again did not restore the untilted hand");

        // The master switch: enableChiselAnimation alone must not be enough.
        setToolAnimationSwitches(context, false, true);
        context.waitTicks(HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(context, true);

        clearChat(context);
        Path masterOff = context.takeScreenshot("chisel-g-master-switch-off");
        assertReturnedToBaseline(noiseFloor,
                ScreenshotDiff.compare("control (master switch off, chisel switch on)", live, masterOff),
                "the hand tilted with tools.enableToolAnimations off, so the animation no longer "
                        + "needs both switches");

        restoreToolAnimationDefaults(context);
    }

    /**
     * Switches off the vignette, the one HUD element that keeps moving on a scene nobody touches.
     *
     * <p>{@code Hud.updateVignetteBrightness} lerps {@code vignetteBrightness} towards the ambient
     * light by 1 % per tick, starting from 1.0, so in a lit world it is still visibly falling
     * minutes after the client joined. Measured here across sixty ticks it lifted the floor in the
     * bottom corners of the frame by 13 per channel - one level over the tolerance of
     * {@link ScreenshotDiff}, and enough to fail a control step on its own. The other renderer
     * tests in this package never see it because {@code Hud} only extracts the vignette while the
     * HUD is visible, and they hide the HUD; this one needs the HUD for the hand.
     *
     * <p>It is a full screen overlay drawn after the world, so switching it off cannot hide a hand
     * that moved - it can only stop the corners from drifting underneath one.
     */
    private void freezeHudOverlays(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.vignette().set(false));
    }

    /** Puts the vignette back for whatever runs in this client after this test. */
    private void restoreVignette(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.vignette().set(true));
    }

    /**
     * Empties the chat, then lets one tick pass so the next rendered frame is the one without it.
     *
     * <p>Every command this test sends prints its feedback into the chat, a chat line stays for
     * 200 ticks and then fades out. A line that is printed between two compared screenshots
     * therefore leaves the picture somewhere in between them, and the diff would be credited to
     * the renderer: the first run of this test failed its control step on 10032 changed pixels
     * that were all the words "Your game mode has been updated to Creative Mode" disappearing.
     * Clearing before every single shot keeps the chat area empty in all seven of them.
     */
    private void clearChat(ClientGameTestContext context) {
        context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
        context.waitTick();
    }

    /**
     * Writes both animation switches through the same object the mixin reads and checks that the
     * write arrived. {@code Simplebuilding.getConfig()} hands out one live config object, which the
     * server side config test asserts separately.
     */
    private void setToolAnimationSwitches(ClientGameTestContext context, boolean master, boolean chisel) {
        context.runOnClient(client -> {
            SimplebuildingConfig config = Simplebuilding.getConfig();

            if (config == null || config.tools == null) {
                return;
            }

            config.tools.enableToolAnimations = master;
            config.tools.enableChiselAnimation = chisel;
        });

        String problem = context.computeOnClient(client -> {
            SimplebuildingConfig config = Simplebuilding.getConfig();

            if (config == null || config.tools == null) {
                return "Simplebuilding.getConfig() handed out no config, so the animation switches "
                        + "cannot be driven from this test";
            }

            if (config.tools.enableToolAnimations != master || config.tools.enableChiselAnimation != chisel) {
                return "the switches read back as " + config.tools.enableToolAnimations + "/"
                        + config.tools.enableChiselAnimation + " instead of " + master + "/" + chisel;
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Tool animation config setup failed: " + problem);
        }
    }

    /** Puts both switches back to their shipped defaults so later client tests see a clean config. */
    private void restoreToolAnimationDefaults(ClientGameTestContext context) {
        context.runOnClient(client -> {
            SimplebuildingConfig config = Simplebuilding.getConfig();

            if (config != null && config.tools != null) {
                config.tools.enableToolAnimations = true;
                config.tools.enableChiselAnimation = true;
            }
        });
    }

    /**
     * Verifies every condition {@code HeldItemRendererMixin} checks before the screenshot is taken.
     * {@code canChisel} is the renderer's own predicate, so this cannot drift away from it.
     */
    private void assertChiselTriggerConditions(ClientGameTestContext context, boolean expectConvertible) {
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            ItemStack held = client.player.getMainHandItem();

            if (!(held.getItem() instanceof ChiselItem chisel)) {
                return "main hand does not hold a ChiselItem but " + held;
            }

            if (chisel.isDedicatedSpatula()) {
                return "the held item is a spatula, which reads the reversed conversion tables";
            }

            if (client.player.isShiftKeyDown()) {
                return "the player is sneaking, which swaps the forward and backward tables";
            }

            boolean convertible = chisel.canChisel(client.level, RendererTestScene.TARGET, held, client.player);

            if (convertible != expectConvertible) {
                return "canChisel on " + client.level.getBlockState(RendererTestScene.TARGET)
                        + " returned " + convertible + " but this step needs " + expectConvertible;
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Chisel animation trigger conditions not met: " + problem + ". "
                    + RendererTestScene.describeAim(context));
        }
    }

    // =====================================================================================
    // 4. GLOWING ARMOUR TRIM
    // =====================================================================================

    /**
     * Screenshot difference test on the trim light override, run in a sealed unlit room so that
     * full bright and the surrounding light 0 are as far apart as they can get.
     */
    private void testGlowingArmourTrim(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        buildDarkRoom(context, singleplayer);
        wearTrimmedArmour(context, singleplayer, 0);

        Path unlit = context.takeScreenshot("glowtrim-a-unlit-trim");
        context.waitTicks(10);
        Path unlitAgain = context.takeScreenshot("glowtrim-b-unlit-trim-again");

        ScreenshotDiff.Diff noiseFloor =
                ScreenshotDiff.compare("noise floor (trimmed armour, no glow)", unlit, unlitAgain);
        ScreenshotDiff.assertUnchanged(noiseFloor);

        // Level 1: constant full bright. Sampled at both extremes of the sine that level 2 uses,
        // so a level 1 that started pulsing would fail the second comparison.
        wearTrimmedArmour(context, singleplayer, 1);
        waitForPulse(context, true);
        Path levelOnePeak = context.takeScreenshot("glowtrim-c-level-one-at-peak");
        waitForPulse(context, false);
        Path levelOneTrough = context.takeScreenshot("glowtrim-d-level-one-at-trough");

        ScreenshotDiff.assertDrew("EquipmentRendererMixin (glow level 1)", noiseFloor,
                ScreenshotDiff.compare("glow level 1 against the unlit trim", unlit, levelOnePeak));
        ScreenshotDiff.Diff levelOneDrift =
                ScreenshotDiff.compare("glow level 1 across half a pulse period", levelOnePeak, levelOneTrough);

        if (levelOneDrift.changedPixels() > Math.max(noiseFloor.changedPixels() * 4 + 200,
                levelOneDrift.totalPixels() / 20000)) {
            throw new AssertionError("Glow level 1 is not constant: " + levelOneDrift
                    + " between the peak and the trough of the level 2 sine. Level 1 is supposed to "
                    + "be a flat FULL_BRIGHT, only level 2 pulses.");
        }

        // Level 2: has to differ between the peak and the trough, and has to come back.
        wearTrimmedArmour(context, singleplayer, 2);
        waitForPulse(context, true);
        Path bright = context.takeScreenshot("glowtrim-e-level-two-bright");
        waitForPulse(context, false);
        Path dim = context.takeScreenshot("glowtrim-f-level-two-dim");
        waitForPulse(context, true);
        Path brightAgain = context.takeScreenshot("glowtrim-g-level-two-bright-again");

        ScreenshotDiff.assertDrew("EquipmentRendererMixin (glow level 2, peak against trough)", noiseFloor,
                ScreenshotDiff.compare("glow level 2 bright against dim", bright, dim));
        ScreenshotDiff.assertDrew("EquipmentRendererMixin (glow level 2, trough against next peak)", noiseFloor,
                ScreenshotDiff.compare("glow level 2 dim against the next bright phase", dim, brightAgain));

        // Control: without the component the picture has to be the unlit baseline again.
        wearTrimmedArmour(context, singleplayer, 0);
        Path removed = context.takeScreenshot("glowtrim-h-glow-removed");
        assertReturnedToBaseline(noiseFloor,
                ScreenshotDiff.compare("control (glow component removed again)", unlit, removed),
                "removing the glow component did not restore the unlit trim, so the measured "
                        + "differences cannot be attributed to EquipmentRendererMixin");
    }

    /**
     * A sealed stone box with no light source in it. Sky light cannot reach the inside, so every
     * block face renders at light 0 and the overridden trim light is the only bright thing on
     * screen. The camera keeps the position and heading of {@link RendererTestScene} and only
     * looks down far enough to frame the armour stand.
     */
    private void buildDarkRoom(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        String[] commands = {
                "gamerule advance_time false",
                "gamerule random_tick_speed 0",
                "time set midnight",
                "weather clear",
                "clear @a",
                "kill @e[type=!minecraft:player]",
                // hollow builds the shell and clears the inside in one go.
                "fill 4 -1 9 17 7 23 minecraft:stone hollow",
                "tp @a 10.5 0.0 16.5 0.0 10.0",
        };

        for (String command : commands) {
            singleplayer.getServer().runCommand(command);
        }

        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(20);
        singleplayer.getConnection().waitForChunksRender();

        RendererTestScene.makeRenderingDeterministic(context);

        context.runOnClient(client -> {
            // Stay in first person: the player's own model bobs its arms every tick, an armour
            // stand does not. A narrow field of view fills the frame with the stand.
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.fov().set(40);
            client.options.gamma().set(0.0);
        });

        context.waitTicks(20);
    }

    /**
     * Hangs helmet, leggings and boots with the same trim on the armour stand and sets their glow
     * level. No chestplate: its sleeves ride on the humanoid model's arms, and those bob with
     * {@code ageInTicks} even on an armour stand whose own arms are hidden.
     *
     * <p>The stand is created once and then reused. Replacing it between the steps would put an
     * entity removal and an entity spawn into the very interval whose pixels are compared, and a
     * screenshot taken while the client is between the two would be attributed to the renderer.
     */
    private void wearTrimmedArmour(ClientGameTestContext context, TestSingleplayerContext singleplayer, int glowLevel) {
        singleplayer.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            List<ArmorStand> present = level.getEntitiesOfClass(ArmorStand.class, standSearchBox());
            ArmorStand stand;

            if (present.size() == 1) {
                stand = present.get(0);
            } else {
                for (ArmorStand stale : present) {
                    stale.discard();
                }

                stand = new ArmorStand(level, STAND_X, STAND_Y, STAND_Z);
                level.addFreshEntity(stand);
            }

            ArmorTrim armourTrim = new ArmorTrim(
                    level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.QUARTZ),
                    level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SILENCE));

            stand.setItemSlot(EquipmentSlot.HEAD, glowingPiece(Items.NETHERITE_HELMET, armourTrim, glowLevel));
            stand.setItemSlot(EquipmentSlot.LEGS, glowingPiece(Items.NETHERITE_LEGGINGS, armourTrim, glowLevel));
            stand.setItemSlot(EquipmentSlot.FEET, glowingPiece(Items.NETHERITE_BOOTS, armourTrim, glowLevel));
        });

        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(20);

        assertGlowTriggerConditions(context, glowLevel);
    }

    private static ItemStack glowingPiece(Item item, ArmorTrim armourTrim, int glowLevel) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.TRIM, armourTrim);

        if (glowLevel > 0) {
            GlowingTrimUtils.setGlowLevel(stack, glowLevel);
        }

        return stack;
    }

    private static AABB standSearchBox() {
        return new AABB(STAND_X - 2.0, STAND_Y - 2.0, STAND_Z - 2.0, STAND_X + 2.0, STAND_Y + 4.0, STAND_Z + 2.0);
    }

    /**
     * Verifies on the client what {@code EquipmentRendererMixin} needs: an armour stand that is
     * really there, wearing trimmed pieces, at the glow level this step asked for. A silently
     * dropped equipment packet would otherwise look like a renderer that draws nothing.
     */
    private void assertGlowTriggerConditions(ClientGameTestContext context, int glowLevel) {
        String problem = context.computeOnClient(client -> {
            if (client.level == null) {
                return "no client level";
            }

            List<ArmorStand> stands = client.level.getEntitiesOfClass(ArmorStand.class, standSearchBox());

            if (stands.size() != 1) {
                return "the client sees " + stands.size() + " armour stands in front of the camera, "
                        + "expected exactly one";
            }

            ArmorStand stand = stands.get(0);

            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack piece = stand.getItemBySlot(slot);

                if (piece.isEmpty()) {
                    return "the armour stand wears nothing in " + slot;
                }

                if (piece.get(DataComponents.TRIM) == null) {
                    return "the piece in " + slot + " has no trim, and the mixin only touches the "
                            + "light of the trim submit";
                }

                int actual = GlowingTrimUtils.getGlowLevel(piece);

                if (actual != glowLevel) {
                    return "the piece in " + slot + " reports glow level " + actual + " instead of " + glowLevel;
                }
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Glowing trim trigger conditions not met: " + problem);
        }
    }

    /**
     * Waits until the mixin's sine has just entered its bright (or dim) end. It first waits for the
     * sine to be well away from that end, so the screenshot is always taken at the <em>start</em>
     * of the window and not somewhere near its end: entering at 0.95 leaves roughly 400 ms before
     * the value moves appreciably, which is far more than a frame.
     */
    private void waitForPulse(ClientGameTestContext context, boolean bright) {
        waitForSine(context, sine -> bright ? sine < -0.5 : sine > 0.5, bright, "leaving");
        waitForSine(context, sine -> bright ? sine > 0.95 : sine < -0.95, bright, "entering");
    }

    private void waitForSine(ClientGameTestContext context, DoublePredicate reached, boolean bright, String stage) {
        // Budgeted in wall clock rather than in ticks, because the sine the mixin reads is wall
        // clock as well: a client that does not tick at 20 Hz would otherwise change how many
        // periods a fixed tick budget covers. One period is 2 * pi * 500 ms, so 20 s is six of them.
        long deadline = System.currentTimeMillis() + 20_000L;

        while (System.currentTimeMillis() < deadline) {
            if (reached.test(Math.sin(System.currentTimeMillis() / PULSE_DIVISOR_MILLIS))) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("Waiting for the pulse phase timed out while " + stage + " the "
                + (bright ? "bright" : "dim") + " end. The sine in this test uses the divisor "
                + PULSE_DIVISOR_MILLIS + " ms; if the mixin no longer does, the two cannot line up.");
    }

    // =====================================================================================
    // SHARED
    // =====================================================================================

    /**
     * The control step of every difference test above: after the trigger is taken away the picture
     * has to be the baseline again, within a few times the measured noise floor.
     */
    private static void assertReturnedToBaseline(ScreenshotDiff.Diff noiseFloor, ScreenshotDiff.Diff residual,
                                                 String message) {
        int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, residual.totalPixels() / 20000);

        if (residual.changedPixels() > allowed) {
            throw new AssertionError("Control step failed: " + message + " (" + residual + ", allowed "
                    + allowed + " pixels).");
        }
    }
}
