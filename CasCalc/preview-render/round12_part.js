// ---- Round of 30 September (6): Cyrillic in the symbol builder, italic ħ on the key, point
// size in tenths, graph files (.g2d .g3d .gcp) in the export dialog and saved graphs.
const ncm=document.createElement('style');
ncm.textContent='@font-face{font-family:NCM;src:url(../app/src/main/res/font/ncm_math.otf)}@font-face{font-family:NCH;src:url(../app/src/main/res/font/ncm_hebrew.otf)}@font-face{font-family:NCC;src:url(../app/src/main/res/font/ncm_cyrillic.otf)}';
document.head.appendChild(ncm);
const cap=t=>`<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`;
function put(html,cls,theme,c){const w=document.createElement('div');w.innerHTML=cap(c)+`<div class="${cls} ${theme}" style="position:relative">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const st=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
function group(items,sel){return `<div style="display:flex;gap:2px">${items.map((t,k)=>{const r=k==sel?'24px':`${k==0?24:8}px ${k==items.length-1?24:8}px ${k==items.length-1?24:8}px ${k==0?24:8}px`;
 return `<span style="flex:1;height:48px;display:flex;align-items:center;justify-content:center;gap:6px;border-radius:${r};background:${k==sel?'var(--pri)':'var(--swb)'};color:${k==sel?'var(--onpri)':'var(--on)'};font:500 14px GSF">${t}</span>`}).join('')}</div>`}
const key=(t,sel,f)=>`<span style="height:44px;border-radius:14px;display:flex;align-items:center;justify-content:center;font:21px ${f};background:${sel?'var(--pc)':'var(--swb)'};${sel?'box-shadow:inset 0 0 0 2px var(--pri)':''}">${t}</span>`;
function builder(theme){P=PAL[theme];
 const cyr='АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзий';
 const h=st+`<div style="padding:4px 20px;display:flex;flex-direction:column;gap:10px">
  <div style="font:400 22px GSF">Build a symbol</div>
  <div style="height:120px;border-radius:24px;background:var(--card);display:flex;align-items:center;justify-content:center"><span style="display:inline-flex;align-items:flex-end;font-size:64px"><span style="font-family:NCC;line-height:1">Ш</span><span style="font:26px CMR;margin-bottom:-4px">0</span></span></div>
  <div style="font:13px GSF;color:var(--onv);text-align:center">LaTeX: <span style="font-family:monospace">Ш_{0}</span></div>
  <div style="font:500 14px GSF;color:var(--pri)">Letter</div>
  ${group(['<span style="font:18px CMI">Aa</span>','<span style="font:18px CMR">Γ</span><span style="font:18px CMI">γ</span>','<span style="font:18px NCH">אב</span>','<span style="font:18px NCC">Бб</span>','<span style="font:18px CMR">𝒜ℬ</span>','<span style="font:18px CMR">𝔄𝔞</span>','<span style="font:18px NCM">𝔸𝔹</span>'],3)}
  <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:6px">${[...cyr].slice(0,42).map(c=>key(c,c=='Ш','NCC')).join('')}</div>
 </div>`;
 put(h,'phone',theme,'Symbol builder · Cyrillic (New Computer Modern, upright as in TeX)')}
function pointAndKey(theme){P=PAL[theme];
 const k=(t)=>`<span style="height:52px;border-radius:99px;background:var(--sec);color:var(--onsec);display:flex;align-items:center;justify-content:center;font:500 24px GSF">${t}</span>`;
 const track=f=>`<div style="position:relative;height:44px;display:flex;align-items:center"><div style="position:absolute;left:0;height:24px;width:calc(${f*100}% - 8px);border-radius:12px 2px 2px 12px;background:var(--pri)"></div><div style="position:absolute;right:0;height:24px;width:calc(${(1-f)*100}% - 8px);border-radius:2px 12px 12px 2px;background:var(--swb)"></div><div style="position:absolute;left:calc(${f*100}% - 2px);width:4px;height:44px;border-radius:2px;background:var(--pri)"></div></div>`;
 const h=`<div style="padding:20px;display:flex;flex-direction:column;gap:14px;width:340px">
  <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px">${k('<i style="font-family:CMI">c</i>')}${k('<i style="font-family:CMI">h</i>')}${k('<span style="font-style:oblique 10deg">ħ</span>')}${k('<i style="font-family:CMI">k</i><sub style="font:14px CMR">B</sub>')}</div>
  <div style="font:13px GSF;color:var(--onv)">Size: 4.5 dp</div>${track(.18)}</div>`;
 const w=document.createElement('div');w.innerHTML=cap('ħ slanted on the key · point size to one decimal')+`<div class="${theme}" style="background:var(--bg);color:var(--on);border-radius:24px">${h}</div>`;document.getElementById('sheet').appendChild(w)}
function exportDlg(theme){P=PAL[theme];
 const seg=(items,sel)=>`<div style="display:flex;border:1px solid var(--outv);border-radius:99px;overflow:hidden;font:500 13px GSF">${items.map((m,k)=>`<span style="flex:1;text-align:center;padding:9px 0;${k?'border-left:1px solid var(--outv);':''}${k==sel?'background:var(--sec);color:var(--onsec)':''}">${m}</span>`).join('')}</div>`;
 const tonal=t=>`<span style="border-radius:99px;background:var(--sec);color:var(--onsec);padding:10px 18px;font:500 14px GSF">${t}</span>`;
 const h=st+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:360px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:10px">
  <div style="font:400 24px GSF">Export graph</div>
  <div style="height:190px;border-radius:16px;background:#fff;border:1px solid var(--outv)"></div>
  ${seg(['PDF','PNG','JPG','SVG'],0)}
  <div style="font:12px GSF;color:var(--onv)">Vector: sharp at any size, for documents and printing.</div>
  <div style="font:12px GSF;color:var(--onv)">… limits, light / dark …</div>
  <div style="height:1px;background:var(--outv);margin:4px 0"></div>
  <div style="font:500 14px GSF;color:var(--onv)">Graph file (.g2d)</div>
  <div style="font:12px GSF;color:var(--onv)">The graph itself, with its lines, colours and sliders, to open again in CasCalc (Saved graphs › Import).</div>
  <div style="display:flex;gap:8px">${tonal('Share file')}${tonal('Save file')}</div>
  <div style="text-align:right;font:500 15px GSF;color:var(--pri);margin-top:6px"><span style="margin-right:22px">Cancel</span><span style="margin-right:22px">Share</span>Save</div></div></div>`;
 put(h,'phone',theme,'Export · the graph as a .g2d / .g3d / .gcp file')}
