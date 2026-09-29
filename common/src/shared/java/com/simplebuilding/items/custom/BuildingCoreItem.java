package com.simplebuilding.items.custom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Baukern (Kupfer bis Enderit): nicht stapelbar; ein Rechtsklick spielt eine kurze Animation ohne
 * Spielwirkung (Besitzer 2026-09-28). Welche, entscheidet ein Wurf mit den Gewichten 70/20/10; seit
 * 2026-09-29 (Besitzer: "cooler, kleineres Ausholen, gern kuerzere Abklingzeit") laufen alle drei als
 * kleine Choreografie ueber mehrere Ticks, die der Server taktet ({@link #tickAnimations}):
 * <ul>
 *   <li>{@link Animation#GLOW} (70 %): Funken schiessen in die Hand (3 Ticks Ausholen), dann ein
 *       Leuchtring in der Farbe des Kerns quer zum Blick, Amethyst-Klingen, und ein leiser Nachhall-Ring.</li>
 *   <li>{@link Animation#ORBIT} (20 %): nach dem Ausholen schraubt sich ein Funkenpaar als Doppelspirale
 *       von den Fuessen bis ueber den Kopf, begleitet von einem aufsteigenden Glockenspiel-Arpeggio;
 *       oben eine Krone aus Endstab-Funken.</li>
 *   <li>{@link Animation#BURST} (10 %): zweistufiges Aufladen (Seelenanker-Klang), dann ein Stern mit
 *       acht Strahlen wie der Netherstern, Feuerwerks- und Totem-Funken, Knall und Leuchtfeuer; danach
 *       Glitzerregen und ein Ring, der am Boden auslaeuft.</li>
 * </ul>
 * Partikel und Klang schickt der Server an alle Spieler in der Naehe ({@code sendParticles},
 * {@code playSound(null, ...)}); der Client schwingt nur die Hand. Jede Animation endet vor dem Ende der
 * Abklingzeit ({@link #duration}), damit sich zwei nie ueberlagern.
 *
 * <p>Auf einem Block, in dem Erze entstehen, hat jeder Klick zusaetzlich die kleine Chance der
 * {@link CoreOreTransmutation} (Osterei, Besitzer 2026-09-29); der Kern wird dabei nicht verbraucht.
 */
public class BuildingCoreItem extends Item {
    /** Abklingzeit nach einer Animation, in Ticks (1 s; bis 2026-09-29 1,5 s). */
    public static final int COOLDOWN_TICKS = 20;

    /** Die drei Animationen, in Wurf-Reihenfolge; die Gewichte summieren sich zu 100. */
    public enum Animation {
        GLOW(70), ORBIT(20), BURST(10);

        public final int weight;

        Animation(int weight) {
            this.weight = weight;
        }
    }

    /** Farbe des Kerns fuer den Staub (RGB). */
    private final int color;
    /** Erz-Chance dieses Kerns: 1 zu N je Klick auf einen Wirtsblock (Tabelle in {@link CoreOreTransmutation}). */
    private final int oreChanceOneIn;

    public BuildingCoreItem(Properties properties, int color, int oreChanceOneIn) {
        super(properties);
        this.color = color;
        this.oreChanceOneIn = oreChanceOneIn;
    }

    public int color() {
        return color;
    }

    /** "1 zu N" je Klick, dass ein Wirtsblock zu Erz wird. */
    public int oreChanceOneIn() {
        return oreChanceOneIn;
    }

    /** Die Animation zu einem Wurf {@code roll} aus 0..99: 0-69 Leuchten, 70-89 Kreisen, 90-99 Stern. */
    public static Animation fromRoll(int roll) {
        int bound = 0;
        for (Animation animation : Animation.values()) {
            bound += animation.weight;
            if (roll < bound) {
                return animation;
            }
        }
        return Animation.BURST;
    }

    /** Wuerfelt eine Animation mit den Gewichten 70/20/10. */
    public static Animation roll(RandomSource random) {
        return fromRoll(random.nextInt(100));
    }

    /** Tick des letzten Schritts einer Animation, gezaehlt ab dem Klick. */
    public static int duration(Animation animation) {
        return switch (animation) {
            case GLOW -> GLOW_ECHO;
            case ORBIT -> ORBIT_CROWN;
            case BURST -> BURST_RIPPLE;
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            activate(server, player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Klick auf einen Block: dieselbe Animation wie in die Luft, dazu die Erz-Chance, wenn der Spieler
     * hier bauen darf (Abenteuermodus, Spawnschutz). Die Abklingzeit prueft Vanilla vor diesem Aufruf.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (context.getLevel() instanceof ServerLevel server) {
            activate(server, player, stack);
            transmuteOnClick(server, player, stack, context.getClickedPos(), context.getClickedFace(), oreChanceOneIn);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Der Erz-Teil eines Blockklicks: nur wenn der Spieler hier bauen darf (Abenteuermodus, Spawnschutz),
     * dann die Chance "1 zu {@code oneIn}". Kein Text an den Spieler (Besitzer-Regel: Gadgets zeigen
     * nichts im Chat oder ueber der Schnellleiste). Oeffentlich, damit die Spieltests den echten Pfad mit
     * erzwungener Chance fahren koennen.
     */
    public static void transmuteOnClick(ServerLevel server, Player player, ItemStack stack, BlockPos pos,
                                        net.minecraft.core.Direction face, int oneIn) {
        if (player.mayBuild() && player.mayUseItemAt(pos, face, stack) && server.mayInteract(player, pos)) {
            CoreOreTransmutation.tryTransmute(server, pos, oneIn, player.getRandom());
        }
    }

    private void activate(ServerLevel level, Player player, ItemStack stack) {
        play(level, player, roll(player.getRandom()), color);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
    }

    // ---------------------------------------------------------------------------------------------
    // Choreografie. Die Zeiten sind Ticks ab dem Klick; jeder Schritt liest die Position des Spielers
    // erst, wenn er dran ist, damit die Effekte mitlaufen.

    private static final int GLOW_PULSE = 3;
    private static final int GLOW_ECHO = 6;
    private static final int ORBIT_STEPS = 12;
    private static final int ORBIT_CROWN = ORBIT_STEPS + 1;
    private static final int BURST_SECOND_CHARGE = 2;
    private static final int BURST_BANG = 4;
    private static final int BURST_GLITTER = 8;
    private static final int BURST_RIPPLE = 12;
    /** Arpeggio der Spirale: Grundton, grosse Terz, Quinte, Oktave. */
    private static final float[] ORBIT_NOTES = {1.0F, 1.26F, 1.498F, 2.0F};

    /** Spielt {@code animation} am Spieler: Partikel und Klang fuer alle in der Naehe, ueber mehrere Ticks. */
    public static void play(ServerLevel level, Player player, Animation animation, int color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
        DustParticleOptions fineDust = new DustParticleOptions(color, 0.6F);
        RandomSource random = player.getRandom();
        switch (animation) {
            case GLOW -> {
                later(level, player, 0, () -> {
                    Vec3 hand = hand(player);
                    converge(level, ParticleTypes.ELECTRIC_SPARK, hand, 8, 0.5, 0.12);
                    level.sendParticles(dust, hand.x, hand.y, hand.z, 3, 0.05, 0.05, 0.05, 0.0);
                    sound(level, hand, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 1.8F);
                });
                float pitch = 1.1F + random.nextFloat() * 0.3F;
                later(level, player, GLOW_PULSE, () -> {
                    Vec3 hand = hand(player);
                    ringAcrossLook(level, player, dust, hand, 0.3, 12, 0.0);
                    level.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 6, 0.18, 0.18, 0.18, 0.02);
                    sound(level, hand, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, pitch);
                });
                later(level, player, GLOW_ECHO, () -> {
                    Vec3 hand = hand(player);
                    ringAcrossLook(level, player, fineDust, hand, 0.55, 16, 0.0);
                    level.sendParticles(ParticleTypes.WAX_ON, hand.x, hand.y, hand.z, 3, 0.3, 0.3, 0.3, 0.0);
                    sound(level, hand, SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, pitch + 0.5F);
                });
            }
            case ORBIT -> {
                later(level, player, 0, () -> {
                    Vec3 hand = hand(player);
                    converge(level, ParticleTypes.ELECTRIC_SPARK, hand, 12, 0.55, 0.13);
                    sound(level, hand, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.7F);
                });
                for (int step = 1; step <= ORBIT_STEPS; step++) {
                    int k = step;
                    later(level, player, k, () -> {
                        double height = player.getBbHeight() + 0.3;
                        double y = player.getY() + 0.1 + height * k / ORBIT_STEPS;
                        double angle = k * 0.8;
                        for (int arm = 0; arm < 2; arm++) {
                            double a = angle + arm * Math.PI;
                            double x = player.getX() + Math.cos(a) * 0.95;
                            double z = player.getZ() + Math.sin(a) * 0.95;
                            burstOne(level, ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0, 0);
                            burstOne(level, dust, x, y, z, 0, 0, 0, 0);
                        }
                        if (k % 3 == 1) {
                            level.playSound(null, player.getX(), y, player.getZ(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS,
                                    0.5F, ORBIT_NOTES[k / 3]);
                        }
                    });
                }
                later(level, player, ORBIT_CROWN, () -> {
                    Vec3 crown = new Vec3(player.getX(), player.getY() + player.getBbHeight() + 0.35, player.getZ());
                    ringFlat(level, ParticleTypes.END_ROD, crown, 0.6, 16, 0.06);
                    ringFlat(level, dust, crown, 0.45, 8, 0.0);
                    level.sendParticles(ParticleTypes.GLOW, crown.x, crown.y, crown.z, 6, 0.3, 0.1, 0.3, 0.01);
                    sound(level, crown, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
                });
            }
            case BURST -> {
                later(level, player, 0, () -> {
                    Vec3 hand = hand(player);
                    converge(level, ParticleTypes.END_ROD, hand, 16, 0.9, 0.16);
                    sound(level, hand, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6F, 1.5F);
                });
                later(level, player, BURST_SECOND_CHARGE, () -> {
                    Vec3 hand = hand(player);
                    converge(level, ParticleTypes.ELECTRIC_SPARK, hand, 10, 0.45, 0.1);
                    sound(level, hand, SoundEvents.BEACON_POWER_SELECT, 0.4F, 1.8F);
                });
                later(level, player, BURST_BANG, () -> {
                    Vec3 hand = hand(player);
                    Vec3 look = player.getLookAngle();
                    Vec3 side = sideOf(look);
                    Vec3 up = side.cross(look).normalize();
                    // Acht Strahlen wie der Netherstern: vier lange quer zum Blick, vier kurze diagonal.
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        Vec3 ray = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                        int steps = i % 2 == 0 ? 6 : 3;
                        for (int step = 1; step <= steps; step++) {
                            double d = step * 0.17;
                            burstOne(level, ParticleTypes.END_ROD, hand.x + ray.x * d, hand.y + ray.y * d, hand.z + ray.z * d,
                                    ray.x, ray.y, ray.z, 0.035 * step);
                        }
                    }
                    level.sendParticles(ParticleTypes.FIREWORK, hand.x, hand.y, hand.z, 28, 0.1, 0.1, 0.1, 0.2);
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, hand.x, hand.y, hand.z, 16, 0.1, 0.1, 0.1, 0.3);
                    level.sendParticles(dust, hand.x, hand.y, hand.z, 20, 0.4, 0.4, 0.4, 0.0);
                    sound(level, hand, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8F, 1.3F);
                    sound(level, hand, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.6F);
                    sound(level, hand, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F);
                });
                later(level, player, BURST_GLITTER, () -> {
                    Vec3 chest = new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.75, player.getZ());
                    level.sendParticles(fineDust, chest.x, chest.y, chest.z, 24, 1.0, 0.6, 1.0, 0.0);
                    level.sendParticles(ParticleTypes.GLOW, chest.x, chest.y, chest.z, 10, 0.9, 0.5, 0.9, 0.0);
                    sound(level, chest, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.7F, 1.2F);
                });
                later(level, player, BURST_RIPPLE, () -> {
                    Vec3 feet = new Vec3(player.getX(), player.getY() + 0.1, player.getZ());
                    ringFlat(level, dust, feet, 1.3, 24, 0.0);
                    ringFlat(level, ParticleTypes.END_ROD, feet, 0.6, 8, 0.1);
                    sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.9F);
                });
            }
        }
    }

    private static Vec3 hand(Player player) {
        Vec3 look = player.getLookAngle();
        return player.getEyePosition().add(look.x * 0.7, look.y * 0.7 - 0.35, look.z * 0.7);
    }

    /** Waagerechte Querachse zum Blick; beim Blick senkrecht nach oben/unten die x-Achse. */
    private static Vec3 sideOf(Vec3 look) {
        Vec3 side = new Vec3(-look.z, 0, look.x);
        return side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
    }

    private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** {@code count} Partikel auf einer Kugel vom Radius {@code radius}, die in die Mitte fliegen. */
    private static void converge(ServerLevel level, ParticleOptions particle, Vec3 center, int count, double radius, double speed) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            double y = 1 - 2 * (i + 0.5) / count;
            double r = Math.sqrt(1 - y * y);
            double a = golden * i;
            Vec3 dir = new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
            burstOne(level, particle, center.x + dir.x * radius, center.y + dir.y * radius, center.z + dir.z * radius,
                    -dir.x, -dir.y, -dir.z, speed);
        }
    }

    /** Ring quer zum Blick des Spielers um {@code center}; {@code speed} > 0 laesst ihn nach aussen laufen. */
    private static void ringAcrossLook(ServerLevel level, Player player, ParticleOptions particle, Vec3 center,
                                       double radius, int points, double speed) {
        Vec3 look = player.getLookAngle();
        Vec3 side = sideOf(look);
        Vec3 up = side.cross(look).normalize();
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            Vec3 dir = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            burstOne(level, particle, center.x + dir.x * radius, center.y + dir.y * radius, center.z + dir.z * radius,
                    dir.x, dir.y, dir.z, speed);
        }
    }

    /** Waagerechter Ring um {@code center}. */
    private static void ringFlat(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            double dx = Math.cos(a);
            double dz = Math.sin(a);
            burstOne(level, particle, center.x + dx * radius, center.y, center.z + dz * radius, dx, 0, dz, speed);
        }
    }

    /** Ein Partikel mit fester Bewegung: {@code count = 0} macht aus dem Versatz die Richtung. */
    private static void burstOne(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                 double dx, double dy, double dz, double speed) {
        level.sendParticles(particle, x, y, z, 0, dx, dy, dz, speed);
    }

    // ---------------------------------------------------------------------------------------------
    // Taktgeber: die Schritte einer Animation warten hier auf ihren Server-Tick. Fabric ruft
    // tickAnimations aus END_SERVER_TICK, NeoForge/Forge aus ServerTickEvent.Post. Nur der Server-Thread
    // fasst die Liste an.

    private record Step(MinecraftServer server, UUID player, int due, Runnable action) {
    }

    private static final List<Step> PENDING = new ArrayList<>();

    /** Fuehrt {@code action} {@code delay} Ticks spaeter aus, solange der Spieler dann noch in dieser Welt ist. */
    private static void later(ServerLevel level, Player player, int delay, Runnable action) {
        Runnable guarded = () -> {
            if (!player.isRemoved() && player.level() == level) {
                action.run();
            }
        };
        if (delay <= 0) {
            guarded.run();
            return;
        }
        MinecraftServer server = level.getServer();
        PENDING.add(new Step(server, player.getUUID(), server.getTickCount() + delay, guarded));
    }

    /** Einmal pro Server-Tick: faellige Schritte ausfuehren, Schritte eines alten Servers verwerfen. */
    public static void tickAnimations(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        List<Step> due = new ArrayList<>();
        Iterator<Step> it = PENDING.iterator();
        while (it.hasNext()) {
            Step step = it.next();
            if (step.server() != server) {
                it.remove();
            } else if (step.due() <= now) {
                due.add(step);
                it.remove();
            }
        }
        for (Step step : due) {
            step.action().run();
        }
    }

    /** Wie viele Animationsschritte fuer {@code player} noch ausstehen (fuer die Spieltests). */
    public static int pendingSteps(Player player) {
        int count = 0;
        for (Step step : PENDING) {
            if (step.player().equals(player.getUUID())) {
                count++;
            }
        }
        return count;
    }
}
