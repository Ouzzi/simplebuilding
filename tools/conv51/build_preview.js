// Builds the standalone CONV51 preview: node build_preview.js <out.html>
const fs = require('fs'), path = require('path');
const R = path.join(__dirname, '../..'), ex = (f) => fs.readFileSync(path.join(R, 'wiki/tests/conv51/examples', f));
const core = fs.readFileSync(path.join(R, 'wiki/assets/conv/conv-core.js'), 'utf8'), ui = fs.readFileSync(path.join(R, 'wiki/assets/conv/conv-ui.js'), 'utf8');
const samples = { blueprint: { name: 'hut.nbt', b64: ex('hut.nbt').toString('base64') }, model: { name: 'stool.bbmodel', b64: ex('stool.bbmodel').toString('base64') } };
const Q = [
  ['1', 'Formatpriorität Bauwerke (Phase 2 zuerst)', 'A = .nbt + Sponge .schem (v2/v3) + .litematic (empfohlen); B = alle Formate gleichzeitig; C = nur .nbt und .schem'],
  ['2', '3D-Bibliothek für die Vorschau', 'ja = three.js von erlaubtem CDN (echte 3D-Ansicht, +~600 KB, Drittanbieter-Anfrage); nein = eigene Canvas-Ansicht ohne Bibliothek (empfohlen: Wiki ist offline-fähig, keine Fremd-Hosts)'],
  ['3', 'Voxel-Näherung für .obj/.gltf/.glb', 'ja = Modelle werden in Würfel-Näherung (Quader, Grenze 256 Elemente, Hinweis „Näherung“) gewandelt; nein = nur .bbmodel und Vanilla-JSON'],
  ['4', 'Seitenplatz im Wiki', 'A = eigener Menüpunkt „Werkzeuge“ mit beiden Seiten; B = je eine Unterseite bei Simple Building Tools bzw. Simple Models (empfohlen); C = eigene Standalone-Seiten ohne Menü, nur verlinkt'],
  ['5', 'Rückweg Code → Datei (Pflicht oder später?)', 'ja = in Phase 2 Pflicht (.nbt und .bbmodel/JSON); später = erst nach Freigabe der Hinrichtung'],
  ['6', 'Große Bauwerke (> 32 000 Zeichen Code)', 'A = automatisch in Teile von unten nach oben schneiden, Teile einzeln einfügbar (empfohlen); B = Grenze nur anzeigen; C = Grenze für importierte Codes in der Mod anheben (Mod-Änderung)'],
  ['7', 'Bauwerke > 256 Blöcke je Achse', 'A = in 256er-Kacheln schneiden mit Platzier-Hinweis; B = ablehnen']
];
const qh = Q.map((q) => `<p><b>${q[0]}. ${q[1]}</b><br><small>${q[2]}</small><br><input data-q="${q[0]}" size="40" placeholder="Antwort"></p>`).join('');
const html = `<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>CONV51 – Vorschau Bauwerke / 3D-Modelle Konverter</title>
<style>body{font:15px/1.5 system-ui,sans-serif;max-width:900px;margin:1.5rem auto;padding:0 1rem}.cv-drop{border:2px dashed #888;border-radius:8px;padding:1.5rem;text-align:center;cursor:pointer}.cv-code{width:100%;font:12px/1.35 monospace}.cv-prev{display:flex;gap:1rem;align-items:center;flex-wrap:wrap}.cv-err{color:#b00020}canvas{background:#f4f4f4;border:1px solid #ccc}section{border:1px solid #ccc;border-radius:8px;padding:1rem;margin:1rem 0}#qbox{background:#fffbe6;border:1px solid #d9c26b;padding:1rem;border-radius:8px}</style></head><body>
<h1>CONV51 – Konverter-Seiten im Wiki (Phase 1: Prototyp)</h1>
<p>Zwei neue Wiki-Seiten, reines Browser-JS, <b>kein Upload</b>. Hier eingebettet und klickbar: „Beispiel laden“ nimmt eine selbst erzeugte Hütte (.nbt) bzw. einen Hocker (.bbmodel); eigene Dateien gehen per Ablegen/Klick. Der Blaupausen-Code ist die echte Bausprache der Mod (Serialisierer 1:1 nachgebaut, gegen einen Python-Nachbau getestet). Der Modell-Code ist das Vanilla-Modell-JSON im kompakten Layout des Simple-Models-Editors. In der echten Wiki bis zu deiner Freigabe nur im Dev-Wiki sichtbar.</p>
<p><b>Wichtig zum Format:</b> Ein „Blaupausen-Code“ ist <i>Text</i> (z. B. <code>stone 0..4,0,0..4</code>), kein Binärstring; Höchstlänge beim Einfügen 32 000 Zeichen – die Beispielhütte hat ~620, der Turm (tests/conv51/examples/tower.nbt) ~48 000 und wird deshalb in 2 Teile geschnitten.</p>
<section><h2>Seite „Bauwerke“</h2><div data-conv="blueprint"></div></section>
<section><h2>Seite „3D-Modelle“</h2><div data-conv="model"></div></section>
<div id="qbox"><h2>Fragen an den Besitzer (Antwortcode CONV51)</h2>${qh}
<p>Antwort (zum Kopieren): <input id="ans" size="70" readonly> <button id="cp" type="button">Kopieren</button></p><p><small>Format: <code>CONV51: 1=A, 2=nein, 3=ja, 4=B, 5=ja, 6=A, 7=A</code> – leere Felder = „wie empfohlen“.</small></p></div>
<script>window.CONV51_PREVIEW=true;window.CONV51_SAMPLES=${JSON.stringify(samples)};</script>
<script>${core}</script><script>${ui}</script>
<script>(function(){var a=document.getElementById('ans');function u(){a.value='CONV51: '+[].map.call(document.querySelectorAll('[data-q]'),function(i){return i.getAttribute('data-q')+'='+(i.value||'empf.')}).join(', ')}document.querySelectorAll('[data-q]').forEach(function(i){i.oninput=u});u();document.getElementById('cp').onclick=function(){a.select();document.execCommand('copy')}})();</script></body></html>`;
fs.writeFileSync(process.argv[2], html); console.log('wrote', process.argv[2], html.length);
