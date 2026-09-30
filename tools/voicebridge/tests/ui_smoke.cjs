// A deterministic DOM/API harness, not a rendered browser or real microphone test.
const fs = require('node:fs'), vm = require('node:vm'), assert = require('node:assert/strict');
const elements = new Map(), storage = new Map([['voicebridge-token','fake-test-token']]);
const element = id => {
  if (!elements.has(id)) elements.set(id, {id, value: ['rate','pitch'].includes(id)?'1':id==='language'?'de-DE':'',
    checked:false, hidden:true, type:id.startsWith('server')?'checkbox':'', textContent:'', tagName:'DIV',
    className:'', handlers:{}, addEventListener(event,fn){this.handlers[event]=fn;},
    setAttribute(){}, replaceChildren(){}, add(){}, append(){}});
  return elements.get(id);
};
let recognition, utterances = [], requests = [];
class Recognition {
  constructor(){ recognition = this; }
  start(){ this.started = true; } stop(){ this.onend?.(); } abort(){this.started=false;}
}
const document = {body:{dataset:{}},visibilityState:'visible',getElementById:element,
  addEventListener(){},createElement:()=>({append(){}})};
const context = {document,location:{hash:'',pathname:'/'},history:{replaceState(){}},
  localStorage:{getItem:key=>storage.get(key)||null,setItem:(k,v)=>storage.set(k,String(v))},
  window:{SpeechRecognition:Recognition,isSecureContext:true},navigator:{vibrate(){}},
  speechSynthesis:{getVoices:()=>[],addEventListener(){},cancel(){},speak:u=>utterances.push(u)},
  SpeechSynthesisUtterance:class {constructor(text){this.text=text;}},
  URL,URLSearchParams,Option:class{},console,Blob,
  fetch:async(url,options)=>{
    requests.push({url,options});
    const result = url.endsWith('state')?{project:'demo',projects:['demo'],stt:false,tts:false,dry_run:true}:
      {spoken:'Fertig. Testantwort.',project:'demo'};
    return {ok:true,json:async()=>result};
  }};
context.window.speechSynthesis = context.speechSynthesis;
vm.createContext(context); vm.runInContext(fs.readFileSync('tools/voicebridge/static/app.js','utf8'),context);
const settle = () => new Promise(resolve=>setImmediate(resolve));
(async()=>{
  await settle(); assert.equal(element('project').textContent,'demo · Lesemodus');
  await element('talk').handlers.click(); assert.equal(document.body.dataset.state,'listening');
  assert.equal(utterances.length,0,'silence while owner speaks');
  recognition.onresult({resultIndex:0,results:[Object.assign([{transcript:'Hallo'}],{isFinal:true})]});
  await element('talk').handlers.click(); await settle();
  assert.equal(document.body.dataset.state,'speaking');
  const turn = requests.find(r=>r.url.endsWith('turn'));
  assert.equal(JSON.parse(turn.options.body).text.trim(),'Hallo');
  assert.equal(turn.options.headers['X-CSRF-Token'],'fake-test-token');
  assert.equal(utterances[0].text.trim(),'Fertig.'); utterances[0].onend();
  assert.equal(utterances[1].text.trim(),'Testantwort.'); utterances[1].onend();
  await settle(); assert.equal(document.body.dataset.state,'idle');
  await element('repeat').onclick(); assert.equal(document.body.dataset.state,'speaking');
  await element('stop').onclick(); assert.equal(document.body.dataset.state,'idle');
  const stale = utterances[utterances.length-1]; stale.onend();
  assert.equal(document.body.dataset.state,'idle','cancelled speech cannot restart');
  console.log('UI harness: tap start/stop, silence, authenticated turn, sentence speech, repeat, cancellation OK');
})().catch(error=>{console.error(error);process.exitCode=1;});
