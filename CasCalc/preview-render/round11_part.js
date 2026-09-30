// ---- Round of 30 September (5): saved graphs redesigned (Projects.kt), ħ on the key, upright
// script buttons with NCM upright Greek, Hebrew without ℵ–ℸ and the accent brought down.
const ncm=document.createElement('style');
ncm.textContent='@font-face{font-family:NCM;src:url(../app/src/main/res/font/ncm_math.otf)}@font-face{font-family:NCH;src:url(../app/src/main/res/font/ncm_hebrew.otf)}@font-face{font-family:NCI;src:url(../app/src/main/res/font/ncm_italic.otf)}';
document.head.appendChild(ncm);
const cap=t=>`<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`;
function put(html,cls,theme,c){const w=document.createElement('div');w.innerHTML=cap(c)+`<div class="${cls} ${theme}" style="position:relative">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const st=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
const sw=on=>`<span style="width:52px;height:32px;border-radius:99px;box-sizing:border-box;display:inline-flex;align-items:center;${on?'background:var(--pri);justify-content:flex-end;padding:0 4px':'border:2px solid var(--outv);background:var(--swb);padding:0 6px'}"><span style="width:${on?24:16}px;height:${on?24:16}px;border-radius:50%;background:${on?'var(--onpri)':'var(--onv)'}"></span></span>`;
const PATH={g2:'M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z',g3:'M3 4v4h2V6h2V4H3zm14 0v2h2v2h2V4h-4zM5 16H3v4h4v-2H5v-2zm14 2h-2v2h4v-4h-2v2zM12 3 6 6.5v7L12 17l6-3.5v-7L12 3zm4 9.4-4 2.3-4-2.3V7.6l4-2.3 4 2.3v4.8z',more:'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',search:'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',back:'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',table:'M10 10.02h5V21h-5zM17 21h3c1.1 0 2-.9 2-2v-9h-5v11zm3-18H5c-1.1 0-2 .9-2 2v3h19V5c0-1.1-.9-2-2-2zM3 19c0 1.1.9 2 2 2h3V10H3v9z',add:'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z'};
const ic=(k,s=22,c='currentColor')=>`<svg width="${s}" height="${s}" viewBox="0 0 24 24"><path d="${PATH[k]}" fill="${c}"/></svg>`;
const modeIc=m=>m=='2D'?ic('g2',20):m=='3D'?ic('g3',20):`<span style="font:italic 18px CMR">ℂ</span>`;
function group(items,sel){return `<div style="display:flex;gap:2px">${items.map((t,k)=>{const r=k==sel?'24px':`${k==0?24:8}px ${k==items.length-1?24:8}px ${k==items.length-1?24:8}px ${k==0?24:8}px`;
 return `<span style="flex:1;height:48px;display:flex;align-items:center;justify-content:center;gap:6px;border-radius:${r};background:${k==sel?'var(--pri)':'var(--swb)'};color:${k==sel?'var(--onpri)':'var(--on)'};font:500 14px GSF">${t}</span>`}).join('')}</div>`}
const G=[['Pendulum','2D',[['θ(t) = θ₀ <span style=\'font-family:CMR\'>cos</span>(ωt)','#5B6133'],['DATA','#3B665C'],['y = <span style=\'font-family:CMR\'>sin</span> x','#8A5A44']],5,'30 Sep 2026, 14:05','Today'],
 ['Lab results','2D',[['DATA','#5B6133'],['y = a x + b','#3B665C']],2,'30 Sep 2026, 09:12','Today'],
 ['Saddle','3D',[['z = x² − y²','#5B6133']],1,'29 Sep 2026, 18:12','Yesterday'],
 ['Zeta','ℂ',[['f(z) = ζ(z)','#5B6133']],1,'28 Sep 2026, 09:40','This week'],
 ['Torus','3D',[['(√(x²+y²) − 2)² + z² = 1','#5B6133'],['z = 0','#3B665C']],2,'25 Sep 2026, 11:02','This week'],
 ['Möbius','ℂ',[['f(z) = (z − i)/(z + i)','#5B6133']],1,'22 Sep 2026, 16:48','Earlier']];
function card([n,m,ls,count,date]){return `<div style="border-radius:28px;background:var(--card);padding:14px 8px 14px 18px;display:flex;flex-direction:column;gap:10px">
 <div style="display:flex;align-items:center;gap:6px"><div style="flex:1;min-width:0"><div style="font:500 17px GSF">${n}</div><div style="font:12px GSF;color:var(--onv)">${count} line${count>1?'s':''} · ${date}</div></div>
 <span style="width:40px;height:40px;border-radius:14px;background:var(--sec);color:var(--onsec);display:flex;align-items:center;justify-content:center">${modeIc(m)}</span><span style="width:40px;display:flex;justify-content:center;color:var(--onv)">${ic('more')}</span></div>
 <div style="margin-right:10px;border-radius:18px;background:var(--bg);padding:10px 12px;display:flex;flex-direction:column;gap:7px">${ls.map(([e,c])=>`<div style="display:flex;align-items:center;gap:10px;white-space:nowrap;overflow:hidden"><span style="width:10px;height:10px;flex:none;border-radius:50%;background:${c}"></span>${e=='DATA'?`<span style="display:inline-flex;align-items:center;gap:6px;border-radius:99px;background:var(--tc);color:var(--otc);padding:3px 10px;font:500 12px GSF">${ic('table',14)}Data set</span>`:`<span style="font:17px CMI">${e}</span>`}</div>`).join('')}
 ${count>3?`<div style="font:500 12px GSF;color:var(--onv);padding-left:20px">+ ${count-3} more</div>`:''}</div></div>`}
function topbar(){return `<div style="display:flex;align-items:center;gap:6px;padding:8px 12px"><span style="width:40px;display:flex;justify-content:center">${ic('back',24)}</span><span style="flex:1;font:400 24px GSF">Saved graphs</span><span style="width:40px;display:flex;justify-content:center;color:var(--onv)">${ic('search')}</span><span style="font:500 14px GSF;color:var(--pri);padding:0 8px">Newest first</span></div>`}
function fab(){return `<div style="position:absolute;right:20px;bottom:24px;display:flex;align-items:center;gap:10px;height:56px;padding:0 20px;border-radius:16px;background:var(--pc);color:var(--onpc);font:500 15px GSF;box-shadow:0 4px 10px rgba(0,0,0,.25)">${ic('add')}Save this graph</div>`}
function savedPhone(theme){P=PAL[theme];
 let body='';let last='';G.slice(0,4).forEach(g=>{if(g[5]!=last){body+=`<div style="font:500 14px GSF;color:var(--pri);padding:6px 4px 0">${g[5]}</div>`;last=g[5]}body+=card(g)});
 const h=st+topbar()+`<div style="padding:0 16px">${group(['All (6)',ic('g2',18)+'(2)',ic('g3',18)+'(2)','<span style="font:italic 17px CMR">ℂ</span>(2)'],0)}</div><div style="padding:4px 16px;display:flex;flex-direction:column;gap:12px;overflow:hidden">${body}</div>`+fab();
 put(h,'phone',theme,'Saved graphs · phone: counts in brackets, grouped by date, data sets as a chip')}
function savedTablet(theme){P=PAL[theme];
 const sec=(label,items)=>`<div style="grid-column:1/-1;font:500 14px GSF;color:var(--pri);padding:4px 4px 0">${label}</div>`+items.map(card).join('');
 const by={};G.forEach(g=>(by[g[5]]=by[g[5]]||[]).push(g));
 const h=`<div style="height:24px"></div>`+topbar()+`<div style="padding:0 24px;max-width:720px">${group(['All (6)',ic('g2',18)+'2D graphing (2)',ic('g3',18)+'3D graphing (2)','<span style="font:italic 17px CMR">ℂ</span>Complex plotting (2)'],0)}</div>
 <div style="box-sizing:border-box;width:100%;padding:8px 24px;display:grid;grid-template-columns:repeat(3,1fr);gap:12px;align-content:start;overflow:hidden">${Object.entries(by).map(([k,v])=>sec(k,v)).join('')}</div>`+fab();
 put(h,'tablet',theme,'Saved graphs · tablet: the whole screen, a grid of cards, mode in each card\'s corner')}
function builder(theme){P=PAL[theme];
 const key=(t,sel,f)=>`<span style="height:44px;min-width:44px;border-radius:14px;display:flex;align-items:center;justify-content:center;font:21px ${f};background:${sel?'var(--pc)':'var(--swb)'};${sel?'box-shadow:inset 0 0 0 2px var(--pri)':''}">${t}</span>`;
 const slot=(n,c,a,f)=>`<div style="border-radius:14px;padding:6px 8px;background:${a?'var(--pc)':'var(--swb)'};${a?'box-shadow:inset 0 0 0 2px var(--pri)':''}"><div style="font:500 11px GSF;color:var(--onv)">${n}</div><div style="height:28px;display:flex;align-items:center;font:20px ${f||'CMR'}">${c||'<span style="color:var(--outv);font:14px GSF">empty</span>'}</div></div>`;
 const heb='אבגדהוזחטיכלמנסעפצקרשת';
 const h=st+`<div style="padding:4px 20px;display:flex;flex-direction:column;gap:10px">
  <div style="font:400 22px GSF">Build a symbol</div>
  <div style="height:120px;border-radius:24px;background:var(--card);display:flex;align-items:center;justify-content:center"><span style="display:inline-flex;align-items:flex-end;font-size:60px"><span style="position:relative;font-family:NCH;line-height:1">ב<span style="position:absolute;left:50%;top:-.28em;transform:translateX(-50%);font-family:CMR">^</span></span><span style="font:26px NCM;margin-bottom:-4px">αβ</span></span></div>
  <div style="font:500 14px GSF;color:var(--pri)">Letter</div>
  ${group(['<span style="font:19px CMI">Aa</span>','<span style="font:19px CMR">Γ</span><span style="font:19px CMI">γ</span>','<span style="font:19px NCH">אב</span>','<span style="font:19px CMR">𝒜ℬ</span>','<span style="font:19px CMR">𝔄𝔞</span>','<span style="font:19px NCM">𝔸𝔹</span>'],2)}
  <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:6px">${[...heb].slice(0,14).map((c,k)=>key(c,k==1,'NCH')).join('')}</div>
  <div style="font:500 14px GSF;color:var(--pri)">Scripts</div>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px">${slot('Left superscript','')}${slot('Right superscript','')}${slot('Left subscript','')}${slot('Right subscript','αβ',true,'NCM')}</div>
  <div style="display:flex;align-items:center;justify-content:space-between"><div><div style="font:16px GSF">Italic</div><div style="font:12px GSF;color:var(--onv)">Off: the buttons below turn upright too</div></div>${sw(false)}</div>
  <div style="display:flex;gap:6px">${['Numbers','Latin','Greek'].map((t,k)=>`<span style="border-radius:8px;padding:7px 12px;font:500 13px GSF;${k==2?'background:var(--sec);color:var(--onsec)':'border:1px solid var(--outv);color:var(--onv)'}">${k==2?'✓ ':''}${t}</span>`).join('')}</div>
  <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:6px">${[...'αβγδεζηθικλμνξ'].map(c=>key(c,false,'NCM')).join('')}</div>
 </div>`;
 put(h,'phone',theme,'Symbol builder · Hebrew letters only, accent sits on the letter, upright Greek when italic is off')}
function keyH(theme){P=PAL[theme];
 const k=(t)=>`<span style="height:52px;border-radius:99px;background:var(--sec);color:var(--onsec);display:flex;align-items:center;justify-content:center;font:500 24px GSF">${t}</span>`;
 const h=`<div style="padding:20px;display:grid;grid-template-columns:repeat(4,78px);gap:8px">${k('<i style="font-family:CMI">c</i>')}${k('<i style="font-family:CMI">h</i>')}${k('ħ')}${k('<i style="font-family:CMI">k</i><sub style="font:14px CMR">B</sub>')}</div>
 <div style="padding:0 20px 20px;font:34px CMR">E = <span style="font-family:NCI">ħ</span><i style="font-family:CMI">ω</i></div>`;
 const w=document.createElement('div');w.innerHTML=cap('ħ: the Maltese h on the key, in the key font; italic ħ in maths')+`<div class="${theme}" style="background:var(--bg);color:var(--on);border-radius:24px;width:380px">${h}</div>`;document.getElementById('sheet').appendChild(w)}
Promise.all(['GSF','Rob','CMR','CMI','NCM','NCH','NCI'].map(f=>document.fonts.load('400 20px '+f,f=='NCM'?'α𝔸':f=='NCH'?'ב':f=='NCI'?'ħ':'a'))).then(()=>{
 savedPhone('light');builder('dark');keyH('light');
 const br=document.createElement('div');br.style.cssText='width:100%;height:0';document.getElementById('sheet').appendChild(br);
 savedTablet('dark');
 document.title='ready'});
