package com.simplebuilding.clientgametest;

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
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
 * <p>Five independent claims are proven here, in this order. The first three observe client state,
 * the last two are screenshot difference tests in the style of the other renderer tests in this
 * package (noise floor, trigger, measurement, control step).
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
 * {@link MultiBlockBreakingClientTest}.
 *
 * <p>What breaks it: renaming or removing a case in the item definition, changing a return value
 * in {@code EnchantmentModelProperty}, dropping the property registration (every book then falls
 * back to one model), or a generated model or texture that never reached the jar.
 *
 * <h2>2. Radiance and glow tooltips</h2>
 * {@code ItemMixin} appends the radiance and glow lines. The test reads the tooltip through
 * {@code ItemStack.getTooltipLines}, so the mixin has to be reached through the real vanilla path,
 * and pins text and colour. The negative case is part of the claim: a plain chestplate must carry
 * neither line, or the positive assertions would pass for a tooltip that says the same thing to
 * everyone.
 *
 * <h2>3. Armour trim tooltip numbers</h2>
 * The same mixin adds a client side estimate of the trim bonuses. The test pins the computed
 * percentages, and again asserts that an untrimmed chestplate carries neither line.
 *
 * <p>What breaks 2 and 3: dropping the mixin, changing a translation key or its English text,
 * changing a colour, changing a base percentage, or changing the client side multiplier.
 *
 * <h2>4. First person chisel tilt</h2>
 * {@code HeldItemRendererMixin} rotates the held chisel while the aimed at block is convertible,
 * gated by {@code tools.enableToolAnimations && tools.enableChiselAnimation}. The trigger used
 * here is the config pair, because it is the only switch that changes nothing else on screen:
 * the world, the camera, the HUD and the held stack are all identical between the two shots.
 * The second half of the branch - that the tilt needs a convertible target - is covered by
 * running the same config flip against a bedrock target, where the image must not move.
 *
 * <p>The HUD is switched back on for this part. Hiding it also removes the first person hand (see
 * {@link TestScene}), which is exactly the thing under test. Nothing in the HUD reacts to the
 * config flip, and the held stack is equipped well before the baseline, so the item name overlay
 * has faded by then.
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
 * <h2>5. Glowing armour trim</h2>
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
 * peak shot are byte identical, because both branches end up packing light 15, and the level 2
 * trough shot is indistinguishable from the unlit one, because light 1 on a trim that stands in
 * light 0 at gamma 0 renders the same. So the run really only sees two distinct pictures here. The
 * assertions still bite: with the level 2 branch deleted, or turned into a constant, both level 2
 * comparisons collapse to zero changed pixels and fail.
 *
 * <p>The armour hangs on an armour stand rather than on the player: the vanilla humanoid model bobs
 * its arms with {@code ageInTicks}, so a player (or a chestplate on the stand, whose sleeves
 * follow those arms) would never hold still between two screenshots. An armour stand hides its
 * arms by default ({@code ShowArms} defaults to false), and helmet, leggings and boots hang on
 * parts that do not move.
 *
 * <p>What breaks it: removing the mixin or its injection point, letting level 1 pulse or level 2
 * stand still, or changing the sine divisor far enough that peak and trough no longer line up with
 * the 500 ms period this test waits for.
 *
 * <h2>What the port to the shared step form changed, and why</h2>
 * <ul>
 *   <li><b>Every screenshot path travels in a {@link Later}.</b> The comparisons need the PNG of an
 *       <em>earlier</em> shot, and the path cannot be derived from the checkpoint name: Fabric
 *       writes {@code 0004_name.png} with a per-run counter, NeoForge writes {@code name.png}.
 *       {@link Script#shot} hands the path back once the file is on disk, and a later step reads
 *       it. {@link Later#get} throwing on an unfilled value is what keeps a dropped path loud
 *       instead of green.</li>
 *   <li><b>The comparisons run in {@link Script#verify}, not in {@link Script#act}.</b> They need
 *       no game state at all, and on Fabric a harness call from inside a client task is forbidden
 *       outright - a comparison inside one would rule out the screenshots around it.</li>
 *   <li><b>The armour stand is summoned and dressed by command, not through the server object.</b>
 *       The shared {@link Harness} has no server handle; it has {@link Script#command}. The stand
 *       is summoned once under a tag and its three pieces are replaced per glow level, which keeps
 *       the property the old server side code was written for: no entity is removed and re-added
 *       inside an interval whose pixels are compared. The cost is that the trim and the glow level
 *       now travel as item component syntax in a command string; a syntax that no longer parses
 *       fails the command and therefore the test, which is loud rather than silent.</li>
 *   <li><b>The trigger checks throw directly</b> instead of returning a problem string to a caller
 *       that throws. An {@code act} step already runs on the client thread and an exception in it
 *       fails the test naming the step. The messages are unchanged.</li>
 *   <li><b>Every case rebuilds what it needs instead of inheriting it.</b> The old class built the
 *       bedrock scene once and let five parts run in it in a fixed order. The whole suite shares
 *       one world, so an order dependency between cases is a defect even where it used to work:
 *       the chisel case builds its own bedrock scene, the glow case builds its own dark room, and
 *       the glow case tears that room down again.</li>
 *   <li><b>The cleanup is a set of ordinary last steps, not a {@code finally} block.</b> A step
 *       list has no place to hang one. <b>They therefore do not run when a step above them
 *       fails.</b> What they put back outlives this test: the two tool animation switches, the
 *       vignette option, the gamma the dark room lowered, the dark room itself and the hidden HUD.
 *       Of those, only the HUD and the field of view heal by themselves, because
 *       {@link TestScene#build} sets them for the next test anyway; the config values, the gamma
 *       and the leftover stone would survive a failure of this test into the next one.</li>
 *   <li><b>The restore targets are read on entry rather than written as constants.</b> The old
 *       class restored the shipped defaults ({@code true}/{@code true} for the switches,
 *       {@code true} for the vignette). Since the suite shares one client, "the default" is not a
 *       safe assumption about what this test found, so what it found is what it puts back.</li>
 *   <li><b>The wall clock budget of the pulse wait survived the move into a tick driven step.</b>
 *       See {@link #waitForSine}.</li>
 * </ul>
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The sledgehammer half of the tool animation.</b> {@code HeldItemRendererMixin} tilts a
 *       sledgehammer when {@code getTransformationState} is non null. That state depends on the
 *       exact hit vector inside the block face, which the fixed scene of {@link TestScene} cannot
 *       vary without moving the camera, and moving the camera changes every pixel. The shared
 *       config gate is covered through the chisel; the sledgehammer specific condition is not.</li>
 *   <li><b>The pulse waveform.</b> Only "bright at the peak, dim at the trough, bright again" is
 *       asserted. The exact brightness the mixin packs at a given moment is a local variable inside
 *       a vanilla method and is not observable from the outside, so the shape of the curve and the
 *       exact clamp bounds cannot be pinned without also pinning the defect below. The trough in
 *       particular is not distinguishable from an override that never happened at all: as measured
 *       above it renders exactly like the unlit trim. What is proven is that the trim light changes
 *       with wall clock time, not what the low end of its range is.</li>
 *   <li><b>The remaining eighteen trim material and pattern tooltip branches.</b> Two of them are
 *       pinned by value. The others are the same expression with a different constant; listing them
 *       all here would restate {@code ItemMixin} rather than test it.</li>
 *   <li><b>That the armour equipment really travelled from the server to the client.</b> It does -
 *       the stand is dressed by command and the trigger check reads the <em>client</em> entity back
 *       - but that is a side effect of how the scene is built, not a claim this test defends.</li>
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
public final class ItemRenderingClientTest {

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

    /**
     * Wall clock budget for one half of a pulse wait, unchanged from the Fabric-only class.
     * One period is 2 * pi * 500 ms, so 20 s is a little over six of them.
     */
    private static final long PULSE_WAIT_MILLIS = 20_000L;

    /**
     * Tick budget of the same step. Deliberately far above the 400 ticks that 20 s covers at
     * 20 Hz, so that the wall clock deadline inside the condition is the one that normally fires
     * and the failure message can name the divisor that stopped lining up. The tick budget is only
     * the backstop for a client that has stopped ticking altogether.
     */
    private static final int PULSE_WAIT_TICK_BUDGET = 1200;

    /** Where the armour stand hangs in the dark room, three blocks in front of the camera. */
    private static final double STAND_X = 10.5;
    private static final double STAND_Y = 0.0;
    private static final double STAND_Z = 19.5;

    /**
     * Entity tag on the armour stand, so the dressing commands address exactly this stand.
     *
     * <p>A tag rather than {@code @e[type=armor_stand,limit=1]}: {@code limit} on {@code @e} picks
     * an arbitrary one of the matches, and "there happens to be only one" is an assumption the
     * client side trigger check is supposed to be testing, not relying on.
     */
    private static final String STAND_TAG = "simplebuilding_glowtrim";

    /** The trim on the armour stand's pieces, as the item component syntax of a command. */
    private static final String STAND_TRIM =
            "minecraft:trim={material:\"minecraft:quartz\",pattern:\"minecraft:silence\"}";

    /** The three pieces the stand wears; a chestplate is left off - see the class javadoc. */
    private static final String[][] STAND_PIECES = {
            {"armor.head", "minecraft:netherite_helmet"},
            {"armor.legs", "minecraft:netherite_leggings"},
            {"armor.feet", "minecraft:netherite_boots"},
    };

    /**
     * The volume of the sealed dark room, as the arguments of a fill command.
     *
     * <p>Written once because it is filled twice: once to build the shell and once to take it away
     * again. Parts of it ({@code z} 9 and 21 to 23) lie outside the volume {@link TestScene#build}
     * rebuilds, so a room that is not removed here survives into every test after this one.
     */
    private static final String DARK_ROOM_VOLUME = "4 -1 9 17 7 23";

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

    private ItemRenderingClientTest() {
    }

    /**
     * Everything this test does; it needs the joined world the driver keeps open.
     *
     * <p>The scene is built once here for the three state observing cases, which need a client
     * level and a player and nothing else. The two renderer cases below build their own scene, so
     * neither of them depends on what the case before it left behind.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, INERT_WALL, "creative");

        Later<Boolean> originalToolAnimations = new Later<>("the enableToolAnimations found on entry");
        Later<Boolean> originalChiselAnimation = new Later<>("the enableChiselAnimation found on entry");
        Later<Boolean> originalVignette = new Later<>("the vignette option found on entry");
        Later<Double> originalGamma = new Later<>("the gamma found on entry");

        script.act("remember the client state the last steps have to put back", client -> {
            SimplebuildingConfig config = requireConfig();
            originalToolAnimations.set(config.tools.enableToolAnimations);
            originalChiselAnimation.set(config.tools.enableChiselAnimation);
            originalVignette.set(client.options.vignette().get());
            originalGamma.set(client.options.gamma().get());
        });

        enchantedBookModelPerEnchantment(script);
        radianceAndGlowTooltips(script);
        armourTrimTooltipNumbers(script);
        chiselHandAnimation(script, originalToolAnimations, originalChiselAnimation);
        glowingArmourTrim(script, originalGamma);

        // What the old finally block did. These are ordinary last steps, because a step list has no
        // hook to hang a finally on: on a FAILING run they do not run at all. See the class javadoc
        // for what that leaves behind for the next test.
        restoreToolAnimationSwitches(script, originalToolAnimations, originalChiselAnimation);
        restoreVignette(script, originalVignette);
        TestScene.showHudAgain(script);
    }

    // =====================================================================================
    // 1. ENCHANTED BOOK MODELS
    // =====================================================================================

    /**
     * Proves that every mod enchantment picks its own enchanted book model, and that a book
     * without one falls back to the vanilla model.
     *
     * <p>One {@code act} step, because the model resolver, the resource manager and the client
     * registries all have to be touched from the client thread and the whole comparison is one
     * pass over them - splitting it would only add steps that hand each other a map.
     *
     * <p>The three controls are the reason the positive assertions mean anything: a plain
     * enchanted book has to resolve to <em>some</em> model at all (otherwise "different from the
     * plain book" is satisfied by every broken resolve), a book with a vanilla enchantment has to
     * land on that same fallback (otherwise the mod's select property is grabbing books it has no
     * case for), and no two mod cases may share a baked model (otherwise nineteen cases could be
     * served by two files and still pass).
     */
    private static void enchantedBookModelPerEnchantment(Script script) {
        script.act("every mod enchantment resolves the enchanted book to its own baked model", client -> {
            if (client.level == null) {
                throw bookFailure("no client level, so the property cannot look up the enchantment registry");
            }

            String missingAssets = missingBookAssets(client);

            if (missingAssets != null) {
                throw bookFailure(missingAssets);
            }

            EnchantmentModelProperty property = new EnchantmentModelProperty();
            Map<String, List<Object>> identities = new LinkedHashMap<>();

            for (BookCase bookCase : BOOK_CASES) {
                ItemStack book = enchantedBook(client, bookCase.enchantment());

                String actual = property.get(book, client.level, client.player, 0, ItemDisplayContext.GUI);

                if (!bookCase.modelCase().equals(actual)) {
                    throw bookFailure("EnchantmentModelProperty returned " + actual + " for "
                            + bookCase.enchantment().identifier() + ", expected " + bookCase.modelCase());
                }

                identities.put(bookCase.modelCase(), modelIdentity(client, book));
            }

            List<Object> plain = modelIdentity(client, new ItemStack(Items.ENCHANTED_BOOK));

            if (plain.isEmpty()) {
                throw bookFailure("the plain enchanted book resolved to no model layers at all, so nothing "
                        + "below can tell two models apart");
            }

            List<Object> vanillaEnchanted = modelIdentity(client, enchantedBook(client, Enchantments.UNBREAKING));

            if (!plain.equals(vanillaEnchanted)) {
                throw bookFailure("a book with a vanilla enchantment did not resolve to the same fallback model "
                        + "as a plain enchanted book, so the mod's select property is grabbing books "
                        + "it has no case for");
            }

            for (Map.Entry<String, List<Object>> entry : identities.entrySet()) {
                if (entry.getValue().equals(plain)) {
                    throw bookFailure("the book for case " + entry.getKey() + " resolved to the same model as a "
                            + "plain enchanted book, so its case in the item definition never matched");
                }
            }

            List<String> names = new ArrayList<>(identities.keySet());

            for (int i = 0; i < names.size(); i++) {
                for (int j = i + 1; j < names.size(); j++) {
                    if (identities.get(names.get(i)).equals(identities.get(names.get(j)))) {
                        throw bookFailure("the cases " + names.get(i) + " and " + names.get(j)
                                + " resolved to the very same baked model, so they do not have "
                                + "separate models after all");
                    }
                }
            }

            TestLog.info(BOOK_CASES.size()
                    + " enchanted book cases each resolve to their own baked model");
        });
    }

    private static AssertionError bookFailure(String problem) {
        return new AssertionError("Enchanted book model swap: " + problem);
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
    // 2. RADIANCE AND GLOW TOOLTIPS
    // =====================================================================================

    /**
     * Pins the two lines {@code ItemMixin} adds for the dynamic light upgrades, including the
     * negative case: a plain chestplate must carry neither of them.
     *
     * <p>The radiance stack is stepped up three times with the mod's own
     * {@code incrementEmissionLevel} rather than written straight to level 3, because that is the
     * only path that also proves the increment is the thing the tooltip reads.
     */
    private static void radianceAndGlowTooltips(Script script) {
        script.act("the radiance and glow tooltip lines read as declared", client -> {
            if (client.player == null || client.level == null) {
                throw tooltipFailure("Radiance and glow tooltips", "no client player or level");
            }

            ItemStack radiant = new ItemStack(Items.DIAMOND_CHESTPLATE);

            for (int step = 0; step < 3; step++) {
                GlowingTrimUtils.incrementEmissionLevel(radiant);
            }

            requireTooltipLine(client, "Radiance and glow tooltips", radiant, "Radiance Level: 3/5",
                    TextColor.fromLegacyFormat(ChatFormatting.GOLD));

            ItemStack glowing = new ItemStack(Items.DIAMOND_CHESTPLATE);
            GlowingTrimUtils.setGlowLevel(glowing, 1);
            requireTooltipLine(client, "Radiance and glow tooltips", glowing, "Glowing", TextColor.fromLegacyFormat(ChatFormatting.AQUA));

            ItemStack overcharged = new ItemStack(Items.DIAMOND_CHESTPLATE);
            GlowingTrimUtils.setGlowLevel(overcharged, 2);
            requireTooltipLine(client, "Radiance and glow tooltips", overcharged, "Glowing II", TextColor.fromLegacyFormat(ChatFormatting.AQUA));

            List<String> plain = tooltipTexts(client, new ItemStack(Items.DIAMOND_CHESTPLATE));

            for (String line : plain) {
                if (line.startsWith("Radiance Level") || line.equals("Glowing") || line.equals("Glowing II")) {
                    throw tooltipFailure("Radiance and glow tooltips",
                            "a plain diamond chestplate already carries the line " + line
                                    + ", so the lines above prove nothing about the upgrades");
                }
            }

            TestLog.info("radiance and glow tooltip lines are as declared");
        });
    }

    // =====================================================================================
    // 3. ARMOUR TRIM TOOLTIP NUMBERS
    // =====================================================================================

    /**
     * Pins the numbers the armour tooltip prints. Both are the mixin's own base value scaled by the
     * client side fallback multiplier; see the Known defect note in the class javadoc for what
     * those numbers are worth.
     *
     * <p>The expectations are formatted with the same {@code String.format} call the mixin uses, so
     * the decimal separator of the run's default locale cancels out on both sides. That is what
     * makes this test locale independent - and also what stops it from ever noticing the missing
     * {@code Locale} argument.
     */
    private static void armourTrimTooltipNumbers(Script script) {
        String expectedMaterialLine = String.format("Material: Hard Shell (-%.1f%% Dmg)",
                DIAMOND_MATERIAL_BASE_PERCENT * CLIENT_TRIM_FACTOR);
        String expectedPatternLine = String.format("Trim Bonus: Projectile Dampening (-%.1f%%)",
                SENTRY_PATTERN_BASE_PERCENT * CLIENT_TRIM_FACTOR);

        script.act("the armour trim tooltip prints the client side estimate", client -> {
            if (client.player == null || client.level == null) {
                throw tooltipFailure("Armour trim tooltip", "no client player or level");
            }

            ItemStack trimmed = new ItemStack(Items.DIAMOND_CHESTPLATE);
            trimmed.set(DataComponents.TRIM, trim(client, TrimMaterials.DIAMOND, TrimPatterns.SENTRY));

            requireTooltipLine(client, "Armour trim tooltip", trimmed, expectedMaterialLine, TextColor.fromLegacyFormat(ChatFormatting.AQUA));
            requireTooltipLine(client, "Armour trim tooltip", trimmed, expectedPatternLine, TextColor.fromLegacyFormat(ChatFormatting.BLUE));

            for (String line : tooltipTexts(client, new ItemStack(Items.DIAMOND_CHESTPLATE))) {
                if (line.startsWith("Material: ") || line.startsWith("Trim Bonus: ")) {
                    throw tooltipFailure("Armour trim tooltip",
                            "an untrimmed diamond chestplate already carries the line " + line);
                }
            }

            TestLog.info("armour trim tooltip prints " + expectedMaterialLine
                    + " and " + expectedPatternLine);
        });
    }

    private static ArmorTrim trim(Minecraft client, ResourceKey<TrimMaterial> material,
                                  ResourceKey<TrimPattern> pattern) {
        var materials = client.level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        var patterns = client.level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        return new ArmorTrim(materials.getOrThrow(material), patterns.getOrThrow(pattern));
    }

    /**
     * Asserts that the tooltip of {@code stack} contains {@code text} in {@code colour}. The
     * tooltip is read through the vanilla entry point, so a mixin that stopped being applied shows
     * up here rather than nowhere.
     *
     * <p>Throws instead of returning a problem string: the step it runs in is already on the client
     * thread, and an exception there fails the test naming the step, which is what the old string
     * relay was reimplementing.
     */
    private static void requireTooltipLine(Minecraft client, String subject, ItemStack stack, String text,
                                           TextColor colour) {
        for (Component line : stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player,
                TooltipFlag.NORMAL)) {
            if (!line.getString().equals(text)) {
                continue;
            }

            TextColor actual = line.getStyle().getColor();

            if (actual == null || actual.getValue() != colour.getValue()) {
                throw tooltipFailure(subject, "the tooltip line " + text + " is drawn in " + actual
                        + " instead of " + colour);
            }

            return;
        }

        throw tooltipFailure(subject, "the tooltip has no line " + text + "; it reads "
                + tooltipTexts(client, stack));
    }

    private static AssertionError tooltipFailure(String subject, String problem) {
        return new AssertionError(subject + ": " + problem);
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
    // 4. CHISEL HAND ANIMATION
    // =====================================================================================

    /**
     * Screenshot difference test on the first person chisel tilt. The only thing that ever changes
     * between a compared pair is a boolean in the config, so anything that moves is the mixin.
     *
     * <p><b>The order of the steps is the argument, not a style.</b> Every measurement below walks
     * the same four stages, and each is a precondition for the next being readable at all:
     * <ol>
     *   <li><b>noise floor</b> - two shots of the very same, untouched scene, taken
     *       {@value #HAND_SETTLE_TICKS} ticks apart. That interval is not decoration: it is the same
     *       one that separates every compared pair below, and a shorter one would understate
     *       anything that drifts slowly - with the HUD up, things do. The result is
     *       <em>asserted</em> to be essentially zero, so a scene that is not actually static fails
     *       here as a setup error instead of quietly producing "differences" further down;</li>
     *   <li><b>every trigger condition</b> - re-computed from the client's own state right before
     *       each shot, including {@code canChisel}, which is the renderer's own predicate. Without
     *       this a missing difference could always be explained by "the test never armed the
     *       renderer";</li>
     *   <li><b>the measurement</b> - the shot with the trigger in place, against the baseline;</li>
     *   <li><b>the control</b> - take the trigger away again and require the baseline picture back.
     *       Without it, "the picture drifts anyway" would explain the signal just as well as the
     *       renderer would.</li>
     * </ol>
     *
     * <p>The two negative cases around the positive one are real branches of the mixin: the
     * {@code canChisel} guard (animation on, bedrock target) and the master switch
     * ({@code enableToolAnimations} off while {@code enableChiselAnimation} is on).
     *
     * <p>Builds its own bedrock scene rather than inheriting the one {@link #inWorld} built, so
     * this case does not depend on the three that run before it.
     */
    private static void chiselHandAnimation(Script script, Later<Boolean> originalToolAnimations,
                                            Later<Boolean> originalChiselAnimation) {
        TestScene.build(script, INERT_WALL, "creative");

        // The hand only renders while the HUD is up; see the class javadoc.
        TestScene.showHudAgain(script);
        freezeHudOverlays(script);

        setToolAnimationSwitches(script, false, false);
        script.command("item replace entity @a weapon.mainhand with simplebuilding:netherite_chisel");
        script.awaitPackets();
        script.idle("let the chisel arrive and its equip animation finish", HAND_SETTLE_TICKS);

        assertChiselTriggerConditions(script, false);

        clearChat(script);
        Later<Path> inert = script.shot("chisel-a-inert-target");
        // Measured across the same interval that separates every compared pair below.
        script.idle("let the noise floor span a full settle interval", HAND_SETTLE_TICKS);
        clearChat(script);
        Later<Path> inertAgain = script.shot("chisel-b-inert-target-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the chisel scene");

        script.verify("measure the noise floor of the chisel scene", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (chisel in hand, animation off)", inert.get(), inertAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        // Control: with the animation switched on but the target not convertible, canChisel is
        // false and the hand has to stay exactly where it was.
        setToolAnimationSwitches(script, true, true);
        script.idle("let the hand settle after switching the animation on", HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(script, false);

        clearChat(script);
        Later<Path> inertAnimated = script.shot("chisel-c-inert-target-animated");

        script.verify("the animation does not move the hand on a target that cannot be chiselled", () -> {
            // assertLooksIdentical rather than assertBackToBaseline: same tolerance, but the claim
            // here is "the trigger was never sufficient", not "the trigger was taken away again",
            // and a failure has to name the right one or it sends the reader to the wrong place.
            ScreenshotDiff.assertLooksIdentical(noiseFloor.get(),
                    ScreenshotDiff.compare("control (animation on, target not convertible)",
                            inert.get(), inertAnimated.get()),
                    "the aimed at block is not convertible, so switching the chisel animation on must "
                            + "not move the hand - if it did, the canChisel guard in "
                            + "HeldItemRendererMixin is gone");
        });

        // Now the same flip against a target the chisel can actually convert.
        script.command("setblock "
                + TestScene.TARGET.getX() + " " + TestScene.TARGET.getY() + " "
                + TestScene.TARGET.getZ() + " " + CONVERTIBLE_TARGET);
        script.awaitPackets();
        setToolAnimationSwitches(script, false, false);
        script.idle("let the hand settle on the convertible target", HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(script, true);

        clearChat(script);
        Later<Path> live = script.shot("chisel-d-live-target");

        setToolAnimationSwitches(script, true, true);
        script.idle("let the tilt build up on the convertible target", HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(script, true);

        clearChat(script);
        Later<Path> liveAnimated = script.shot("chisel-e-live-target-animated");

        script.verify("the chisel tilt reached the screen", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "chisel tilt on a convertible target", live.get(), liveAnimated.get());
            ScreenshotDiff.assertDrew("HeldItemRendererMixin (chisel tilt)", noiseFloor.get(), signal);
        });

        // Control: switching it off again has to bring the hand back.
        setToolAnimationSwitches(script, false, false);
        script.idle("let the hand settle back after switching the animation off", HAND_SETTLE_TICKS);

        clearChat(script);
        Later<Path> liveOffAgain = script.shot("chisel-f-live-target-animation-off");

        script.verify("switching the chisel animation off again restores the untilted hand", () ->
                ScreenshotDiff.assertBackToBaseline("switching the chisel animation off again",
                        noiseFloor.get(),
                        ScreenshotDiff.compare("control (chisel animation off again)",
                                live.get(), liveOffAgain.get())));

        // The master switch: enableChiselAnimation alone must not be enough.
        setToolAnimationSwitches(script, false, true);
        script.idle("let the hand settle with only the chisel switch on", HAND_SETTLE_TICKS);
        assertChiselTriggerConditions(script, true);

        clearChat(script);
        Later<Path> masterOff = script.shot("chisel-g-master-switch-off");

        script.verify("the chisel switch alone does not tilt the hand", () ->
                ScreenshotDiff.assertLooksIdentical(noiseFloor.get(),
                        ScreenshotDiff.compare("control (master switch off, chisel switch on)",
                                live.get(), masterOff.get()),
                        "the animation needs both switches, so tools.enableChiselAnimation alone must "
                                + "not tilt the hand while tools.enableToolAnimations is off"));

        restoreToolAnimationSwitches(script, originalToolAnimations, originalChiselAnimation);
    }

    /**
     * Switches off the vignette, the one HUD element that keeps moving on a scene nobody touches.
     *
     * <p>{@code Hud.updateVignetteBrightness} lerps {@code vignetteBrightness} towards the ambient
     * light by 1 % per tick, starting from 1.0, so in a lit world it is still visibly falling
     * minutes after the client joined. Measured across sixty ticks it lifted the floor in the
     * bottom corners of the frame by 13 per channel - one level over the tolerance of
     * {@link ScreenshotDiff}, and enough to fail a control step on its own. The other renderer
     * tests in this package never see it because {@code Hud} only extracts the vignette while the
     * HUD is visible, and they hide the HUD; this one needs the HUD for the hand.
     *
     * <p>It is a full screen overlay drawn after the world, so switching it off cannot hide a hand
     * that moved - it can only stop the corners from drifting underneath one.
     */
    private static void freezeHudOverlays(Script script) {
        script.act("switch the vignette off so the frame corners stop drifting", client ->
                client.options.vignette().set(false));
    }

    /**
     * Puts the vignette back the way it was found, for whatever runs in this client afterwards.
     *
     * <p>Nothing else resets this option - {@link TestScene#makeRenderingDeterministic} does not
     * touch it - so a failing run of this test leaves the vignette off for every later test. Those
     * tests hide the HUD, which is why that is survivable rather than fine.
     */
    private static void restoreVignette(Script script, Later<Boolean> originalVignette) {
        script.act("put the vignette option back the way it was found", client ->
                client.options.vignette().set(originalVignette.get()));
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
    private static void clearChat(Script script) {
        script.act("empty the chat before the shot", client -> client.gui.getChat().clearMessages(true));
        script.idle("let one frame pass without the chat", 1);
    }

    /**
     * Writes both animation switches through the same object the mixin reads and checks that the
     * write arrived. {@code Simplebuilding.getConfig()} hands out one live config object, which the
     * server side config test asserts separately.
     *
     * <p>The read back is cheap and it is the difference between "the renderer ignores the config"
     * and "the test never actually set it" - two failures that look identical in a screenshot.
     */
    private static void setToolAnimationSwitches(Script script, boolean master, boolean chisel) {
        script.act("set enableToolAnimations to " + master + " and enableChiselAnimation to " + chisel,
                client -> {
                    SimplebuildingConfig config = requireConfig();
                    config.tools.enableToolAnimations = master;
                    config.tools.enableChiselAnimation = chisel;
                });

        script.act("the animation switches really read back as " + master + "/" + chisel, client -> {
            SimplebuildingConfig config = requireConfig();

            if (config.tools.enableToolAnimations != master || config.tools.enableChiselAnimation != chisel) {
                throw new AssertionError("Tool animation config setup failed: the switches read back as "
                        + config.tools.enableToolAnimations + "/" + config.tools.enableChiselAnimation
                        + " instead of " + master + "/" + chisel);
            }
        });
    }

    /**
     * Puts both switches back the way they were found on entry.
     *
     * <p>The old class wrote the shipped defaults ({@code true}/{@code true}) here. The suite shares
     * one client, so what this test found is not necessarily the default - restoring what was there
     * is the only version that is correct in both cases.
     */
    private static void restoreToolAnimationSwitches(Script script, Later<Boolean> originalToolAnimations,
                                                     Later<Boolean> originalChiselAnimation) {
        script.act("put the tool animation switches back the way they were found", client -> {
            SimplebuildingConfig config = requireConfig();
            config.tools.enableToolAnimations = originalToolAnimations.get();
            config.tools.enableChiselAnimation = originalChiselAnimation.get();
        });
    }

    /**
     * The one live config object, or a failure that says the switches cannot be driven at all.
     *
     * <p>The old class returned quietly from its setter when the config was missing and only
     * complained in the read back afterwards. Failing at the source names the cause instead.
     */
    private static SimplebuildingConfig requireConfig() {
        SimplebuildingConfig config = Simplebuilding.getConfig();

        if (config == null || config.tools == null) {
            throw new AssertionError("Simplebuilding.getConfig() handed out no config, so the animation "
                    + "switches cannot be driven from this test");
        }

        return config;
    }

    /**
     * Verifies every condition {@code HeldItemRendererMixin} checks before the screenshot is taken.
     * {@code canChisel} is the renderer's own predicate, so this cannot drift away from it.
     *
     * <p>The aim check comes first and is separate on purpose: a scene that is aimed somewhere else
     * fails as a setup error with the full aim diagnosis, instead of being reported as "the
     * renderer draws nothing".
     */
    private static void assertChiselTriggerConditions(Script script, boolean expectConvertible) {
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("the chisel trigger conditions hold, convertible target " + expectConvertible, client -> {
            String problem = null;

            if (client.player == null || client.level == null) {
                problem = "no client player or level";
            } else {
                ItemStack held = client.player.getMainHandItem();

                if (!(held.getItem() instanceof ChiselItem chisel)) {
                    problem = "main hand does not hold a ChiselItem but " + held;
                } else if (chisel.isDedicatedSpatula()) {
                    problem = "the held item is a spatula, which reads the reversed conversion tables";
                } else if (client.player.isShiftKeyDown()) {
                    problem = "the player is sneaking, which swaps the forward and backward tables";
                } else {
                    boolean convertible =
                            chisel.canChisel(client.level, TestScene.TARGET, held, client.player);

                    if (convertible != expectConvertible) {
                        problem = "canChisel on " + client.level.getBlockState(TestScene.TARGET)
                                + " returned " + convertible + " but this step needs " + expectConvertible;
                    }
                }
            }

            if (problem != null) {
                throw new AssertionError("Chisel animation trigger conditions not met: " + problem + ". "
                        + TestScene.describeAim(client));
            }
        });
    }

    // =====================================================================================
    // 5. GLOWING ARMOUR TRIM
    // =====================================================================================

    /**
     * Screenshot difference test on the trim light override, run in a sealed unlit room so that
     * full bright and the surrounding light 0 are as far apart as they can get.
     *
     * <p>Same four stages as the chisel case: noise floor first (two shots of the unlit trim ten
     * ticks apart, asserted to be essentially zero), then the trigger conditions before every
     * measurement (the stand is really there, wearing trimmed pieces, at the glow level this step
     * asked for), then the measurements, then the control that removes the glow component again.
     *
     * <p>Builds its own room and takes it down again afterwards, so neither the case before it nor
     * the tests after it are affected by what it does to the world.
     */
    private static void glowingArmourTrim(Script script, Later<Double> originalGamma) {
        buildDarkRoom(script);
        summonArmourStand(script);
        wearTrimmedArmour(script, 0);

        Later<Path> unlit = script.shot("glowtrim-a-unlit-trim");
        script.idle("let ten ticks pass between the two baseline shots", 10);
        Later<Path> unlitAgain = script.shot("glowtrim-b-unlit-trim-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the dark room");

        script.verify("measure the noise floor of the dark room", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (trimmed armour, no glow)", unlit.get(), unlitAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        // Level 1: constant full bright. Sampled at both extremes of the sine that level 2 uses,
        // so a level 1 that started pulsing would fail the second comparison.
        wearTrimmedArmour(script, 1);
        waitForPulse(script, true);
        Later<Path> levelOnePeak = script.shot("glowtrim-c-level-one-at-peak");
        waitForPulse(script, false);
        Later<Path> levelOneTrough = script.shot("glowtrim-d-level-one-at-trough");

        script.verify("glow level 1 lights the trim at all", () ->
                ScreenshotDiff.assertDrew("EquipmentRendererMixin (glow level 1)", noiseFloor.get(),
                        ScreenshotDiff.compare("glow level 1 against the unlit trim",
                                unlit.get(), levelOnePeak.get())));

        script.verify("glow level 1 is constant across half a pulse period", () ->
                ScreenshotDiff.assertLooksIdentical(noiseFloor.get(),
                        ScreenshotDiff.compare("glow level 1 across half a pulse period",
                                levelOnePeak.get(), levelOneTrough.get()),
                        "glow level 1 is supposed to be a flat FULL_BRIGHT and only level 2 pulses, so "
                                + "the peak and the trough of the level 2 sine have to look the same"));

        // Level 2: has to differ between the peak and the trough, and has to come back.
        wearTrimmedArmour(script, 2);
        waitForPulse(script, true);
        Later<Path> bright = script.shot("glowtrim-e-level-two-bright");
        waitForPulse(script, false);
        Later<Path> dim = script.shot("glowtrim-f-level-two-dim");
        waitForPulse(script, true);
        Later<Path> brightAgain = script.shot("glowtrim-g-level-two-bright-again");

        script.verify("glow level 2 differs between the peak and the trough", () ->
                ScreenshotDiff.assertDrew("EquipmentRendererMixin (glow level 2, peak against trough)",
                        noiseFloor.get(),
                        ScreenshotDiff.compare("glow level 2 bright against dim",
                                bright.get(), dim.get())));

        script.verify("glow level 2 comes back at the following peak", () ->
                ScreenshotDiff.assertDrew("EquipmentRendererMixin (glow level 2, trough against next peak)",
                        noiseFloor.get(),
                        ScreenshotDiff.compare("glow level 2 dim against the next bright phase",
                                dim.get(), brightAgain.get())));

        // Control: without the component the picture has to be the unlit baseline again. Without
        // this step the measured differences could not be attributed to EquipmentRendererMixin at
        // all - "the dark room drifts anyway" would explain them just as well.
        wearTrimmedArmour(script, 0);
        Later<Path> removed = script.shot("glowtrim-h-glow-removed");

        script.verify("removing the glow component restores the unlit trim", () ->
                ScreenshotDiff.assertBackToBaseline("removing the glow component", noiseFloor.get(),
                        ScreenshotDiff.compare("control (glow component removed again)",
                                unlit.get(), removed.get())));

        removeDarkRoom(script, originalGamma);
    }

    /**
     * A sealed stone box with no light source in it. Sky light cannot reach the inside, so every
     * block face renders at light 0 and the overridden trim light is the only bright thing on
     * screen. The camera keeps the position and heading of {@link TestScene} and only looks down
     * far enough to frame the armour stand.
     */
    private static void buildDarkRoom(Script script) {
        script.command("gamerule advance_time false");
        script.command("gamerule random_tick_speed 0");
        script.command("time set midnight");
        script.command("weather clear");
        // Tolerant, exactly as in TestScene: an empty inventory and an empty room are both
        // perfectly good states to already be in, and vanilla reports them as command failures.
        script.command("clear @a", true);
        script.command("kill @e[type=!minecraft:player]", true);
        // hollow builds the shell and clears the inside in one go.
        script.command("fill " + DARK_ROOM_VOLUME + " minecraft:stone hollow", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 10.0");

        script.awaitPackets();
        script.idle("let the dark room arrive", 20);
        script.awaitChunks();

        TestScene.makeRenderingDeterministic(script);

        script.act("frame the armour stand in the dark", client -> {
            // Stay in first person: the player's own model bobs its arms every tick, an armour
            // stand does not. A narrow field of view fills the frame with the stand.
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.fov().set(40);
            client.options.gamma().set(0.0);
        });

        script.idle("let the narrowed view and the darkness settle", 20);
    }

    /**
     * Takes the room away again and puts the gamma back.
     *
     * <p>Neither is optional. Parts of the shell stand outside the volume {@link TestScene#build}
     * rebuilds, so leftover stone would sit in every scene after this one; and nothing resets the
     * gamma - {@link TestScene#makeRenderingDeterministic} does not touch it, so a client left at
     * gamma 0 would darken every later screenshot. The field of view and the camera type are not
     * restored here for exactly the opposite reason: the next {@link TestScene#build} sets both.
     *
     * <p>Like every cleanup in a step list, this does not run when a step above it failed.
     */
    private static void removeDarkRoom(Script script, Later<Double> originalGamma) {
        script.command("kill @e[type=!minecraft:player]", true);
        script.command("fill " + DARK_ROOM_VOLUME + " minecraft:air", true);
        script.awaitPackets();

        script.act("put the gamma back the way it was found", client ->
                client.options.gamma().set(originalGamma.get()));
    }

    /**
     * Puts one tagged armour stand in front of the camera.
     *
     * <p>Summoned once and then only re-dressed. Replacing the stand between the measurements would
     * put an entity removal and an entity spawn into the very interval whose pixels are compared,
     * and a screenshot taken while the client is between the two would be attributed to the
     * renderer.
     *
     * <p>{@code NoGravity} and {@code Invulnerable} keep it from moving or being removed by
     * anything the run does around it; the arms stay hidden, which is the default and is what makes
     * the stand hold still at all (see the class javadoc).
     */
    private static void summonArmourStand(Script script) {
        script.command("summon minecraft:armor_stand " + STAND_X + " " + STAND_Y + " " + STAND_Z
                + " {Tags:[\"" + STAND_TAG + "\"],NoGravity:1b,Invulnerable:1b}");
        script.awaitPackets();
        script.idle("let the armour stand arrive on the client", 20);
    }

    /**
     * Hangs helmet, leggings and boots with the same trim on the armour stand and sets their glow
     * level. No chestplate: its sleeves ride on the humanoid model's arms, and those bob with
     * {@code ageInTicks} even on an armour stand whose own arms are hidden.
     *
     * <p>At glow level 0 the pieces carry no {@code simplebuilding:glow_level} component at all,
     * which is what makes the baseline and the final control step the picture of an item the mixin
     * has no reason to touch.
     */
    private static void wearTrimmedArmour(Script script, int glowLevel) {
        String glow = glowLevel > 0 ? ",simplebuilding:glow_level=" + glowLevel : "";

        for (String[] piece : STAND_PIECES) {
            script.command("item replace entity @e[tag=" + STAND_TAG + "] " + piece[0]
                    + " with " + piece[1] + "[" + STAND_TRIM + glow + "]");
        }

        script.awaitPackets();
        script.idle("let the armour at glow level " + glowLevel + " arrive on the client", 20);

        assertGlowTriggerConditions(script, glowLevel);
    }

    /**
     * Verifies on the client what {@code EquipmentRendererMixin} needs: an armour stand that is
     * really there, wearing trimmed pieces, at the glow level this step asked for. A silently
     * dropped equipment packet would otherwise look like a renderer that draws nothing.
     */
    private static void assertGlowTriggerConditions(Script script, int glowLevel) {
        script.act("the armour stand really wears trimmed pieces at glow level " + glowLevel, client -> {
            String problem = null;

            if (client.level == null) {
                problem = "no client level";
            } else {
                List<ArmorStand> stands = client.level.getEntitiesOfClass(ArmorStand.class, standSearchBox());

                if (stands.size() != 1) {
                    problem = "the client sees " + stands.size() + " armour stands in front of the camera, "
                            + "expected exactly one";
                } else {
                    ArmorStand stand = stands.get(0);

                    for (EquipmentSlot slot : new EquipmentSlot[]{
                            EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        ItemStack piece = stand.getItemBySlot(slot);

                        if (piece.isEmpty()) {
                            problem = "the armour stand wears nothing in " + slot;
                            break;
                        }

                        if (piece.get(DataComponents.TRIM) == null) {
                            problem = "the piece in " + slot + " has no trim, and the mixin only touches the "
                                    + "light of the trim submit";
                            break;
                        }

                        int actual = GlowingTrimUtils.getGlowLevel(piece);

                        if (actual != glowLevel) {
                            problem = "the piece in " + slot + " reports glow level " + actual
                                    + " instead of " + glowLevel;
                            break;
                        }
                    }
                }
            }

            if (problem != null) {
                throw new AssertionError("Glowing trim trigger conditions not met: " + problem);
            }
        });
    }

    private static AABB standSearchBox() {
        return new AABB(STAND_X - 2.0, STAND_Y - 2.0, STAND_Z - 2.0, STAND_X + 2.0, STAND_Y + 4.0, STAND_Z + 2.0);
    }

    /**
     * Waits until the mixin's sine has just entered its bright (or dim) end. It first waits for the
     * sine to be well away from that end, so the screenshot is always taken at the <em>start</em>
     * of the window and not somewhere near its end: entering at 0.95 leaves roughly 400 ms before
     * the value moves appreciably, which is far more than a frame - and more than the frame or two
     * a NeoForge screenshot needs to leave the GPU, since {@code Screenshot.takeScreenshot} there
     * only calls back once the buffer copy has completed.
     */
    private static void waitForPulse(Script script, boolean bright) {
        waitForSine(script, sine -> bright ? sine < -0.5 : sine > 0.5, bright, "leaving");
        waitForSine(script, sine -> bright ? sine > 0.95 : sine < -0.95, bright, "entering");
    }

    /**
     * One half of a pulse wait, as a step.
     *
     * <p><b>The budget is wall clock, inside a step whose own budget is ticks.</b> The sine the
     * mixin reads is wall clock as well, so a client that does not tick at 20 Hz would otherwise
     * change how many periods a fixed tick budget covers - the Fabric-only class made exactly this
     * point and blocked on {@code System.currentTimeMillis}. A step list cannot block, so the
     * deadline moved inside the condition: it is armed on the first tick of the step and throws the
     * original message when it passes. The step's own tick budget sits far above it and is only the
     * backstop for a client that has stopped ticking, where the wall clock deadline would never be
     * evaluated again.
     *
     * <p>The deadline lives in a one element array because a step's body is registered before
     * anything runs and has to be a lambda; each call here registers its own step and its own
     * array, and every step runs exactly once.
     */
    private static void waitForSine(Script script, DoublePredicate reached, boolean bright, String stage) {
        long[] deadline = {0L};

        script.await("wait for the pulse " + stage + " its " + (bright ? "bright" : "dim") + " end",
                PULSE_WAIT_TICK_BUDGET, client -> {
                    if (deadline[0] == 0L) {
                        deadline[0] = System.currentTimeMillis() + PULSE_WAIT_MILLIS;
                    }

                    if (reached.test(Math.sin(System.currentTimeMillis() / PULSE_DIVISOR_MILLIS))) {
                        return true;
                    }

                    if (System.currentTimeMillis() > deadline[0]) {
                        throw new AssertionError("Waiting for the pulse phase timed out while " + stage
                                + " the " + (bright ? "bright" : "dim") + " end. The sine in this test uses "
                                + "the divisor " + PULSE_DIVISOR_MILLIS + " ms; if the mixin no longer does, "
                                + "the two cannot line up.");
                    }

                    return false;
                });
    }
}
