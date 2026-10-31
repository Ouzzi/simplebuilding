# Audit Simple Opt Render (Client) - `simpleoptrender` (b-rel32-opt, 31.10.2026)

Basis b-rel32 d9cda1e21, Branch `b-rel32-opt`. Gelesen ohne Gradle: jede Datei vollstaendig, dazu `framework/.../OptAreas`, `OptCompat`, `OptSettings`, `ConfigFiles`, `SubMods`; MC-Klassen per `javap` gegen das 26.3-Jar (Unterschriften, Null-Pruefungen).

## Durchgesehene Dateien (Zeilen)
- `clienttest/java/com/simplebuilding/modules/simpleoptrender/ModuleClientSmoke.java` (12)
- `fabric/build.gradle` (7)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptrender/ModuleGameTest.java` (18)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptrender/fabric/MixinGuard.java` (96)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptrender/fabric/MixinGuardTests.java` (74)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptrender/fabric/OptTargetScan.java` (48)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptrender/fabric/RenderOptFabric.java` (15)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptrender/fabric/RenderOptMixinPlugin.java` (36)
- `fabric/src/main/resources/fabric.mod.json` (35)
- `shared/java/com/simplebuilding/modules/simpleoptrender/Details.java` (22)
- `shared/java/com/simplebuilding/modules/simpleoptrender/FastIpPing.java` (27)
- `shared/java/com/simplebuilding/modules/simpleoptrender/LeafCulling.java` (30)
- `shared/java/com/simplebuilding/modules/simpleoptrender/LightmapCache.java` (27)
- `shared/java/com/simplebuilding/modules/simpleoptrender/MoreCulling.java` (48)
- `shared/java/com/simplebuilding/modules/simpleoptrender/RenderOpt.java` (144)
- `shared/java/com/simplebuilding/modules/simpleoptrender/RenderOptTests.java` (221)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/details/FogRendererMixin.java` (32)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/details/ParticleEngineMixin.java` (18)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/details/TextureAtlasMixin.java` (17)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/fast_ip_ping/ServerNameResolverMixin.java` (36)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/leaf_culling/ModelBlockRendererMixin.java` (27)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/lightmap_cache/LightmapMixin.java` (45)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/more_culling/AbstractSignRendererMixin.java` (36)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/more_culling/BeaconRendererMixin.java` (28)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/more_culling/EntityRendererMixin.java` (28)
- `shared/java/com/simplebuilding/modules/simpleoptrender/mixin/no_telemetry/ClientTelemetryManagerMixin.java` (18)
- `shared/resources/assets/simpleoptrender/icon.layers.json` (100)
- `shared/resources/assets/simpleoptrender/icon.png` (189 B)
- `shared/resources/assets/simpleoptrender/icon_128.png` (518 B)
- `shared/resources/assets/simpleoptrender/lang/de_de.json` (9)
- `shared/resources/assets/simpleoptrender/lang/en_us.json` (9)
- `shared/resources/data/simpleoptrender/priorities.json` (11)
- `shared/resources/data/simpleoptrender/structure/empty.nbt` (1398 B)
- `shared/resources/simpleoptrender.mixins.json` (24)
- `tools/check_data.py` (21)
- `wiki/manual.json` (137)

## Ergebnis C1-C9

| Punkt | Ergebnis |
|---|---|
| C1 Code | ok, jede Datei gelesen. Hot paths ohne Allokation: `RenderOpt.live` (Identitaetsschleife), `LightmapMixin` nutzt zwei feste Arrays je Instanz. `LeafCulling` liest hoechstens `depth`(<=4) Bloecke in Blickrichtung, bleibt im Mesh-Region-Rand (16er-Sektion, neighbor + 4). Client-Klassen nur in Mixins (`Minecraft`, `Frustum`, Renderer), Rest reines Java/common - Dedicated Server laedt das Jar ohne Client-Klassen (Gametests laufen so). Mixins greifen per Capture-Pfade nicht auf Servertypen. `BeaconRendererMixin`: `getCullFrustum()` null-sicher. `AbstractSignRendererMixin`: Renderer `submit` pruefte `frontText/backText` auf null (per javap auf 26.3-Client-Jar bestaetigt). |
| C2 Mixins | Datei `simpleoptrender.mixins.json`: `required:false`, `defaultRequire:0`, 10 Client-Mixins, Plugin mit `MixinGuard` (nicht angewendeter Injector schaltet den Bereich aus, kein Absturz) und Zielpruefung (`OptTargetScan`, `acceptTargets`). Je Mixin (Ziel -> Konfliktrisiko): `ModelBlockRendererMixin` (`shouldRenderFace`, HEAD, cancellable) - Sodium/Iris: Sodium baut Meshes selbst, Bereich O1 geht bei `sodium` laut Tabelle aus; Fabric-Rendering-API-Mods koennen dieselbe Methode haben, Zielpruefung schaltet dann ab. `LightmapMixin` (`Lightmap.render`, HEAD) - ueberspringt Upload nur bei Aenderung <0,5/255, Iris/Sodium lesen dieselbe Textur, kein Konflikt erwartet; BadOptimizations gewinnt. `ServerNameResolverMixin` (2x `@WrapOperation`) - nur bei `ServerAddressResolver.SYSTEM`, Fast IP Ping gewinnt. `ClientTelemetryManagerMixin` (`createEventSender`, HEAD) - No Telemetry gewinnt. `AbstractSignRendererMixin` (`extractRenderState`, TAIL) - Sodium/Iris rendern Schilder ueber die Vanilla-BE-Renderer, kompatibel; More Culling gewinnt. `BeaconRendererMixin` (`shouldRender`, RETURN) - Iris-Schattenpass: Strahl wirft keinen Schatten, ok. `EntityRendererMixin` (`shouldRender`, HEAD; `ItemFrameRenderer` ueberschreibt sie nicht) - Entity Culling (nicht in der Liste, hakt dieselbe Klasse): Zielpruefung schaltet O3 ab. `ParticleEngineMixin` (`add`), `TextureAtlasMixin` (`cycleAnimationFrames`), `FogRendererMixin` (`setupFog`, RETURN) - Standardwert true = Vanilla; Iris haengt an `FogRenderer`: Zielpruefung kann `details` auf conflict setzen (Log nennt den Mod; `mode ours`/`conflictScan false` hebt auf). Signaturen gegen 26.3 mit `tools/check_client_mixins.py` im Testlauf (siehe opt.md). |
| C3 Config | ok: `config/simpleoptrender.json` (Bereiche mit `mode` auto/ours/theirs/off, `depth` 0..4 geklemmt, Schalter signText/itemFrames/beaconBeams, particles/textureAnimations/fog), `conflictScan`. Werte werden geklemmt (`OptSettings.intValue`), kaputte Datei -> Quarantaene + Defaults. Befehl `/simpleclient simpleoptrender`. Lang EN/DE vorhanden (Bereichsnamen); die Optionsschluessel selbst stehen nur in JSON (keine Autoconfig-Labels), Beschreibung in `wiki/manual.json` EN/DE. |
| C4 Daten | ok: keine Rezepte/Tags/Loot. Lang EN/DE gleiche Schluessel. Icons vorhanden. |
| C5 Unabhaengigkeit | ok: nur simplelib + Fabric Loader/MC; Rivalen nur ueber `FabricLoader.isModLoaded` (keine Klassenzugriffe). `environment: *` bleibt absichtlich (Super-Mod nestet das Jar auch auf Servern; ein `client`-Env koennte dort die Abhaengigkeit des Super-Mods unerfuellt lassen). Auf dem Dedicated Server wird nur die Config-Datei angelegt. |
| C6 A12 | Vanilla-Nutzen/Eingriff: O1 Blaetter-Culling (kein Loch, aussen bleibt Cutout; mittlerer Eingriff in den Mesher), O7 Lichtkarte (nur Upload ueberspringen, Bild identisch), M1 IP-Ping, M2 Telemetrie aus (Datenschutz), O3 (nur Sicht-Culling), O9 `details` alles Standard true = Vanilla. Alles pro Bereich abschaltbar (`mode off`). |
| C7 Netzwerk | n/a (nur Client, keine Pakete). M1 greift nur lokal in die Adressaufloesung. |
| C8 Lizenz/Assets | ok - CC0 (LICENSE, `license` in fabric.mod.json), keine Mojang-Pixel (Icon 16x16/128x128 aus `icon.layers.json` erzeugt), keine Secrets/IPs/Pfade im Code oder in Ressourcen (nur `127.0.0.1`/`[::1]` in Tests). |
| C9 Offen | kein TODO/FIXME. O2/O6 (Entity-/GUI-Batching) laut Konzept Stufe 5 nicht gebaut. |

## Befunde

- simpleoptrender-P3-1: `OptSettings.load` schrieb die Config nicht atomar (framework). Fix: `ConfigFiles.writeAtomic`, Commit 2e29b17fb.
- simpleoptrender-P3-2: `fabric.mod.json`-Beschreibung nannte More Culling und Detail-Schalter nicht. Fix: Commit 64e80c0cc.
- simpleoptrender-P3-3 (Restpunkt, Rueckfrage): Sodium/Iris-Koexistenz ist nur ueber Zielpruefung und Rivalenliste abgesichert, `compat-opt-263` (Cull Leaves/More Culling/BadOptimizations/ServerCore) wurde nie gefahren und enthaelt weder Sodium noch Iris. Vor Release echter Client-Lauf mit Sodium (+Iris) noetig (opt.md, QOLA-OPT-R10).
- simpleoptrender-P3-4 (Hinweis): `details.fog=false` hebt auch Nebel in der Nether/Boss-Atmosphaere auf (nicht nur Fernsicht-Nebel); Default true = Vanilla, daher nur Wunsch-Option.
- simpleoptrender-P3-5 (Hinweis): Zielpruefung vergleicht Klassen, nicht Methoden; mehrere weit verbreitete Mods (z. B. Iris auf `FogRenderer`) koennen einen Bereich daher vorsorglich abschalten. Bewusst konservativ (Konzept 4.5).
Keine P1/P2.

## Urteil
**shipbar (Vorbehalt: Client-Lauf mit Sodium/Iris vor Veroeffentlichung, siehe P3-3)**
