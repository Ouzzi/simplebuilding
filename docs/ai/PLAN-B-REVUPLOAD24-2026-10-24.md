# Plan b-revupload24 (REV24-M-7, Rest: Worker-Thread)

Ist: `MusicServer.finish` lief auf dem Server-Thread: `OggCheck.check` (CRC je Seite), `OggCheck.strip` (CRC doppelt, Kopien), `TrackStore.add` (SHA-256, Datei schreiben).
Messung (Java-Mikrobenchmark, CRC-Schleife wie `OggCheck.crc`): 4 MiB (Standard-Limit) ca. 17 ms CRC je Durchlauf, check+strip also etwa 50 ms = ein Tick; 16 MiB (Config-Maximum) ca. 62 ms je Durchlauf, ca. 190 ms gesamt; SHA-256 4 ms bzw. 15 ms. Messbar teuer, Umbau lohnt.

Umbau (nur `modules/simplemusic`):
- `MusicServer`: ein Daemon-Thread (`ThreadPoolExecutor(1,1)`, Queue 8, bei Voll "busy"), Worker macht NUR den reinen Teil (check + strip). Ergebnis geht in eine `ConcurrentLinkedQueue`, `tick` wendet sie auf dem Server-Thread an (Item/Spieler/`TrackStore` sind nicht threadsicher; `add` bleibt dort, ein Schreiben von <= 16 MiB, Hash 15 ms).
- Pro Spieler `PROCESSING`: kein zweiter Upload (Kontingent) bis das Ergebnis angewendet ist.
- `stopped()`: `shutdownNow`, Ergebnisse verwerfen.
- Test: `upload_worker_delivers` (+ Fabric-Adapter), Hilfsfunktion `flushUploads()` fuer die bestehenden Upload-Tests.
Nicht angefasst: MapsNetwork, TrimLight, Mixins.
