// Builds the standalone CONV51 phase-2 acceptance preview: node build_preview.js <out.html>
const fs = require('fs'), path = require('path');
const R = path.join(__dirname, '../..'), ex = (f) => fs.readFileSync(path.join(R, 'wiki/tests/conv51/examples', f));
const js = ['conv-data.js', 'conv-core.js', 'conv-formats.js', 'conv-lang.js', 'conv-mesh.js', 'conv-ui.js'].map((f) => fs.readFileSync(path.join(R, 'wiki/assets/conv', f), 'utf8'));
const samples = { blueprint: { name: 'hut.litematic', b64: ex('hut.litematic').toString('base64') }, model: { name: 'house.glb', b64: ex('house.glb').toString('base64') } };
const files = ['hut.nbt', 'hut.schem', 'hut_v2.schem', 'hut_v1.schem', 'hut.litematic', 'mini.schematic', 'tower.nbt', 'stool.json', 'stool.bbmodel', 'stool_tex.bbmodel', 'house.obj', 'house.mtl', 'house.gltf', 'house.glb'];
const dl = files.map((f) => `<a download="${f}" href="data:application/octet-stream;base64,${ex(f).toString('base64')}">${f}</a>`).join(' · ');
const esc = (s) => s.replace(/<\/script/gi, '<\\/script');
const html = `<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>CONV51B – Abnahme Konverter Bauwerke / 3D-Modelle (Phase 2)</title>
<style>body{font:15px/1.5 system-ui,sans-serif;max-width:920px;margin:1.5rem auto;padding:0 1rem}.cv-drop{border:2px dashed #888;border-radius:8px;padding:1.5rem;text-align:center;cursor:pointer}.cv-drop.cv-over{border-color:#2a6ad8;background:#eef4ff}.cv-code{width:100%;box-sizing:border-box;font:12px/1.35 monospace}.cv-prev{display:flex;gap:1rem;align-items:center;flex-wrap:wrap}.cv-err,.cv-bad{color:#b00020}.cv-ok{color:#1b6e2a}.cv-hint{color:#555}.cv-back{margin-top:1rem}.cv-back summary{cursor:pointer;font-weight:600}canvas{background:#f4f4f4;border:1px solid #ccc;max-width:100%}section{border:1px solid #ccc;border-radius:8px;padding:1rem;margin:1rem 0}#qbox{background:#fffbe6;border:1px solid #d9c26b;padding:1rem;border-radius:8px}table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:.2rem .5rem;text-align:left;vertical-align:top}</style></head><body>
<h1>CONV51B – Konverter-Seiten „Bauwerke“ und „3D-Modelle“ (Phase 2, Abnahme)</h1>
<p>Beide Seiten sind fertig gebaut und hier klickbar eingebettet. Alles läuft im Browser, <b>nichts wird hochgeladen</b>, keine Fremd-Hosts. Im echten Wiki bleiben sie hinter dem Dev-Schalter (nur <code>/dev/</code>, Dev-Overlay, <code>?dev=1</code>), Seiten mit <code>noindex</code>; der Link erscheint nur im Dev-Wiki auf der Übersicht von Simple Building Tools bzw. Simple Models.</p>
<h2>Deine Entscheidungen (CONV51 = „nach Empfehlung“)</h2>
<table><tr><th>#</th><th>Frage</th><th>umgesetzt</th></tr>
<tr><td>1</td><td>Formate Bauwerke</td><td>.nbt (auch Create), Sponge .schem v1–v3, .litematic (alle Regionen oder eine), dazu MCEdit .schematic (alte Zahlen-Ids, Näherung wird gezählt). Axiom .bp: wird erkannt und erklärt („in Axiom als .schem exportieren“).</td></tr>
<tr><td>2</td><td>3D-Bibliothek</td><td>nein – eigene Canvas-Ansicht, kein CDN</td></tr>
<tr><td>3</td><td>.obj/.gltf/.glb</td><td>ja – Würfel-Näherung (Feinheit, Größe 1–3 Blöcke, innen füllen), ≤ 256 Quader, Materialfarben als Texturen</td></tr>
<tr><td>4</td><td>Seitenplatz</td><td>B – Hinweis-Kasten auf der Übersicht der jeweiligen Mod (nur Dev)</td></tr>
<tr><td>5</td><td>Rückweg</td><td>ja – Code → .nbt / .schem v3; Modell-Code → .json / .bbmodel / Ressourcenpaket (.zip mit Modell + PNGs)</td></tr>
<tr><td>6</td><td>&gt; 32 000 Zeichen</td><td>A – Teile von unten nach oben, je Teil Versatz</td></tr>
<tr><td>7</td><td>&gt; 256 je Achse</td><td>A – 256er-Kacheln, darin Teile, je Code Versatz zur Ecke des Bauwerks</td></tr></table>
<p><b>Zum Ausprobieren</b> (selbst erzeugte Beispieldateien, herunterladen und in die Felder ziehen): ${dl}</p>
<section><h2>Seite „Bauwerke“</h2><div data-conv="blueprint"></div></section>
<section><h2>Seite „3D-Modelle“</h2><div data-conv="model"></div></section>
<h2>Was geprüft ist und was nicht</h2>
<ul><li>Geprüft (node, automatisch): alle Leser gegen eine unabhängige Python-Referenz, Zufallsdateien je Format, Umbenennungen alter Ids, unbekannte Blöcke, Kacheln/Teile, Bausprache-Parser (Formen, Variablen, Fehler wie im Spiel), Rückweg Datei → Code → Datei, Netze → Würfel, .bbmodel-Rundlauf, PNG/ZIP, Seiten-Rauchtest mit DOM-Attrappe.</li>
<li><b>Nicht geprüft:</b> Klicktest im echten Browser auf der Dev-Wiki (im Container gibt es keinen Browser); Dateien echter Fremd-Werkzeuge (Litematica, WorldEdit, MCEdit, Blockbench, Blender) – nur selbst erzeugte; Laden der erzeugten .nbt/.schem/.zip im Spiel; Standardwerte aus der Spiel-Registry (Handtabelle, unbekannte Standardwerte werden mitgeschrieben und bleiben gültig).</li></ul>
<div id="qbox"><h2>Abnahme (Antwortcode CONV51B)</h2>
<p><label><input type="radio" name="v" value="ok" checked> ok</label> &nbsp; <label><input type="radio" name="v" value="Einwand"> Einwand:</label> <input id="why" size="60" placeholder="was soll anders sein?"></p>
<p>Antwort (zum Kopieren): <input id="ans" size="70" readonly> <button id="cp" type="button">Kopieren</button></p></div>
<script>window.CONV51_PREVIEW=true;window.CONV51_SAMPLES=${JSON.stringify(samples)};</script>
${js.map((s) => '<script>' + esc(s) + '</script>').join('\n')}
<script>(function(){var a=document.getElementById('ans'),w=document.getElementById('why');function u(){var v=document.querySelector('input[name=v]:checked').value;a.value='CONV51B: '+(v==='ok'?'ok':'Einwand: '+(w.value||'…'))}document.querySelectorAll('input[name=v]').forEach(function(i){i.onchange=u});w.oninput=function(){document.querySelector('input[value=Einwand]').checked=true;u()};u();document.getElementById('cp').onclick=function(){a.select();(navigator.clipboard?navigator.clipboard.writeText(a.value):Promise.reject()).catch(function(){document.execCommand('copy')})}})();</script></body></html>`;
fs.writeFileSync(process.argv[2], html); console.log('wrote', process.argv[2], html.length);
