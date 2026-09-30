'use strict';
const $ = id => document.getElementById(id);
const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let token = localStorage.getItem('voicebridge-token') || '';
const incoming = new URLSearchParams(location.hash.slice(1)).get('token');
if (incoming) { token = incoming; localStorage.setItem('voicebridge-token', token); history.replaceState(null, '', location.pathname); }
let state = 'idle', recognizer, recorder, micStream, chunks = [], wake, audioContext, mediaAudio;
let finalText = '', interim = '', finishing = false, lastAnswer = '', speechGeneration = 0, requestGeneration = 0;
let activeAudio, apiState = {};
const labels = {idle:'Bereit', listening:'Ich höre zu', thinking:'Ich arbeite', speaking:'Antwort wird vorgelesen'};
for (const id of ['language','rate','pitch','voice','serverSTT','serverTTS']) {
  const saved = localStorage.getItem('voicebridge-' + id);
  if (saved !== null) { if ($(id).type === 'checkbox') $(id).checked = saved === 'true'; else $(id).value = saved; }
  $(id).addEventListener('change', () => localStorage.setItem('voicebridge-' + id, $(id).type === 'checkbox' ? $(id).checked : $(id).value));
}
async function api(path, data, rawType) {
  const headers = {Authorization:'Bearer ' + token};
  const options = {headers};
  if (data !== undefined) {
    Object.assign(headers, {'Content-Type':rawType || 'application/json', 'X-CSRF-Token':token});
    options.method = 'POST'; options.body = rawType ? data : JSON.stringify(data);
  }
  const response = await fetch('/api/' + path, options);
  if (!response.ok) { let error; try { error = (await response.json()).error; } catch { error = 'Server nicht erreichbar'; } throw new Error(error); }
  return path === 'tts' ? response.blob() : response.json();
}
function cue() {
  navigator.vibrate?.(25);
  try {
    audioContext ||= new (window.AudioContext || window.webkitAudioContext)();
    audioContext.resume();
    const oscillator = audioContext.createOscillator(), gain = audioContext.createGain();
    oscillator.frequency.value = state === 'listening' ? 700 : 450;
    gain.gain.setValueAtTime(.06, audioContext.currentTime); gain.gain.exponentialRampToValueAtTime(.001, audioContext.currentTime + .07);
    oscillator.connect(gain).connect(audioContext.destination); oscillator.start(); oscillator.stop(audioContext.currentTime + .08);
  } catch { /* Device may block Web Audio. */ }
}
async function setState(next) {
  const changed = next !== state;
  state = next; document.body.dataset.state = next;
  $('state').textContent = labels[next]; $('talk').className = 'talk ' + next;
  $('talk').setAttribute('aria-pressed', String(next === 'listening'));
  $('talk').setAttribute('aria-label', next === 'listening' ? 'Aufnahme beenden und senden' : 'Sprechen starten');
  $('talkLabel').textContent = {idle:'Tippen & sprechen', listening:'Fertig? Tippen.', thinking:'Stopp? Tippen.', speaking:'Antworten? Tippen.'}[next];
  $('talkIcon').textContent = {idle:'●', listening:'■', thinking:'•••', speaking:'◖'}[next];
  if (changed) cue();
  if (next !== 'idle' && !wake && navigator.wakeLock && document.visibilityState === 'visible') {
    try { wake = await navigator.wakeLock.request('screen'); wake.addEventListener('release', () => { wake = null; }); } catch { /* Optional. */ }
  }
  if (next === 'idle' && wake) { wake.release(); wake = null; }
  if ('mediaSession' in navigator) navigator.mediaSession.playbackState = next === 'speaking' || next === 'listening' ? 'playing' : 'paused';
}
function voices() {
  if (!window.speechSynthesis) return;
  const preferred = localStorage.getItem('voicebridge-voice') || $('voice').value;
  $('voice').replaceChildren(new Option('Automatisch', ''));
  for (const voice of speechSynthesis.getVoices()) $('voice').add(new Option(voice.name + ' · ' + voice.lang, voice.voiceURI));
  $('voice').value = preferred;
}
if (window.speechSynthesis) { voices(); speechSynthesis.addEventListener('voiceschanged', voices); }
function cancelSpeech() {
  speechGeneration++; window.speechSynthesis?.cancel();
  if (activeAudio) { activeAudio.pause(); activeAudio = null; }
}
async function speak(text) {
  cancelSpeech();
  if (!text) { setState('idle'); return; }
  lastAnswer = text; localStorage.setItem('voicebridge-last', text); $('message').textContent = text;
  const generation = speechGeneration;
  await setState('speaking');
  if ($('serverTTS').checked && apiState.tts) {
    try {
      const blob = await api('tts', {text});
      if (generation !== speechGeneration) return;
      const url = URL.createObjectURL(blob); activeAudio = new Audio(url);
      const release = () => { URL.revokeObjectURL(url); if (generation === speechGeneration) setState('idle'); };
      activeAudio.onended = release; activeAudio.onerror = release;
      await activeAudio.play(); return;
    } catch (error) { if (generation !== speechGeneration) return; $('message').textContent = error.message + ' Browser-Stimme wird versucht.'; }
  }
  if (!window.speechSynthesis) { $('message').textContent = 'Vorlesen wird hier nicht unterstützt. ' + text; setState('idle'); return; }
  const pieces = text.match(/[^.!?]+[.!?]*|[.!?]+/g) || [text];
  const all = speechSynthesis.getVoices();
  const chosen = all.find(v => v.voiceURI === $('voice').value) || all.find(v => v.lang.toLowerCase().startsWith($('language').value.slice(0,2))) || all[0];
  const next = () => {
    if (generation !== speechGeneration) return;
    if (!pieces.length) { setState('idle'); return; }
    const utterance = new SpeechSynthesisUtterance(pieces.shift());
    utterance.lang = $('language').value; utterance.voice = chosen; utterance.rate = Number($('rate').value); utterance.pitch = Number($('pitch').value); utterance.volume = 1;
    utterance.onend = next;
    utterance.onerror = event => { if (generation === speechGeneration && event.error !== 'interrupted' && event.error !== 'canceled') { $('message').textContent = 'Vorlesen blockiert. Letzte Antwort antippen, um erneut vorzulesen.'; setState('idle'); } };
    speechSynthesis.speak(utterance);
  };
  next();
}
async function send(text) {
  if (!text.trim()) { setState('idle'); await speak('Ich habe nichts verstanden. Bitte nochmal sprechen.'); return; }
  const generation = ++requestGeneration;
  await setState('thinking'); $('message').textContent = 'Deine Nachricht wird verarbeitet …';
  try {
    const answer = await api('turn', {text});
    if (generation !== requestGeneration) return;
    if (answer.project) $('project').textContent = answer.project + ' · Lesemodus';
    if (answer.quiet) { cancelSpeech(); setState('idle'); } else await speak(answer.spoken);
  } catch (error) { if (generation === requestGeneration) await speak(error.message); }
}
function releaseMic() { micStream?.getTracks().forEach(t => t.stop()); micStream = null; }
async function startListening() {
  cancelSpeech(); finalText = ''; interim = ''; finishing = false;
  if ($('serverSTT').checked && apiState.stt) {
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) { await speak('Audioaufnahme wird hier nicht unterstützt. HTTPS und einen aktuellen Browser verwenden.'); return; }
    try {
      micStream = await navigator.mediaDevices.getUserMedia({audio:true});
      chunks = []; recorder = new MediaRecorder(micStream);
      recorder.ondataavailable = event => { if (event.data.size) chunks.push(event.data); };
      recorder.onstop = async () => {
        releaseMic(); if (!finishing) return;
        const generation = requestGeneration;
        await setState('thinking');
        const blob = new Blob(chunks, {type:recorder.mimeType});
        if (blob.size > 1500000) { await speak('Aufnahme zu lang. Bitte kürzer sprechen.'); return; }
        try { const result = await api('stt', blob, blob.type); if (generation === requestGeneration) await send(result.transcript); }
        catch (error) { if (generation === requestGeneration) await speak(error.message); }
      };
      await setState('listening'); recorder.start(); return;
    } catch { releaseMic(); await speak('Mikrofonzugriff fehlgeschlagen. Mikrofon erlauben und HTTPS verwenden.'); return; }
  }
  if (!Recognition) { await speak('Dieser Browser unterstützt keine Spracherkennung. Android Chrome verwenden oder lokale Server-STT am PC aktivieren.'); return; }
  if (!window.isSecureContext) { await speak('Die Handyverbindung braucht HTTPS. Bitte die Tailscale HTTPS-Adresse öffnen.'); return; }
  recognizer = new Recognition(); recognizer.lang = $('language').value;
  recognizer.continuous = true; recognizer.interimResults = true;
  recognizer.onresult = event => {
    interim = '';
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i].isFinal) finalText += event.results[i][0].transcript + ' ';
      else interim += event.results[i][0].transcript + ' ';
    }
  };
  recognizer.onerror = event => {
    if (event.error === 'no-speech' || event.error === 'aborted') return;
    finishing = false; recognizer.onend = null; recognizer.abort(); setState('idle');
    speak('Spracherkennung fehlgeschlagen. Mikrofon und Internet prüfen. Fehler: ' + event.error);
  };
  recognizer.onend = () => {
    if (finishing) { finishing = false; send(finalText + interim); }
    else if (state === 'listening') { try { recognizer.start(); } catch { setState('idle'); } }
  };
  await setState('listening');
  try { recognizer.start(); } catch { await speak('Mikrofon konnte nicht gestartet werden. Bitte nochmal tippen.'); }
}
function finishListening() {
  finishing = true;
  if (recorder?.state === 'recording') recorder.stop(); else recognizer?.stop();
}
async function stop(emergency = false, quiet = true) {
  requestGeneration++; cancelSpeech(); finishing = false;
  if (recognizer) { recognizer.onend = null; recognizer.abort(); }
  if (recorder?.state === 'recording') recorder.stop(); releaseMic();
  await setState('idle');
  try {
    if (emergency) { const result = await api('emergency', {}); await speak(result.spoken); }
    else if (quiet) await api('turn', {text:'stop'});
  } catch (error) { $('message').textContent = error.message; }
}
async function toggleTalk() {
  if (state === 'listening') { finishListening(); return; }
  if (state === 'thinking') { await stop(); return; }
  if (state === 'speaking') await stop(false, false);
  // Arm browser media keys with a local silent WAV generated in memory.
  if ('mediaSession' in navigator && !mediaAudio) {
    const buffer = new ArrayBuffer(8044), view = new DataView(buffer);
    const str = (offset, text) => [...text].forEach((c,i) => view.setUint8(offset+i,c.charCodeAt(0)));
    str(0,'RIFF'); view.setUint32(4,8036,true); str(8,'WAVE'); str(12,'fmt '); view.setUint32(16,16,true);
    view.setUint16(20,1,true); view.setUint16(22,1,true); view.setUint32(24,8000,true); view.setUint32(28,8000,true);
    view.setUint16(32,1,true); view.setUint16(34,8,true); str(36,'data'); view.setUint32(40,8000,true);
    new Uint8Array(buffer,44).fill(128); mediaAudio = new Audio(URL.createObjectURL(new Blob([buffer],{type:'audio/wav'}))); mediaAudio.loop = true; mediaAudio.volume = .001;
    mediaAudio.play().catch(() => {});
  }
  await startListening();
}
$('talk').addEventListener('click', toggleTalk);
document.addEventListener('keydown', event => {
  if (event.code === 'Space' && !event.repeat && !['INPUT','SELECT','TEXTAREA','BUTTON'].includes(event.target.tagName)) { event.preventDefault(); toggleTalk(); }
});
$('stop').onclick = () => stop(); $('emergency').onclick = () => stop(true);
$('repeat').onclick = async () => { await stop(false, state === 'thinking'); await speak(lastAnswer || localStorage.getItem('voicebridge-last') || 'Es gibt noch keine Antwort.'); };
$('settingsToggle').onclick = () => { $('settings').hidden = !$('settings').hidden; };
$('new').onclick = () => send('neues gespräch');
$('projects').onchange = () => send('projekt wechseln ' + $('projects').value);
$('reactivate').onclick = async () => { try { await speak((await api('reactivate', {})).spoken); } catch(error) { await speak(error.message); } };
$('historyToggle').onclick = async () => {
  $('history').hidden = !$('history').hidden; if ($('history').hidden) return;
  try {
    const result = await api('history'); $('reports').replaceChildren();
    for (const record of result.history.reverse()) {
      const entry = document.createElement('details'), summary = document.createElement('summary'), body = document.createElement('pre');
      summary.textContent = new Date(record.time*1000).toLocaleString() + ' · ' + record.spoken;
      body.textContent = record.transcript + '\n\n' + record.full; entry.append(summary, body); $('reports').append(entry);
    }
  } catch(error) { $('message').textContent = error.message; }
};
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible' && state !== 'idle') setState(state); });
if ('mediaSession' in navigator) {
  navigator.mediaSession.metadata = new MediaMetadata({title:'Voicebridge', artist:'Tippen oder Headset-Taste zum Sprechen'});
  for (const action of ['play','pause']) { try { navigator.mediaSession.setActionHandler(action, toggleTalk); } catch {} }
  for (const action of ['stop','nexttrack']) { try { navigator.mediaSession.setActionHandler(action, () => stop()); } catch {} }
}
if ('serviceWorker' in navigator && window.isSecureContext) navigator.serviceWorker.register('/sw.js').catch(() => {});
api('state').then(result => {
  apiState = result; $('project').textContent = result.project + ' · Lesemodus';
  for (const name of result.projects) $('projects').add(new Option(name, name)); $('projects').value = result.project;
  $('serverSTT').disabled = !result.stt; $('serverTTS').disabled = !result.tts;
  if (result.dry_run) $('message').textContent = 'Trockenlauf aktiv. Kein Agent startet; keine Dateien werden geändert.';
  else if (result.stopped) $('message').textContent = 'Notaus aktiv. In den Einstellungen bewusst wieder aktivieren.';
}).catch(error => { $('message').textContent = error.message; });
