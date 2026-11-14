# PLAN-B-ZFIGHT43 (14.11.2026) – N43 Z-Fighting + N45 Dino-Teilhitboxen

Branch `b-zfight43`, Basis `hub/b-bundle192` (e487fcbf4). Wortlaut: /root/b-auftrag-20261109.md „## N43“, „## N45“.

## Ist-Stand (erhoben)
- Modelle sind Java-`LayerDefinition`s. Simple Animals / Simple Dinos: generiert aus `modules/<mod>/tools/gen_models.py` (Spec `B(pos,size,paint,grow)`,
  `grow` -> `CubeDeformation`); Dinos zusätzlich `entity/part/*Bones.java` (gleiche Knochen für den Server). Köpfe: handgemacht in
  `simplemobheads/.../CustomHeadModels.java` (Kern-Zwilling `common/.../tweaks/client/ModSkullModels.java`), Mobs in `simplemobs/.../client/*Model.java`.
- Kein Prüfwerkzeug für Java-Modelle (nur `tools/check_item_zfight.py` für JSON).
- Entity-Layer zeichnen in 26.x ohne Rückseiten-Culling (`entityCutout`; `entityCutoutCull` ist die Ausnahme, BS25-18).
- Teil-Hitboxen: `PartRig.headBoxes/tailBoxes` liefern je WÜRFEL eine AABB (T. rex 3+3, Triceratops 14+3, Stegosaurus 8+23, Parasaurolophus 9+3).
  Utahraptor: nur Ei/Art (`DinoKind.UTAHRAPTOR`), kein Modell, keine Entity -> nur Muster dokumentieren.

## Ursachen (Prüf-Skript, vorher 87 Funde in 14 Modellen, nachher 0)
- Schleimkopf: äußere durchscheinende Hülle (`entityTranslucent`, ohne Culling) liegt mit der Unterseite genau auf dem Boden (y=0) bzw. mit
  der Rückseite an der Wand (z=+4) -> flackert mit der Blockoberfläche (Boden/Wand). Gilt für Modul und Kern-Zwilling.
- Igel: Stachelkleid (`spines`) und Körper gleich lang (z −4,5..4,5) -> Vorder-/Rückfläche koplanar.
- T. rex: Arme bündig mit der Rumpfseite; Hals und Kopf gleich breit (Seiten koplanar); Kopfpanzer hinten bündig mit dem Kopf.
- Triceratops: Hals/Kopf gleich breit; hintere Kragenplatten (Rand + breiter Rand) gleich tief (621 px² Vorder- und Rückseite!);
  Horn-Kappen oben bündig mit der Hornspitze; Fußsohle und Schienbein unten bündig.
- Stegosaurus: Platten bündig mit Buckel/Schwanz-Ende, zwei Platten seitlich bündig, Sattel/Buckel-Unterseite, Pflug-Querbalken, Füße.
- Parasaurolophus: Unterkiefer so breit wie die Schnauze, Schnabel unten bündig, Rückenpanzer-Seitenplatten hinten bündig, Füße (unten, hinten).
- Weitere Funde der Familie: Wildschwein (Hauer vorn bündig mit der Schnauze), Krabben (Ober-/Unterschenkel, Augenstiele), Hirsch (Ohren,
  Geweihsprossen), Shulker-Kopf (Kopf unten bündig mit dem Deckel, verdeckt), Brimguard (Hutband/Krone, Kopf/Rumpf hinten), Howler (Zähne).
- Nicht statisch lesbar (Schleifen über Arrays): Nibblemite, Dragonmite -> als „skip“ gelistet.

## Umsetzung
1. `tools/check_model_zfight.py`: liest alle Java-Modelle (`addOrReplaceChild`), posiert die Teile (Offset, Rotation ZYX, Skalierung,
   `CubeDeformation`), meldet koplanare, gleich gerichtete, überlappende Flächen verschiedener Würfel; bei durchscheinenden Schichten auch
   gegenüberliegende Flächen und Boden/Wand-Kontakt von Köpfen. Exit 1 bei Funden. In AGENTS.md erwähnt.
