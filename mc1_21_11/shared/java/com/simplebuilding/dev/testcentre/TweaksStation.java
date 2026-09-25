package com.simplebuilding.dev.testcentre;

import com.simplebuilding.items.CreativeTabLayout;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Station "tweaks" der Testzentrale: die aus Simple Tweaks uebernommenen Pads, Platten und Werkzeuge.
 * An der Wand alle Tab-Zeilen der Familie als Rahmen; davor je Familie ein Vorfuehrstueck zum
 * Ausprobieren (Teleporter, Elytra-Pad, Flypad, Launchpad mit Windkugeln, Druckplatten an Lampen,
 * Filterplatten mit Fass darunter, Leitstein fuer den Echo-Kompass).
 *
 * <p>Der Chunk-Loader steht bewusst nur im Rahmen: gesetzt wuerde er beim Bau Chunks erzwingen.
 * Die Kupferplatte ist die oxidierte Stufe, damit sie waehrend des Bautests nicht weiter altert.
 */
public final class TweaksStation {

    /** Tab-Zeilen, die diese Station zeigt (fehlen deshalb unter "devices"). */
    public static final Set<String> ROWS = TweaksItems.functionalRows().stream()
            .map(CreativeTabLayout.Row::name).collect(Collectors.toUnmodifiableSet());

    private TweaksStation() {
    }

    public static TcCanvas build(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 4;
        int floorZ = 2;

        List<TcCanvas.Line> lines = new ArrayList<>();
        for (CreativeTabLayout.Row row : TweaksItems.functionalRows()) {
            List<ItemStack> stacks = row.stacks().stream().filter(s -> !s.isEmpty() && !TcContext.isSpacer(s.getItem())).toList();
            lines.add(new TcCanvas.Line(TcText.t("tweaks.row." + row.name(), row.name().replace('_', ' ')), stacks));
        }
        int panelTop = lines.size() + 1;
        c.title(0, panelTop + 1, wallZ, TcText.t("section.tweaks", "Simple Tweaks"), TcText.t("section.tweaks.sub", "pads and plates"));
        int end = c.rowsPanel(0, panelTop, wallZ, lines);

        int x = 1;
        x = station(c, x, floorZ, wallZ, TweaksBlocks.SPAWN_TELEPORTER,
                TcText.t("tweaks.teleporter", "Teleporter"), TcText.t("tweaks.teleporter.1", "stand still 5 s"),
                TcText.t("tweaks.teleporter.2", "to spawn 1"));
        x = station(c, x, floorZ, wallZ, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER,
                TcText.t("tweaks.teleporter_home", "Enderite Tp."), TcText.t("tweaks.teleporter_home.1", "stand still 3 s"),
                TcText.t("tweaks.teleporter_home.2", "to your bed"));
        x = station(c, x, floorZ, wallZ, TweaksBlocks.ELYTRA_PAD,
                TcText.t("tweaks.elytra_pad", "Elytra Pad"), TcText.t("tweaks.elytra_pad.1", "empty chest slot"),
                TcText.t("tweaks.elytra_pad.2", "gets an elytra"));
        x = station(c, x, floorZ, wallZ, TweaksBlocks.FLYPAD,
                TcText.t("tweaks.flypad", "Flypad"), TcText.t("tweaks.flypad.1", "flight nearby"));

        // Launchpad mit einer Truhe Windkugeln daneben.
        int launch = x;
        x = station(c, x, floorZ, wallZ, TweaksBlocks.LAUNCHPAD,
                TcText.t("tweaks.launchpad", "Launchpad"), TcText.t("tweaks.launchpad.1", "load wind charges"),
                TcText.t("tweaks.launchpad.2", "stand 3 s"));
        c.place(launch, 0, floorZ + 1, Blocks.CHEST);
        c.contents(launch, 0, floorZ + 1, List.of(new ItemStack(Items.WIND_CHARGE, 64)));

        x = plateAtLamp(c, x, floorZ, wallZ, TweaksBlocks.DIAMOND_PRESSURE_PLATE, false,
                TcText.t("tweaks.diamond_plate", "Diamond Plate"), TcText.t("tweaks.diamond_plate.1", "players only"));
        x = plateAtLamp(c, x, floorZ, wallZ, TweaksBlocks.NETHERITE_PRESSURE_PLATE, true,
                TcText.t("tweaks.netherite_plate", "Netherite Plate"), TcText.t("tweaks.netherite_plate.1", "barrel below:"),
                TcText.t("tweaks.netherite_plate.2", "carry a diamond"));
        x = plateAtLamp(c, x, floorZ, wallZ, TweaksBlocks.ENDERITE_PRESSURE_PLATE, true,
                TcText.t("tweaks.enderite_plate", "Enderite Plate"), TcText.t("tweaks.enderite_plate.1", "owner lock"),
                TcText.t("tweaks.enderite_plate.2", "+ barrel list"));
        x = plateAtLamp(c, x, floorZ, wallZ, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE, false,
                TcText.t("tweaks.copper_plate", "Copper Plate"), TcText.t("tweaks.copper_plate.1", "stand 4 s"));

        // Leitstein fuer den Echo-Kompass, Enderperlen daneben.
        int echo = x;
        x = station(c, x, floorZ, wallZ, Blocks.LODESTONE,
                TcText.t("tweaks.echo", "Echo Compass"), TcText.t("tweaks.echo.1", "link: lodestone"),
                TcText.t("tweaks.echo.2", "1 pearl a jump"));
        c.place(echo, 0, floorZ + 1, Blocks.CHEST);
        c.contents(echo, 0, floorZ + 1, List.of(new ItemStack(TweaksItems.ECHO_COMPASS), new ItemStack(Items.ENDER_PEARL, 16)));

        c.backWall(0, Math.max(end, x), wallZ, panelTop + 3);
        return c;
    }

    /** Ein Block auf dem Boden, darueber an der Wand sein Schild. Liefert die naechste freie Spalte. */
    private static int station(TcCanvas c, int x, int floorZ, int wallZ, Block block, Component... lines) {
        c.place(x, 0, floorZ, block);
        c.wallSign(x, 1, wallZ, lines);
        return x + 2;
    }

    /** Druckplatte mit Lampe dahinter; Filterplatten mit einem Fass (ein Diamant) darunter. */
    private static int plateAtLamp(TcCanvas c, int x, int floorZ, int wallZ, Block plate, boolean barrel, Component... lines) {
        if (barrel) {
            c.place(x, -1, floorZ, Blocks.BARREL);
            c.contents(x, -1, floorZ, List.of(new ItemStack(Items.DIAMOND)));
        }
        c.place(x, 0, floorZ + 1, Blocks.REDSTONE_LAMP);
        return station(c, x, floorZ, wallZ, plate, lines);
    }
}
