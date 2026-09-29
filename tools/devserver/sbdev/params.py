"""
Annahmen der Rechner: wie oft öffnet ein Spieler welche Kisten, wie oft kommt der fahrende
Händler, wie viele Dorfbewohner bildet er aus, wie viele Mobs toetet er ...

Die Standardwerte kommen aus docs/KERNE-SELTENHEIT.md, Abschnitt 2 ("gezielt", Mittel- bis
Endspiel) - dort nicht genannte Strukturen sind hier geschätzt und so markiert. Jede Annahme ist
ein Wert der Kategorie "param" (apply = "tool"): editierbar, versioniert wie alles andere, wirkt
nie in der Mod.

Gezielt vs. normal: "gezielt" = der Spieler sucht genau diese Quelle; "normal" = er kommt vorbei.
Normal = gezielt x normalFactor (Standard 0,22, also etwa 4,5-mal langsamer - Abschnitt 2 sagt
"ein Viertel bis Fünftel"), ausser eine eigene Normal-Rate ist gesetzt.
"""

from __future__ import annotations

from .values import value

# Struktur -> Behälter -> {Tabellen: Mischgewicht, Rate (Öffnungen/h gezielt), Herkunft}
# Mischgewicht "vault.rareShare": ein normaler Tresor würfelt reward_rare mit 80 % (Vanilla reward.json 8:2).
STRUCTURES = {
    "woodland_mansion": {"label": "Waldanwesen", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/woodland_mansion": 1.0}, "rate": 8.0,
                  "why": "1 Anwesen je 2,5 h (Erkunderkarte), ~20 Truhen (KERNE-SELTENHEIT 2)"}}},
    "mineshaft": {"label": "Verlassene Mine", "containers": {
        "chest": {"label": "Kistenlore", "tables": {"minecraft:chests/abandoned_mineshaft": 1.0}, "rate": 6.0,
                  "why": "~6 Kistenloren je Stunde gezielt (KERNE-SELTENHEIT 5.3)"}}},
    "bastion": {"label": "Bastion", "containers": {
        "other": {"label": "übrige Truhen", "tables": {"minecraft:chests/bastion_other": 1.0}, "rate": 6.0,
                  "why": "1,5 Bastionen/h, ~4 Truhen (KERNE-SELTENHEIT 2)"},
        "treasure": {"label": "Schatzraum", "tables": {"minecraft:chests/bastion_treasure": 1.0}, "rate": 0.375,
                     "why": "jede 4. Bastion hat den Schatzraum (KERNE-SELTENHEIT 2)"}}},
    "fortress": {"label": "Netherfestung", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/nether_bridge": 1.0}, "rate": 4.0,
                  "why": "1 Festung je 45 min, ~3 Truhen (KERNE-SELTENHEIT 2)"}}},
    "trial_chambers": {"label": "Prüfungskammer", "containers": {
        "vault": {"label": "Tresor", "tables": {"minecraft:chests/trial_chambers/reward_rare": "vault.rareShare",
                                                 "minecraft:chests/trial_chambers/reward_common": "vault.commonShare"},
                  "rate": 4.0, "why": "1 Kammer je 1,5 h, ~6 Tresore (KERNE-SELTENHEIT 2)"},
        "ominous": {"label": "unheilvoller Tresor", "tables": {"minecraft:chests/trial_chambers/reward_ominous": 1.0},
                    "rate": 1.33, "why": "~2 unheilvolle je Kammer (KERNE-SELTENHEIT 2)"}}},
    "end_city": {"label": "Endsiedlung", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/end_city_treasure": 1.0}, "rate": 15.0,
                  "why": "mit Elytren 1 Stadt je 20 min, ~5 Truhen (KERNE-SELTENHEIT 2)"}}},
    "ancient_city": {"label": "Antike Stätte", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/ancient_city": 1.0}, "rate": 8.0,
                  "why": "geschätzt: 1 Stadt je 1,5 h, ~12 Truhen"}}},
    "stronghold": {"label": "Festung (Bibliothek)", "containers": {
        "library": {"label": "Bibliothekstruhe", "tables": {"minecraft:chests/stronghold_library": 1.0}, "rate": 1.0,
                    "why": "geschätzt: 1 Festung je 2 h, 1-2 Bibliothekstruhen"}}},
    "outpost": {"label": "Plünderer-Außenposten", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/pillager_outpost": 1.0}, "rate": 2.0,
                  "why": "geschätzt: 2 Außenposten je Stunde, 1 Truhe"}}},
    "buried_treasure": {"label": "Vergrabener Schatz", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/buried_treasure": 1.0}, "rate": 1.0,
                  "why": "geschätzt: 1 Schatzkarte je Stunde"}}},
    "dungeon": {"label": "Verlies", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/simple_dungeon": 1.0}, "rate": 2.0,
                  "why": "geschätzt: 1-2 Verliese je Stunde Höhlenerkundung"}}},
    "shipwreck": {"label": "Schiffswrack", "containers": {
        "treasure": {"label": "Schatztruhe", "tables": {"minecraft:chests/shipwreck_treasure": 1.0}, "rate": 1.5,
                     "why": "geschätzt: 1,5 Wracks je Stunde auf See"}}},
    "igloo": {"label": "Iglu", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/igloo_chest": 1.0}, "rate": 0.3,
                  "why": "geschätzt: selten, nur Schneebiome"}}},
    "ruined_portal": {"label": "Portalruine", "containers": {
        "chest": {"label": "Truhe", "tables": {"minecraft:chests/ruined_portal": 1.0}, "rate": 2.0,
                  "why": "geschätzt: 2 Ruinen je Stunde"}}},
    "fishing": {"label": "Angeln", "containers": {
        "treasure": {"label": "Schatzfang", "tables": {"minecraft:gameplay/fishing/treasure": 1.0}, "rate": 6.0,
                     "why": "geschätzt: ~120 Fänge/h, ~5 % Schatz ohne Glück des Meeres"}}},
}

