package com.simplebuilding.modules.simpletweaks.deed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The pure helpers of the editor: inserting people, line offsets, preview parse and status line, pin/display of identities. */
class DeedTextTest {
    private static final UUID OWNER = new UUID(1, 1), STEVE = new UUID(2, 2), ALEX = new UUID(3, 3);

    @Test
    void addPersonGoesIntoTheTopPartBeforeAnySection() {
        String code = "# head\n*: U\n[1,2]\nSteve: B\n";
        assertEquals("# head\n*: U\nLena: U\n[1,2]\nSteve: B\n", DeedText.addPerson(code, "Lena", "U"));
        assertEquals("*: U\nLena: U\n", DeedText.addPerson("*: U", "Lena", "U"));
        assertEquals("Lena: U\n", DeedText.addPerson("", "Lena", "U"));
    }

    @Test
    void addPersonKeepsExistingPeopleAndTheLimits() {
        String code = "Lena: B\n";
        assertEquals(code, DeedText.addPerson(code, "lena", "U"), "same person, any case: unchanged");
        assertEquals(code, DeedText.addPerson(code, "", "U"));
        String full = "#".repeat(ClaimCode.MAX_CHARS - 1);
        assertEquals(full, DeedText.addPerson(full, "Lena", "U"), "would exceed 4096 characters: unchanged");
        assertTrue(DeedText.hasPerson("  Steve : BD # note", "steve"));
        assertFalse(DeedText.hasPerson("# Steve: BD", "Steve"), "a comment is no person");
    }

    @Test
    void lineStartFindsSourceLines() {
        String code = "ab\ncd\n\nef";
        assertEquals(0, DeedText.lineStart(code, 1));
        assertEquals(3, DeedText.lineStart(code, 2));
        assertEquals(6, DeedText.lineStart(code, 3));
        assertEquals(7, DeedText.lineStart(code, 4));
        assertEquals(-1, DeedText.lineStart(code, 5));
        assertEquals(-1, DeedText.lineStart(code, 0));
        assertEquals(4, DeedText.lineCount(code));
        assertEquals(1, DeedText.lineCount(""));
    }

    @Test
    void previewKnowsOnlineNamesAndOwner() {
        var known = List.of("Steve", "Alex");
        var ok = DeedText.preview("Steve: BDU\nalex: @member\n", OWNER, "Ouzzi", "minecraft:overworld", known, 8);
        assertTrue(ok.ok());
        var status = DeedText.status(ok);
        assertEquals(new DeedText.Status(true, 0, 0, "", "", 2, 0, 0), status);
        var bad = DeedText.preview("Steve: BDU\nTomm: U\nOuzzi: B\nSteve: Q\n", OWNER, "Ouzzi", "minecraft:overworld", known, 8);
        assertFalse(bad.ok());
        var s = DeedText.status(bad);
        assertEquals(2, s.firstLine());
        assertEquals("unknown_name", s.firstKey());
        assertEquals("Tomm", s.firstArg());
        assertTrue(bad.errors().stream().anyMatch(e -> e.line() == 3 && e.key().equals("owner")));
        assertTrue(bad.errors().stream().anyMatch(e -> e.line() == 4 && e.key().equals("unknown_letter")));
    }

    @Test
    void previewCountsSectionsAndHonorsThePeopleLimit() {
        var p = DeedText.preview("Steve: U\n[3,4]\nSteve: B\n", OWNER, "Ouzzi", "minecraft:overworld", List.of("Steve"), 8);
        assertEquals(1, DeedText.status(p).sections());
        var many = DeedText.preview("Steve: U\nAlex: U\n", OWNER, "Ouzzi", "minecraft:overworld", List.of("Steve", "Alex"), 1);
        assertEquals("too_many", DeedText.status(many).firstKey());
    }

    @Test
    void pinStoresUuidsAndKeepsCommentsAndLayout() {
        ClaimCode.Names names = n -> n.equalsIgnoreCase("Steve") ? STEVE : n.equalsIgnoreCase("Alex") ? ALEX : null;
        String code = "# head  Steve: not a line\n*: U\n  Steve : BDU   # friend\n" + ALEX + ": UC\n[1,2]\nalex: @member\n";
        String pinned = ClaimCode.pin(code, names);
        assertEquals("# head  Steve: not a line\n*: U\n  " + STEVE + " : BDU   # friend\n" + ALEX + ": UC\n[1,2]\n" + ALEX + ": @member\n", pinned);
        assertEquals(pinned, ClaimCode.pin(pinned, names), "idempotent");
    }

    @Test
    void pinKeepsNamesThatWouldBreakTheLimits() {
        ClaimCode.Names names = n -> STEVE;
        String longLine = "Steve: " + "B".repeat(ClaimCode.MAX_LINE - 7 - 1) + "\n";
        assertEquals(longLine, ClaimCode.pin(longLine, names), "a line over 96 characters keeps the name");
        String nearFull = "#".repeat(ClaimCode.MAX_CHARS - 20) + "\nSteve: U\n";
        assertEquals(nearFull, ClaimCode.pin(nearFull, names), "over 4096 characters overall: unchanged");
    }

    @Test
    void displayShowsNamesOnlyWhenTheyResolveBack() {
        Map<UUID,String> names = Map.of(STEVE, "Steve", ALEX, "Alex");
        ClaimCode.Names back = n -> names.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(n)).map(Map.Entry::getKey).findFirst().orElse(null);
        String code = "# c\n" + STEVE + ": BDU\n" + ALEX + ": @helper # x\n";
        assertEquals("# c\nSteve: BDU\nAlex: @helper # x\n", ClaimCode.display(code, names::get, back));
        // a name that now belongs to somebody else stays a UUID
        ClaimCode.Names stolen = n -> n.equalsIgnoreCase("Steve") ? ALEX : back.resolve(n);
        assertEquals("# c\n" + STEVE + ": BDU\nAlex: @helper # x\n", ClaimCode.display(code, names::get, stolen));
        // unknown name: untouched
        assertEquals(code, ClaimCode.display(code, id -> null, back));
        // display then parse gives the same rules as the UUID code
        var a = ClaimCode.parse(code, OWNER, "minecraft:overworld", back, 8);
        var b = ClaimCode.parse(ClaimCode.display(code, names::get, back), OWNER, "minecraft:overworld", back, 8);
        assertTrue(a.ok() && b.ok());
        assertEquals(a.top(), b.top());
    }
}