function savedMenu(theme){P=PAL[theme];
 const item=(t,c)=>`<div style="padding:13px 16px;font:15px GSF;${c?'color:var(--err)':''}">${t}</div>`;
 const h=st+`<div style="display:flex;align-items:center;gap:6px;padding:8px 12px"><span style="width:40px;text-align:center;font-size:22px">←</span><span style="flex:1;font:400 24px GSF">Saved graphs</span><span style="width:36px;text-align:center;color:var(--onv)">⌕</span><span style="width:40px;height:40px;border-radius:50%;background:var(--sec);display:flex;align-items:center;justify-content:center;color:var(--onsec);font-size:18px" title="Import">⤓</span><span style="font:500 14px GSF;color:var(--pri);padding:0 6px">Newest first</span></div>
 <div style="margin:10px 16px;border-radius:28px;background:var(--card);padding:14px 18px;height:170px;position:relative"><div style="font:500 17px GSF">Saddle</div><div style="font:12px GSF;color:var(--onv)">1 line · 29 Sep 2026, 18:12</div>
  <div style="position:absolute;right:14px;top:52px;width:230px;border-radius:12px;background:var(--dig);box-shadow:0 6px 18px rgba(0,0,0,.3);padding:6px 0">${item('✎  Rename')}${item('⧉  Duplicate')}${item('⇪  Share as .g3d file')}${item('⤓  Save as .g3d file')}${item('🗑  Delete',1)}</div></div>
 <div style="margin:10px 16px;border-radius:14px;background:var(--inv);color:var(--oninv);padding:14px 16px;font:14px GSF;position:absolute;bottom:40px;left:0;right:0">Imported “Saddle (2)” (3D graphing)</div>`;
 put(h,'phone',theme,'Saved graphs · Import (top bar) and export per graph')}
Promise.all(['GSF','CMR','CMI','NCM','NCH','NCC'].map(f=>document.fonts.load('400 20px '+f,f=='NCC'?'Ш':f=='NCM'?'𝔸':f=='NCH'?'ב':'a'))).then(()=>{
 builder('light');exportDlg('dark');savedMenu('light');pointAndKey('dark');
 document.title='ready'});
