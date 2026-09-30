# Voicebridge: Handy und Laptop (MVP, 2026-09-30)

Gebaut: tools/voicebridge, Python-Standardbibliothek und eine PWA mit grosser
Sprechtaste, Browser-STT und Vorlesen. Standard ist **Lesemodus**. Kein pip erforderlich.
Eigene headless Unterhaltung, keine Fernsteuerung einer bereits offenen CLI-Sitzung.
Der PC muss eingeschaltet und erreichbar bleiben.

## PC starten
1. Im gewuenschten Checkout AGENTS.md lesen. Python 3.12+ verwenden.
2. tools/voicebridge/projects.example.json nach tools/voicebridge/projects.json
   kopieren, sofern die lokale Datei noch nicht existiert. Ohne lokale Datei gilt das Beispiel.
3. Pro Projekt name, root, provider, model, default_mode und brief_folder einstellen.
   Relative Roots beziehen sich auf den Repo-Root. Andere Projekte duerfen absolute
   lokale Roots verwenden. Namen: kurze ASCII-Namen, z.B. simplebuilding oder website.
   default_mode bleibt read-only.
4. `python tools/voicebridge/server.py` starten. Standard: **127.0.0.1:8772**.
5. Die ausgegebene URL `http://127.0.0.1:8772/#token=...` im PC-Browser oeffnen.
   Token wird einmal erzeugt, lokal gespeichert und aus der Adresszeile entfernt.
   Die Seite merkt ihn per localStorage. Nicht weitergeben.
6. Grosse Taste antippen, Mikrofon erlauben, sprechen, nochmal tippen.
   Waehrend du sprichst, schweigt die Bridge. Danach wird die Antwort vorgelesen.
   Am Laptop ist Space dieselbe Taste, ausser in Formularfeldern/Buttons.

Optionen: --port 8772, --host 100.x.y.z oder --tailscale. Externe LAN-/Wildcard-
Bind-Adressen werden abgelehnt. --tailscale fragt tailscale ip -4 ab und akzeptiert
nur 100.64.0.0/10. MagicDNS wird aus tailscale status --json fuer Host-Pruefung ermittelt.

## Handy ueber Tailscale HTTPS: genaue Reihenfolge
1. Tailscale am PC und Handy installieren, beide im selben Tailnet anmelden.
2. Bridge wie oben auf **127.0.0.1** starten; fuer diesen Proxy-Weg kein --tailscale.
3. Zweite PC-Konsole: `tailscale serve --bg http://127.0.0.1:8772`.
   Falls aufgefordert, HTTPS fuer das Tailnet ueber den angebotenen Link aktivieren.
   `tailscale serve status` zeigt die private HTTPS-Adresse. Kein Portforwarding/Funnel.
4. Handy: Tailscale einschalten, Android Chrome oder iOS Safari oeffnen:
   `https://<PC-Name>.<Tailnet>.ts.net/#token=<Token-aus-PC-Konsole>`.
   Die Bridge gibt diese zweite URL aus, wenn MagicDNS bereits erkannt wurde.
   Bei spaeter aktivierter MagicDNS-Konfiguration die Bridge neu starten.
5. Mikrofon erlauben, Sprechtaste testen. Android: Browser-Menue → App installieren /
   Zum Startbildschirm hinzufuegen. iPhone: Teilen → Zum Home-Bildschirm.
   Bei Bedarf die installierte Seite einmal mit der Token-URL oeffnen: iOS kann
   Safari-/Home-Bildschirm-Speicher trennen.
6. Headset verbinden und im Vordergrund testen. Media Session bildet Play/Pause
   auf Sprechen und Next/Stop auf Stopp ab, soweit Browser/Headset mitmachen.
   Sperrbildschirm-Aufnahme ist **nicht garantiert**.
7. Proxy ausschalten: `tailscale serve reset` setzt die lokale Serve-Konfiguration
   zurueck; vorher andere Serve-Dienste beruecksichtigen.

