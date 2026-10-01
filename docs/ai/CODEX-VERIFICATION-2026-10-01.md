# Vereinte Feature-Welle – 2026-10-01

Ausgang `821dd131`, vereinter Quellstand `5f294df9`. Plan:
`docs/ai/CODEX-PLAN.md`. Claims bleiben AUS; offene Aktivierungsblocker stehen in
`modules/simpletweaks/CLAIMS-STAGE4.md`. Forge wird separat weitergeführt.

- Claims 1–4: Merge `bbfc39c6`, Worker `71753fe2`; Zugriffe/Portale 5–6:
  Merge `dc147181`, Worker `2a878f7a`. Beide Worker-checks Exit 0.
- Dimensions-Einstellungen: Merge `5bb8c442`, Worker `8657e3d2`; UI-Test:
  Merge `295ad36a`, Worker `385e4e57`. UI-Test nur kompiliert.
- QoL/Sounds: Merge `6bb478cd`, Worker `7faca095`, Worker-check Exit 0.
- Gemeinsame komplette Tweaks-/Dimensions-Kataloge auf Fabric und NeoForge:
  **alles gruen: 176/176 bestanden, 0 rot**, Run
  `2026-10-01T00-27-20Z-0efa`, Gate-Worktree auf `5f294df9`.
  Tweaks 47 je Loader, Dimensions 41 je Loader. Ergebniszeile gelesen.
- Zehn Produktions-JARs gebaut (`JAR_EXIT=0`) und verschachtelte Archive geprüft:
  SimpleBuilding, Tweaks, Dimensions, Sounds und Visuals, jeweils Fabric/NeoForge.
  Alle enthalten genau eine Framework-0.1.1-JAR mit `Protection.class` und
  `CosmeticIntensity.class`. Beide SimpleBuilding-JARs enthalten außerdem
  `FrameworkProtection.class` und den passenden ServiceLoader-Eintrag.
- Acht geänderte Sprachdateien: keine doppelten JSON-Schlüssel, EN/DE-Schlüssel
  je Modul identisch. Merge-Prüfungen für Manifest, Wiki, Quests, Wiki-Tests,
  Bücher und Texturen grün. Beide Claims-/Dimensions-Testkataloge erhalten.
- Unabhängige rein statische Review auf `5f294df9`: keine zusätzlichen konkret
  belegten Defekte im vereinbarten Umfang. Keine eigene Ausführung durch Reviewer.
- Nicht geprüft: tatsächlicher Dimensions-Dialog, Client-Audio/Visuals,
  Besitzerwelt und andere Minecraft-Laufzeitlinien. Besitzer-Client läuft;
  keine Clients gestartet oder gestoppt. Claims sind nicht vollständig abgesichert.

Das vollständige Gate mit Hauptlinie und allen Integrations-/Modulsuiten sowie
der Push erfordern einen eigenen grünen Lauf auf der endgültig gewählten SHA.
Einzel- oder Modulprüfungen ersetzen diesen Nachweis nicht.
