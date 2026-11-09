/* W12 (IG31-54): Download der Auswahl auf paket.html. Eingebettet von concepts.py in die Seite (statisch, ohne CDN).
   Index: window.SM_INDEX (Vorschau) oder downloads/index.json (tools/zentrale-web/public_downloads.py, nur Release-Jars).
   ZIP: eigene kleine ZIP-Funktion (Methode "stored", CRC-32), jede Datei vor dem Einpacken per SHA-256 geprueft. */
var DL={idx:window.SM_INDEX||null,err:false,lo:null,busy:false,msg:"",base:"downloads/"};
var DT={de:{h:"Herunterladen",rel:"Release zum Testen",mc:"Minecraft",load:"Download-Liste wird geladen …",fail:"Die Download-Liste ist gerade nicht erreichbar. Bitte später erneut versuchen.",lo:"Modloader",np:"noch nicht veröffentlicht",zip:"Als ZIP laden",sums:"Prüfsummen laden",file:"Datei",size:"Größe",one:"einzeln",none:"Wähle oben Mods aus, dann erscheinen hier die Dateien.",inside:"Schon enthalten in",miss:"Noch nicht im Release (kein Download)",ext:"Zusätzlich nötig (nicht enthalten, bitte selbst laden)",opt:"Optional unterstützt",put:"ZIP entpacken und alle .jar-Dateien in den Ordner mods deines Minecraft-Profils legen (Windows: %APPDATA%\\.minecraft\\mods).",ld:"Lade",chk:"prüfe",ok:"Fertig: alle Prüfsummen stimmen, ZIP wird gespeichert.",nochk:"Fertig (Browser kann hier keine Prüfsummen rechnen).",bad:"Prüfsumme stimmt nicht, Abbruch",er:"Fehler beim Laden von",files:"Dateien",total:"zusammen",loader:"Loader"},
en:{h:"Download",rel:"Release for testing",mc:"Minecraft",load:"Loading the download list …",fail:"The download list is not reachable right now. Please try again later.",lo:"Mod loader",np:"not released yet",zip:"Download as ZIP",sums:"Download checksums",file:"File",size:"Size",one:"single",none:"Pick mods above and their files show up here.",inside:"Already inside",miss:"Not in the release yet (no download)",ext:"Also required (not included, please get them yourself)",opt:"Optional support",put:"Unpack the ZIP and put all .jar files into the mods folder of your Minecraft profile (Windows: %APPDATA%\\.minecraft\\mods).",ld:"Loading",chk:"checking",ok:"Done: all checksums match, saving the ZIP.",nochk:"Done (this browser cannot compute checksums here).",bad:"checksum mismatch, aborted",er:"Error loading",files:"files",total:"total",loader:"Loader"}};
function dlL(){return document.documentElement.lang=="en"?"en":"de"}
function dlEsc(s){return String(s).replace(/[&<>"]/g,function(c){return{"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]})}
function dlFmt(n){return n>=1048576?(n/1048576).toFixed(1)+" MB":Math.max(1,Math.round(n/1024))+" KB"}
function dlUrl(m){return /^(data:|https?:|\/)/.test(m.url)?m.url:DL.base+m.url}
/* Auswahl W (id -> warum) -> Dateien fuer den Loader: was in einem gewaehlten Jar steckt (jar-in-jar), faellt weg */
function dlPlan(W){var by={},ids=Object.keys(W),cont={},ext={},opt={};
(DL.idx.mods||[]).forEach(function(m){if(m.loader==DL.lo)by[m.module]=m});
var cand=ids.filter(function(i){return by[i]});
cand.forEach(function(i){(by[i].contains||[]).forEach(function(c){if(c!=i&&!(c in cont))cont[c]=i})});
var files=cand.filter(function(i){return !(i in cont)}).map(function(i){return by[i]});
files.sort(function(a,b){return a.file<b.file?-1:1});
cand.forEach(function(i){(by[i].external||[]).forEach(function(x){ext[x]=1});(by[i].externalOptional||[]).forEach(function(x){opt[x]=1})});
Object.keys(ext).forEach(function(x){delete opt[x]});
return {files:files,inside:Object.keys(cont).filter(function(i){return i in W}).map(function(i){return [i,cont[i]]}),
miss:ids.filter(function(i){return !by[i]&&!(i in cont)}),ext:Object.keys(ext).sort(),opt:Object.keys(opt).sort(),by:by}}
function dlExt(x){var e=(DL.idx.externalMods||{})[x]||{name:x,url:""};return e.url?'<a href="'+dlEsc(e.url)+'" target="_blank" rel="noopener">'+dlEsc(e.name)+'</a>':dlEsc(e.name)}
function dlRender(W,M){var el=document.getElementById("dl");if(!el)return;var t=DT[dlL()],h='<h2 style="margin-top:0">'+t.h+'</h2>';
if(!DL.idx){el.innerHTML=h+'<p>'+(DL.err?t.fail:t.load)+'</p>';return}
var I=DL.idx,los=I.loaders||[];if(!DL.lo||los.indexOf(DL.lo)<0)DL.lo=los[0]||null;
h+='<p class="rel">'+t.rel+': <b>'+dlEsc((I.release||{}).version||"")+'</b> · '+t.mc+' '+dlEsc(I.minecraft||"")+' · '+dlEsc(((I.release||{}).date||"").slice(0,10))+' · <code>'+dlEsc((I.release||{}).commit||"")+'</code></p>';
h+='<div class="los" role="group" aria-label="'+t.lo+'"><span>'+t.lo+':</span>'+["fabric","neoforge","forge"].map(function(lo){var on=los.indexOf(lo)>=0,nm={fabric:"Fabric",neoforge:"NeoForge",forge:"Forge"}[lo];
return '<button type="button" class="lob" data-lo="'+lo+'" aria-pressed="'+(lo==DL.lo)+'"'+(on?'':' disabled title="'+t.np+'"')+'>'+nm+(on?'':' <small>('+t.np+')</small>')+'</button>'}).join("")+'</div>';
if(!Object.keys(W).length||!DL.lo){el.innerHTML=h+'<p>'+t.none+'</p>';return}
var P=dlPlan(W),tot=P.files.reduce(function(a,m){return a+m.size},0);DL.plan=P;
var ll=(I.loaderLinks||{})[DL.lo];
h+='<ol class="st2"><li>'+(ll?'<a href="'+dlEsc(ll.url)+'" target="_blank" rel="noopener">'+dlEsc(ll.name)+'</a> '+t.mc+' '+dlEsc(I.minecraft):"")+'</li>'
+'<li>'+t.put+'</li>'+(P.ext.length?'<li>'+t.ext+': '+P.ext.map(dlExt).join(", ")+'</li>':'')+'</ol>';
h+='<p><button type="button" class="btn" data-zip="1"'+(DL.busy||!P.files.length?' disabled':'')+'>'+t.zip+' ('+P.files.length+' '+t.files+', '+dlFmt(tot)+')</button> '
+'<button type="button" class="btn s" data-sums="1"'+(P.files.length?'':' disabled')+'>'+t.sums+'</button></p><p class="dmsg" aria-live="polite">'+dlEsc(DL.msg)+'</p>';
h+='<ul class="fl">'+P.files.map(function(m){return '<li><span class="fn">'+dlEsc(m.name)+' <small>'+dlEsc(m.version)+'</small></span><span class="fz">'+dlFmt(m.size)+'</span>'
+'<a href="'+dlEsc(dlUrl(m))+'" download="'+dlEsc(m.file)+'">'+t.one+'</a><code class="sh" title="SHA-256">'+dlEsc(m.sha256)+'</code></li>'}).join("")+'</ul>';
var grp={};P.inside.forEach(function(p){(grp[p[1]]=grp[p[1]]||[]).push(M[p[0]]?M[p[0]].n:p[0])});
Object.keys(grp).forEach(function(hst){h+='<p class="dnote">'+t.inside+' '+dlEsc(P.by[hst].name)+': '+dlEsc(grp[hst].sort().join(", "))+'</p>'});
if(P.miss.length)h+='<p class="dnote">'+t.miss+': '+P.miss.map(function(i){return dlEsc(M[i]?M[i].n:i)}).join(", ")+'</p>';
if(P.opt.length)h+='<p class="dnote">'+t.opt+': '+P.opt.map(dlExt).join(", ")+'</p>';
el.innerHTML=h}
var DLCRC=(function(){var t=new Uint32Array(256);for(var n=0;n<256;n++){var c=n;for(var k=0;k<8;k++)c=c&1?0xEDB88320^(c>>>1):c>>>1;t[n]=c>>>0}return t})();
function dlCrc(u){var c=0xFFFFFFFF;for(var i=0;i<u.length;i++)c=DLCRC[(c^u[i])&255]^(c>>>8);return (c^0xFFFFFFFF)>>>0}
/* ZIP (stored): [{name, data:Uint8Array}] -> Blob */
function dlZip(es){var enc=new TextEncoder(),parts=[],cen=[],off=0,cs=0;
es.forEach(function(e){var nm=enc.encode(e.name),crc=dlCrc(e.data),sz=e.data.length,h=new DataView(new ArrayBuffer(30)),c=new DataView(new ArrayBuffer(46));
h.setUint32(0,0x04034b50,true);h.setUint16(4,20,true);h.setUint16(6,0x0800,true);h.setUint16(12,0x21,true);h.setUint32(14,crc,true);h.setUint32(18,sz,true);h.setUint32(22,sz,true);h.setUint16(26,nm.length,true);
c.setUint32(0,0x02014b50,true);c.setUint16(4,20,true);c.setUint16(6,20,true);c.setUint16(8,0x0800,true);c.setUint16(14,0x21,true);c.setUint32(16,crc,true);c.setUint32(20,sz,true);c.setUint32(24,sz,true);c.setUint16(28,nm.length,true);c.setUint32(42,off,true);
parts.push(h.buffer,nm,e.data);cen.push(c.buffer,nm);off+=30+nm.length+sz;cs+=46+nm.length});
var z=new DataView(new ArrayBuffer(22));z.setUint32(0,0x06054b50,true);z.setUint16(8,es.length,true);z.setUint16(10,es.length,true);z.setUint32(12,cs,true);z.setUint32(16,off,true);
return new Blob(parts.concat(cen,[z.buffer]),{type:"application/zip"})}
function dlData(u){var b=atob(u.slice(u.indexOf(",")+1)),a=new Uint8Array(b.length);for(var i=0;i<b.length;i++)a[i]=b.charCodeAt(i);return a.buffer}
function dlHex(b){return Array.prototype.map.call(new Uint8Array(b),function(x){return ("0"+x.toString(16)).slice(-2)}).join("")}
function dlSave(blob,name){var a=document.createElement("a");a.href=URL.createObjectURL(blob);a.download=name;document.body.appendChild(a);a.click();
setTimeout(function(){URL.revokeObjectURL(a.href);a.remove()},60000)}
function dlSums(P){return P.files.map(function(m){return m.sha256+"  "+m.file}).join("\n")+"\n"}
function dlReadme(P){var I=DL.idx,r=I.release||{};return ["Simple Mods "+(r.version||"")+" ("+(r.commit||"")+", "+(r.date||"").slice(0,10)+") - Minecraft "+I.minecraft+" "+DL.lo,"",
"DE: Alle .jar-Dateien in den Ordner mods deines Minecraft-Profils legen.","EN: Put all .jar files into the mods folder of your Minecraft profile.","",
"Lizenz / License: alle Rechte vorbehalten, Quelltext einsehbar / all rights reserved, source-available (LICENSE_* in jedem Jar / in every jar).","Bitte nicht neu hochladen / please do not re-upload.","",
"Zusaetzlich noetig / also required:"].concat(P.ext.map(function(x){var e=(I.externalMods||{})[x]||{name:x,url:""};return "  "+e.name+" "+e.url}))
.concat(["","SHA-256:"]).concat(P.files.map(function(m){return "  "+m.sha256+"  "+m.file})).join("\r\n")+"\r\n"}
async function dlMakeZip(rerender){var P=DL.plan,t=DT[dlL()],es=[],sub=window.crypto&&crypto.subtle;if(!P||DL.busy)return;DL.busy=true;
try{for(var i=0;i<P.files.length;i++){var m=P.files[i];DL.msg=t.ld+" "+(i+1)+"/"+P.files.length+": "+m.file;rerender();
var u=dlUrl(m),buf;if(/^data:/.test(u))buf=dlData(u);else{var r=await fetch(u,{cache:"no-cache"});if(!r.ok)throw new Error(t.er+" "+m.file+" ("+r.status+")");buf=await r.arrayBuffer()}
if(sub){DL.msg=t.chk+" "+m.file;rerender();var hx=dlHex(await crypto.subtle.digest("SHA-256",buf));if(hx!=m.sha256)throw new Error(m.file+": "+t.bad)}
es.push({name:m.file,data:new Uint8Array(buf)})}
var enc=new TextEncoder();es.push({name:"LIESMICH-README.txt",data:enc.encode(dlReadme(P))});
dlSave(dlZip(es),"simplemods-"+DL.idx.minecraft+"-"+DL.lo+"-"+((DL.idx.release||{}).version||"release")+".zip");DL.msg=sub?t.ok:t.nochk}
catch(e){DL.msg=String(e.message||e)}finally{DL.busy=false;rerender()}}
function dlInit(rerender){document.addEventListener("click",function(e){var b=e.target.closest("[data-lo],[data-zip],[data-sums]");if(!b||b.disabled)return;
if(b.dataset.lo){DL.lo=b.dataset.lo;rerender()}else if(b.dataset.zip)dlMakeZip(rerender);
else if(DL.plan)dlSave(new Blob([dlSums(DL.plan)],{type:"text/plain"}),"sha256sums-"+DL.lo+".txt")});
if(!DL.idx)fetch(DL.base+"index.json",{cache:"no-cache"}).then(function(r){if(!r.ok)throw r.status;return r.json()})
.then(function(j){DL.idx=j;rerender()},function(){DL.err=true;rerender()})}
