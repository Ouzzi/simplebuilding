# Audit Simple Stats (Client + Server) - `simplestats` (b-w3b, 14.11.2026, Paket simplestats-1)

Basis `hub/b-bundle192` e487fcbf4, Branch `b-w3b`. Nachgeholte Release-Prüfung C1-C9 (Muster der Audits 31.10./12.11., z. B. `modules/simpleoptserver/AUDIT.md`).
Gelesen ohne Gradle: Struktur und Kernstellen (`SimpleStats`, `StatsConfig`, `StatsComponents`, `StatsRuntime` Tore/Puffer/Tick/Leave, `StatsCommands`, `MobKeys`, `StatNames`, alle 12 Server-Mixins und die 3 Client-Mixins als Liste mit Einhängestellen, `fabric.mod.json`, `simplestats.mixins.json`, Lang EN/DE, `StatsTests`-Katalog). **Nicht** vollständig Zeile für Zeile gelesen (rund 2 900 Zeilen Java); die Tabelle sagt je Punkt, was belegt ist.

## Ergebnis C1-C9

| Punkt | Ergebnis |
|---|---|
| C1 Code | ok nach Stichprobe. Zählung ist ereignisgetrieben (`StatsRuntime`): Änderungen je Item sammeln sich in einem `Delta` und werden einmal am Ende des Server-Ticks in die Komponente geschrieben (ein Hammer-Abbau von 25 Blöcken = 1 Schreibvorgang, Test `mining_bundling`); Strecken/Tragezeit alle 200 Ticks (`ACCRUE_TICKS`, Zeitraum auf 4 x 200 Ticks begrenzt). Größe je Item begrenzt (`maxBytes` 2 048 bis 262 144, neue Schlüssel laufen danach in `other`, Invariante getestet). Wert einer neueren Schema-Version wird gelesen, nie verändert (`foreign_version`). Tore (`counts`): Schalter, Kreativ/Zuschauer, Maschinen-Spieler. Hinweis P3: `PENDING` ist ein statischer `IdentityHashMap` ohne Synchronisation; er wird nur auf dem Server-Thread benutzt. |
| C2 Mixins | 12 Server-Mixins (`ItemStack.mineBlock`, `LivingEntity.die/applyItemBlocking/stopFallFlying`, `Player.startFallFlying`, `ServerPlayer.die/awardStat`, `ProjectileWeaponItem.shoot`, `ThrownTrident.<init>`, `Projectile.spawnProjectile`, `FishingRodHookedTrigger.trigger`, `BrushableBlockEntity`, `FireworkRocketEntity.<init>`, `MinecraftServer.tickServer`, `PlayerList.remove`) und 3 Client-Mixins (Tooltip, Minecraft, Container-Bildschirm). **Fix (simplestats-1):** `simplestats.mixins.json` stand auf `required: true`/`defaultRequire: 1` - ein Mojang-Patch an einer Einhängestelle hätte den Start abgebrochen. Jetzt `required: false`/`defaultRequire: 0`: der betroffene Zähler fällt aus (Warnung im Log), alles andere läuft. Test `mixins_not_required`. Die Forge-Konfiguration (`RegistryLoadTaskMixin`, Forge-Port) bleibt unverändert (Ports erst am Ende). |
| C3 Config | ok: `config/simplestats.json` (`enabled`, sechs `track*`-Schalter, `countCreative`, `countFakePlayers`, `trackPlayerKills`, `maxBytes` geklemmt 2 048..262 144 in `normalize`), Datei über 64 KiB oder kaputt = Standardwerte (`read`), `/simplestats reload`. **Neu (simplestats-4):** Mod-Menü-Bildschirm und `/simplestats config` über den SimpleLib-Baustein, Lang EN/DE für Titel und alle 11 Optionen (Test `config_labels` auf Fabric). |
| C4 Daten | ok: keine Rezepte/Loot/Tags außer Leitfaden-Fortschritt; Lang EN/DE gleiche Schlüssel (Test `variant_names_german` prüft das für die Mob-Varianten, simplestats-3). Komponente `simplestats:item_stats` mit `persistent` (Codec, bereinigt über `StatData.sanitize` mit harten Grenzen) und `networkSynchronized`. |
| C5 Unabhängigkeit | ok: `depends` nur Fabric Loader, Minecraft, Java, Fabric API; `suggests simplebuilding`. SimpleLib ist per Jar-in-Jar eingebettet (`include project(':modules:simplelib:fabric')`), nicht als eigene Abhängigkeit. Läuft allein (Standalone-Test `module-simplestats-standalone-fabric-263`). |
| C6 A12 | Eingriff: mittel. Die Mod ändert kein Spielverhalten, sie zählt nur und hängt Daten an Items. Vanilla-Nutzen: reine Anzeige (Tooltip bei Shift, Statistik-Fenster Taste I), Schalter je Kategorie. Standardwerte sind Vanilla-nah (Kreativ/Zuschauer und Maschinen zählen nicht). |
| C7 Netzwerk | Keine eigenen Pakete. Die Komponente ist synchronisiert (Spieler sehen die Zahlen am Item): ein Vanilla-Client ohne die Mod kennt den Komponententyp nicht - Mod auf dem Client nötig. Das steht auf der Mod-Seite ("Environment: client and server"); Server-Befehle nur für Spielleiter (Stufe 2). |
| C8 Lizenz/Assets | Lizenz `LicenseRef-SimpleMods-ARR` (wie die Familie), Icons 16 px aus `icon.layers.json` und 128-px-Kopie (Prüfung `check_mod_icons.py` grün), Leitfaden-Buch-Textur aus `guide_book.layers.json`. Keine Secrets/IPs im gelesenen Code. |
| C9 Offen | kein TODO/FIXME im Mod-Code (grep). Offen bleiben die Im-Spiel-Prüfungen (Paket simplestats-2: Tooltip/Fenster mit echter Maus, Schild/Raketen/Hammer real zählen) und der echte Client-Start im Bündel (`mixinaudit-client-263`). |