HTTP auf einer 100.x-IP reicht fuer viele Handy-Browser nicht: Mikrofon, Wake Lock,
Service Worker und Installation brauchen HTTPS. Tailscale verschluesselt den Transport,
der Browser erkennt eine HTTP-IP trotzdem nicht als sicheren Kontext.
Offizielle Einrichtung: [Tailscale Serve](https://tailscale.com/docs/reference/tailscale-cli/serve).

## Anbieter, Modell und Unterhaltung
- **claude** ist Standard. Native CLI installieren und am PC anmelden. model leer
  bedeutet CLI-Standard; sonst einen fuer den Account verfuegbaren Modellnamen angeben.
  Die Bridge prueft zuerst claude --help, ohne Agentenstart. Fehlen restricted/tools/
  plan/resume, wird abgebrochen. Hier war Claude nicht installiert; Flags gegen die
  [offizielle CLI-Referenz](https://code.claude.com/docs/en/cli-reference) geprueft.
  Verwendet: -p, JSON, --resume, --permission-mode plan, restricted mode, nur
  Read/Glob/Grep, keine MCP-Werkzeuge, Skills oder Hooks.
- **codex**: native CLI oder normale npm-Installation, PC-Login. Lokale Hilfen fuer
  codex, exec und exec resume sowie Features gelesen. JSONL/thread_id wird gespeichert.
  Resume erhaelt explizit read-only/never; Shell, Unified Exec, Hooks, Apps, Browser,
  Computer Use und Code Host sind deaktiviert, User-Config wird nicht importiert.
  Sprachtext geht nie durch Windows-Batchdateien: der npm-Launcher wird direkt ueber
  Node aufgerufen. Die eingeschraenkte Werkzeugauswahl kann Projektanalyse begrenzen.
  Vorhandene .codex/config.toml im Projekt oder seinen Vorfahren wird sicher abgelehnt,
  damit daraus keine ausfuehrbaren MCP-Server geladen werden. Fuer solche Checkouts
  Claude oder einen konfigurationsfreien Worktree waehlen. Codex/Ollama erhalten einen
  begrenzten Datenkontext aus AGENTS/README/HANDOFF und explizit genannten relativen
  Dateipfaden; grosse Dateien werden gekuerzt, nicht automatisch ausgefuehrt.
- **ollama**: lokale HTTP-Q&A auf 127.0.0.1:11434, model waehlt das Modell, sonst
  llama3.2. Die letzten zehn aktiven Gespraechsturns sind Kontext, keine Agentenwerkzeuge.
- **echo**: Fake-Anbieter ohne KI, fuer Mikrofon/Vorlesen und UI-Pruefungen.

Anbieter/Modell am PC einstellen, Server neu starten. Sitzung je Projekt und Kombination
Root/Anbieter/Modell; Wechsel beginnt eine neue Unterhaltung. Neues Gespraech verwirft
nur den aktiven Kontext, Berichte bleiben. Projekt wechseln website waehlt den Namen.
UI-Einstellungen: Sprache de-DE/EN-US, Stimme, Tempo, Tonhoehe; bevorzugt passende Stimme.

## Sprachbefehle und Sicherheit
Allowlist: Status, Was ist fehlgeschlagen, Lies den letzten Bericht, Neues Gespraech,
Projekt wechseln <name>, Stopp/Ruhe, Wiederholen, Notaus. Englische Entsprechungen auch.
Status liest Branch/Aenderungen, Laufzentrale und gespeicherte testing/runs-Daten, startet
keine Tests. running?-Eintraege sind Indizien, keine bestaetigten Prozesse.
Fehlerberichte durchsuchen die letzten 20 Runs. Gespeicherte Ergebnisse sind kein frisches
Gate. Andere Saetze gehen als Frage an den lesenden Agenten, niemals direkt an eine Shell.

**Aktion <Beschreibung>** erzeugt zuerst konkrete Dateivorschlaege. Die Bridge prueft
Pfade, Groessen und geschuetzte Linien und liest den Vorschlag mit einem zufaelligen
zusammengesetzten Wort vor. Nur genau dieses Wort in der **naechsten** Nachricht,
innerhalb 60 Sekunden, erlaubt diesen einen Vorschlag. Falsches Wort, anderes Projekt,
Stopp, Notaus oder Timeout verbrauchen die Freigabe. Danach wieder Lesemodus.
Vorhandene Dateien werden vorher gelesen, auf unveraenderten Inhalt geprueft und
unveraenderlich gesichert. Maximal acht UTF-8-Dateien zu je 64 KiB, keine leeren Inhalte,
keine Loeschung, keine Symlink-/Junction-Ziele, Git-Metadaten oder Bridge-Selbstbearbeitung.
Ein edits-Feld in einer normalen Antwort wird **nie** angewendet. Teilweise Schreibfehler
koennen trotz Vorpruefung vorkommen; alle schon geschriebenen Dateien haben Backups.

Runs starten, Merges und beliebige Shell-Befehle bleiben im MVP **gesperrt**, da sie
nicht in der vorgegebenen Befehls-Allowlist stehen. PC/Launch Hub bleiben dafuer zustaendig.
Push, Force und Datenloeschung sind nie verfuegbar. Sprachbestaetigung entsperrt keinen
allgemeinen Agenten-Schreib-/Shellmodus. Geschriebener Code wird nicht gestartet.
Repository-/Log-/Webtexte sind Daten, keine Befehle oder Freigaben. Status verwendet feste
argv-Befehle, nie shell=True.

Notaus / Emergency stop / Bruecke aus: eigene Agentenprozesse stoppen und Bridge anhalten.
POST /api/emergency bleibt waehrend Antworten erreichbar und ist vom Rate Limit ausgenommen;
Token/CSRF bleiben erforderlich. Nur bewusstes Reaktivieren in Einstellungen hebt Notaus auf.
Keine Besitzer-Clients/fremden Runs werden beendet. Ollama-Antworten werden verworfen;
dessen laufende HTTP-Berechnung kann bis zum Timeout weiterlaufen. Stopp beendet Aufnahme,
Vorlesen und eigenen CLI-Turn. Wiederholen/Skip sind immer sichtbar; Betriebssystem-
Lautstaerke und Bluetooth-Routing gelten weiterhin, auch beim Wiederholen.

API: Bearer-Token plus CSRF fuer POST, Host-/Origin-Pruefung, kein CORS, maximal 1,5 MB
Body, 6000 Transcript-Zeichen, 2 MB CLI-Ausgabe, 180 Sekunden CLI-Turn. Kein GET aendert
die Unterhaltung. Ein Besitzer/ein aktiver Turn je Server; Tabs teilen Projekt/Freigabe.

## Lokale Daten, optionale Adapter und Tests
Unter tools/voicebridge: token.txt, projects.json, audit.log, sessions.json, history.jsonl,
out/ und backups/, alles gitignored. Vollstaendige Antworten spaeter im Verlauf lesen.
Audit protokolliert Ereignisse, keine Tokens oder Bestaetigungswoerter. Private Transkripte/
Berichte liegen lokal; Zugriff auf PC/Tailnet begrenzen. Keine automatische Loeschung oder
Retention. PWA cached nur Shell, nie API, Token, Audio oder Berichte. Offline-Shell ist
sichtbar, KI braucht den PC und Browser-STT normalerweise Internet.

Optional und standardmaessig **aus**: --stt whisper braucht separat installiertes
faster-whisper; --whisper-model small waehlt das Modell (erster Download eventuell online).
--piper-model <lokale.onnx> braucht separat installiertes Piper und Modell.
Keine automatische Paketinstallation. Danach lokale STT/TTS in Einstellungen aktivieren.
Aufnahmen maximal 1,5 MB. Android Chrome ist am verlaesslichsten; iOS Safari, Stimmen,
Haptik, Autoplay, Bluetooth und Media Session variieren. Wake Lock ist best effort.
Nicht beim aktiven Fahren bedienen; lange Aufgaben als Passagier oder im Stand besprechen.

PowerShell-Trockenlauf: `$env:SB_VOICE_DRY_RUN='1'`, dann Server starten.
Kein Agent und keine Dateischreibaktion. Fuer echten Betrieb Variable entfernen:
`Remove-Item Env:SB_VOICE_DRY_RUN`. .claude/launch.json startet Vorschau auf 8772
bewusst im Trockenlauf. Echo startet ebenfalls keine echten Agenten.

Tests: `python -m unittest discover tools/voicebridge/tests`,
`node tools/voicebridge/tests/ui_smoke.cjs`, `node --check tools/voicebridge/static/app.js`.
Node-Harness prueft Zustaende mit Fake-STT/TTS/HTTP, **kein** gerendertes Layout.
Launch-Hub-Pythontests sind nicht in Gradle check verdrahtet; Gradle bleibt unveraendert.

## Research result 2026-09-30 (what a phone can do today)
- Claude phone app: has its own voice mode (hands-free and push-to-talk, spoken replies) for normal chats. It does NOT read aloud the replies of a Remote Control session: those are text only. Remote Control needs the home machine and the session to stay running.
- Claude Code: `/voice` is input only (no text-to-speech). Community add-ons exist for spoken replies and a full voice loop (for example `mbailey/voicemode` as MCP server with Whisper + Kokoro/Piper, `claude-speak` as hook with Kokoro, `tts-companion` with Piper for German); they are unofficial, check their status before relying on them. Kokoro is English-first; Piper has German voices.
- Siri/Google Assistant cannot route a spoken request to a Claude session today (only rumors about later iOS versions).
- Fast fallback with zero build: phone + Remote Control + the phone's system read-aloud (Android "Select to Speak", iOS "Speak Screen") + keyboard dictation.
- Chosen build: `tools/voicebridge/` (brief `docs/ai/briefs/voicebridge.md`): one big talk button, browser speech recognition and speech synthesis, German by default, the bridge continues a headless Claude session (`claude -p --resume`) so it feels like talking to the same agent, read-only by default, spoken confirmation for actions, reachable only over Tailscale.