2. Behebung mit kleinstem Versatz, nur auf der Achse der Fläche (Generator: `grow` darf jetzt ein Tupel (x,y,z) sein):
   Dinos ±0,1 px (Maßstab 0,75, große Sichtweite), kleine Tiere ±0,02 px, Igel-Stacheln 0,05 px, Schleimhülle −0,05 px, Mobs ±0,02 px.
   Regel: der Würfel, der sichtbar sein soll, wächst (+) – so entstehen keine Spalten; ein ganz verdeckter Würfel schrumpft (−).
   UVs bleiben (CubeDeformation ändert keine UVs), Texturen unverändert.
3. N45 `PartRig`: Kopf = EINE Box (Hülle aller Kopfketten-Würfel: Hals, Kopf, Kiefer, Hörner, Kragen, Kamm), Schwanz = EINE Box (Hülle der
   Schwanzglieder ab dem zweiten + was daran hängt, z. B. Stego-Stacheln; das erste Glied sitzt direkt hinter der Kernbox). Rumpf = Vanilla-Box.
   Verhalten bleibt: Kopf zieht sich vor Wand/Spieler zurück bzw. stoppt (solidHead), Schwanz biegt aus; Reichweite/Höhe wie vorher
   (Hülle hat dieselben Extremwerte). Gametests: Zahl der Boxen = 1 + 1 prüfen (`dino_part_boxes_minimal`).
   Python-Nachbau (`/root/sb-tools-b/b14/zfight43/sim45.py`) der Tests: alle Wand-/Säulen-/Spieler-Szenarien bestehen mit der Hülle;
   `stegosaurus_head_and_tail_parts` war mit Einzelwürfeln rot (gestreckter Kopf berührte den Spieler nicht) und ist mit der Hülle grün.
4. Vorschau ZFIGHT43: Software-Render (isometrisch, Python) vorher/nachher je Modell mit rot markierten Fundflächen + Skript-Ausgabe.

## Risiken
- Hülle statt Einzelboxen ist etwas größer (Bereich unter dem Kopf vor dem Hals) -> Kopf zieht sich bei Hindernissen dort früher zurück. Gewollt (weniger Boxen).
- Parallel `b-fix192` (rote Dino-Tests): Konflikt möglich in DinosTests/PartRig – im Bericht genannt.

## Verifikation
- Lokal: `check_model_zfight.py` 0 Funde (außer skip), beide `gen_models.py --check`, `check_data.py` der Module, javac nicht nötig (sbt).
- EIN sbt-Lauf: Kern `:compileJava`, `:mc26_3:fabric:compileJava`, Module simpledinos/simpleanimals/simplemobheads/simplemobs Fabric-compile,
  Gametests `module-simpledinos-fabric-263,module-simpledinos-standalone-fabric-263` + Kopf-/Tier-/Mob-Ziele.

## Abweichungen / Entscheidungen bei der Umsetzung
- Triceratops-Kragenrand: zuerst nur z +0,1 -> neuer Fund (Unterseite bündig mit der vorderen Kragenplatte) -> Rand (0, 0,1, 0,1).
- Stego-Platten: `plates()`/`row()` nehmen optional ein `grow` für den Plattenfuß (nur die drei betroffenen Platten).
- Scope über die Wortlaut-Modelle hinaus: alle Funde der Familie behoben (Wildschwein, Krabben, Hirsch, Shulker-Kopf, Brimguard, Howler),
  weil das Skript sonst rot bleibt; alle Korrekturen ≤ 0,1 px, Texturen/UVs unverändert.
- Schwanzbox ab dem 2. Glied (statt ganzer Schwanz): eine Hülle über den ganzen gebogenen Schwanz deckt die Körpermitte immer mit ab, Ausbiegen
  hilft dann kaum; die Schwanzwurzel liegt direkt hinter der Kernbox.
