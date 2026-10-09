# Zentrale (Status-Dashboard)

Statische Seite, die `status.json` aus demselben Verzeichnis lädt (alle 30 s). Ausgeliefert vom Webserver
des Projekts direkt aus diesem Ordner des Arbeitsstands; `status.json` und `logs/<ref>.log` werden dort
getrennt erzeugt und nicht eingecheckt. Reiter: Status, Tests, Roadmap, Laptop-Start, Links.
Die Knöpfe unter „Laptop-Start“ nutzen das Protokoll `simplemods://` des Laptop-Starters.