# Einzelne Items, für die das Kernmodell eine andere Rate annimmt (KERNE-SELTENHEIT 2:
# "Schatz-Bastion gezielt: 1 je 1,5 h").
ITEM_RATE_OVERRIDES = {
    ("bastion", "treasure", "simplebuilding:netherite_core"): 0.67,
}

VILLAGER_TRAINED_PER_HOUR = {1: 3.0, 2: 1.5, 3: 1.0, 4: 0.6, 5: 0.4}

ERAS = [
    {"id": "early", "name": "Early Game, erste Basis", "hours": 2},
    {"id": "village", "name": "Dorf & Handel", "hours": 4},
    {"id": "diamond", "name": "Diamantzeit, Zaubertisch", "hours": 8},
    {"id": "nether", "name": "Nether-Einstieg", "hours": 12},
    {"id": "brewing", "name": "Braustand & Tränke", "hours": 15},
    {"id": "trial", "name": "Diamantrüstung, Prüfungskammern", "hours": 15},
    {"id": "netherite", "name": "Late Game, Netherit", "hours": 25},
    {"id": "dragon", "name": "Enderdrache besiegt", "hours": 30},
    {"id": "endcities", "name": "End-Städte & Elytren", "hours": 35},
    {"id": "wither", "name": "Wither, Beacon, Großbauten", "hours": 45},
]


def table_structures() -> dict[str, list[tuple[str, str]]]:
    """Beutetabelle -> [(Struktur, Behälter)]"""
    out: dict[str, list[tuple[str, str]]] = {}
    for skey, struct in STRUCTURES.items():
        for ckey, container in struct["containers"].items():
            for table in container["tables"]:
                out.setdefault(table, []).append((skey, ckey))
    return out