## Befunde

- simplestats-P2-1: Mixin-Konfiguration `required:true` - Absturz beim Start nach Minecraft-Änderung an einer Einhängestelle. **gefixt** (simplestats-1).
- simplestats-P3-1: Tragezeit im 10-s-Raster (200 Ticks) und Absorption als Näherung - gewollt, im Handbuch genannt.
- simplestats-P3-2: Der Zähler der letzten Ticks vor einem harten Server-Ende (ohne `PlayerList.remove`) wird nicht geschrieben; höchstens ein Tick Zählung. Hinweis.
- simplestats-P3-3: Mob-Varianten ohne Vanilla-Variantenkomponente (Mods) zählen unter ihrer Entity-ID, Namen ohne Lang-Schlüssel fallen auf lesbares Englisch zurück (`StatNames.title`). Hinweis.

## Urteil

**shipbar** (0 P1, 1 P2 gefixt, 3 P3 Hinweis). Vor der Veröffentlichung: Im-Spiel-Prüfung (simplestats-2) und echter Client-Start im Bündel.

## Release-Checkliste Simple Stats 1.0.0

- [x] Version 1.0.0 (`modules.json`, `moduleVersion` über `gradle/module-fabric.gradle`, Changelog-Kopf)
- [x] Icon 16 px + 128 px (`fabric.mod.json` Größenliste), Lizenz, Kontakt/Quellen
- [x] Mixin-Konfiguration nicht erforderlich (Absturz-Schutz), Test `mixins_not_required`
- [x] Mod-Menü-Bildschirm (SimpleLib), Lang EN/DE, Test `config_labels`
- [x] Variantennamen Deutsch, Test `variant_names_german`
- [x] Seitentext und Changelog (`docs/release/pages/simplestats.md`, `docs/release/CHANGELOG-simplestats.md`)
- [ ] Im Spiel: Tooltip (Shift), Statistik-Fenster (Taste I), Schild/Elytra-Raketen/SB-Hammer (Paket simplestats-2)
- [ ] Echter Client-Start `mixinaudit-client-263` im Bündel, Gate grün
- [ ] Besitzer-Abnahme, Freigabe "ja" vor dem Hochladen
