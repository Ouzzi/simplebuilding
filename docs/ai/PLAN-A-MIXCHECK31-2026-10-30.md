# Plan A-MIXCHECK31 (30.10.): Client-Mixins statisch prüfen

Anlass: Besitzer-Client (runIntegrationClient) stürzte ab, `LeggingsTextureMixin` @Shadow `resourceId()` fehlt in
26.3 `SimpleTexture` (Mixin in 7830ff3d8 entfernt). Gametests laufen serverseitig, Client-Mixins werden dort nie
angewendet.

1. `tools/check_client_mixins.py`: liest die in `fabric.mod.json` genannten Mixin-Configs aller 26.3-Fabric-Ausgaben,
   parst die kompilierten Mixin-Klassen (eigener Classfile-Parser) und prüft gegen das Spieljar
   (`minecraft-merged-deobf-26.3`) + Bibliotheken: Zielklasse, @Shadow/@Overwrite, @Accessor/@Invoker,
   Injektor-Zielmethoden, @At INVOKE/FIELD/NEW als Instruktion in der Zielmethode, @Inject-Handler-Argumente.
2. Gradle-Task `:integration:checkClientMixins` (Klassenpfad der Integration = 26.3-Jar), in `check` eingehängt
   (damit auch in bprep/Gate).
3. Lauf auf sb-test, Treffer beheben (26.3-Ziel suchen, sonst Mixin raus + Schalter aus + Queue).
4. Einmal echter Client-Start headless (Integration-Client-Gametest, alle Module geladen) bis Welt betreten.
5. Leggings-Fix BS29-4 für 26.3 neu bauen, falls sauberer Einhängepunkt, sonst Queue.
Risiko: Fehlalarme des Prüfers (Hierarchie, MixinExtras-Sonderfälle) → an echten Treffern abgleichen.