def param_records(mob_victims: list[str], ore_blocks: list[str]) -> list[dict]:
    recs = []

    def p(key, label, kind, current, group, lo=0.0, hi=None, unit="", note="", nullable=False):
        recs.append(value(f"param:{key}", "param", label, kind, current, group=group, min=lo, max=hi,
                          apply="tool", unit=unit, note=note, nullable=nullable, source={"file": "tools/devserver/sbdev/params.py"}))

    p("normalFactor", "Faktor normales Spiel", "float", 0.22, "Allgemein", 0.01, 1.0,
      note="normal = gezielt x Faktor (KERNE-SELTENHEIT 2: 'ein Viertel bis Fünftel')")
    p("eraTargetRatio", "Zielanteil am Zeitalter", "float", 0.85, "Allgemein", 0.05, 2.0,
      note="Rückwärts-Rechner: mittlere Zeit bis zum ersten Stück = Anteil x Zeitalter (Regel aus KERNE-SELTENHEIT 5.3)")
    p("vault.rareShare", "Anteil 'selten' je normalem Tresor", "prob", 0.8, "Allgemein",
      note="Vanilla reward.json: rare 8 zu common 2")
    for skey, struct in STRUCTURES.items():
        for ckey, container in struct["containers"].items():
            group = struct["label"]
            p(f"rate.{skey}.{ckey}", f"{container['label']}: Öffnungen/h gezielt", "float", container["rate"], group,
              0.0, 1000.0, "/h", container["why"])
            p(f"rateNormal.{skey}.{ckey}", f"{container['label']}: Öffnungen/h normal", "float", None, group, 0.0, 1000.0,
              "/h", "leer = gezielt x Faktor normales Spiel", nullable=True)
    for (skey, ckey, item), rate in ITEM_RATE_OVERRIDES.items():
        p(f"rate.{skey}.{ckey}@{item}", f"{STRUCTURES[skey]['containers'][ckey]['label']}: Öffnungen/h gezielt nur für {item.split(':')[1]}",
          "float", rate, STRUCTURES[skey]["label"], 0.0, 1000.0, "/h", "KERNE-SELTENHEIT 2: Schatz-Bastion gezielt 1 je 1,5 h")
    p("trader.visitsPerHour", "Besuche des fahrenden Händlers/h gezielt", "float", 1.0, "Händler", 0.0, 100.0, "/h",
      "KERNE-SELTENHEIT 3: ein Händler je Spielstunde")
    p("traderNormal.visitsPerHour", "Besuche des fahrenden Händlers/h normal", "float", 0.75, "Händler", 0.0, 100.0, "/h",
      "geschätzt; der Händler kommt von selbst, darum kaum weniger als gezielt")
    p("villager.restocksPerHour", "Auffüllungen je Dorfbewohner/h", "float", 2.0, "Dorfbewohner", 0.0, 100.0, "/h",
      "bis zu zweimal je Ingame-Tag (20 min), wenn er arbeitet")
    for level, rate in VILLAGER_TRAINED_PER_HOUR.items():
        p(f"villager.trainedPerHour.{level}", f"ausgebildete Dorfbewohner Stufe {level}/h", "float", rate, "Dorfbewohner",
          0.0, 100.0, "/h", "geschätzt: wie viele neue Dorfbewohner des Berufs man je Stunde auf diese Stufe bringt")
    for victim in mob_victims:
        p(f"mob.charged_creeper.{victim}", f"Tötungen durch geladenen Creeper: {victim}/h", "float", 0.25, "Mobs",
          0.0, 1000.0, "/h", "geschätzt: geladene Creeper sind selten (Gewitter + Blitz)")
    for block in ore_blocks:
        p(f"block.{block}", f"abgebaute {block.split(':')[1]}/h", "float", 30.0, "Blöcke", 0.0, 10000.0, "/h",
          "geschätzt: Erzblöcke je Stunde gezielter Suche")
    p("eras", "Zeitalter", "json", ERAS, "Zeitalter", None, None,
      note="Richtwerte für einen durchschnittlichen Spieler (Gesamtspielzeit), KERNE-SELTENHEIT 5.1")
    return recs
