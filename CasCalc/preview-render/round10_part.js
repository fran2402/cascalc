// ---- Round of 30 September (4): symbol builder fixes (SymbolBuilder.kt), 3D letters like the
// calculus chooser (Graph3DScreen.kt), slider gap (Expressive.kt), folders and points
// (GraphCommon.kt), saved graphs across modes (Projects.kt), tablet export (GraphExport.kt).
const ncm=document.createElement('style');
ncm.textContent='@font-face{font-family:NCM;src:url(../app/src/main/res/font/ncm_math.otf)}@font-face{font-family:NCH;src:url(../app/src/main/res/font/ncm_hebrew.otf)}@font-face{font-family:NCI;src:url(../app/src/main/res/font/ncm_italic.otf)}';
document.head.appendChild(ncm);
const cap=t=>`<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`;
function put(html,cls,theme,c){const w=document.createElement('div');w.innerHTML=cap(c)+`<div class="${cls} ${theme}" style="position:relative">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const st=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
const sw=on=>`<span style="width:52px;height:32px;border-radius:99px;box-sizing:border-box;display:inline-flex;align-items:center;${on?'background:var(--pri);justify-content:flex-end;padding:0 4px':'border:2px solid var(--outv);background:var(--swb);padding:0 6px'}"><span style="width:${on?24:16}px;height:${on?24:16}px;border-radius:50%;background:${on?'var(--onpri)':'var(--onv)'}"></span></span>`;
const title=t=>`<div style="font:500 14px GSF;color:var(--pri);margin-top:6px">${t}</div>`;
const cmi=t=>`<span style="font-family:CMI">${t}</span>`;
/** M3 connected button group: 48 dp, 2 dp gaps, outer 24 dp, inner 8 dp, the chosen one a pill. */
function group(items,sel){return `<div style="display:flex;gap:2px">${items.map((t,k)=>{const r=k==sel?'24px':`${k==0?24:8}px ${k==items.length-1?24:8}px ${k==items.length-1?24:8}px ${k==0?24:8}px`;
 return `<span style="flex:1;height:48px;display:flex;align-items:center;justify-content:center;border-radius:${r};background:${k==sel?'var(--pri)':'var(--swb)'};color:${k==sel?'var(--onpri)':'var(--on)'};font-size:19px">${t}</span>`}).join('')}</div>`}
const alph=['<span style="font-family:CMI">A</span><span style="font-family:CMI">a</span>','<span style="font-family:CMR">Γ</span><span style="font-family:CMI">γ</span>','<span style="font-family:NCH">ℵב</span>','<span style="font-family:CMR">𝒜ℬ</span>','<span style="font-family:CMR">𝔄𝔞</span>','<span style="font-family:NCM">𝔸𝔹</span>'];
const key=(t,sel,f)=>`<span style="height:44px;border-radius:14px;display:flex;align-items:center;justify-content:center;font:22px ${f};background:${sel?'var(--pc)':'var(--swb)'};${sel?'box-shadow:inset 0 0 0 2px var(--pri)':''}">${t}</span>`;
function slot(name,content,active,upright){return `<div style="border-radius:14px;padding:6px 8px;background:${active?'var(--pc)':'var(--swb)'};${active?'box-shadow:inset 0 0 0 2px var(--pri)':''}"><div style="font:500 11px GSF;color:var(--onv)">${name}</div><div style="height:28px;display:flex;align-items:center;font:20px ${upright?'CMR':'CMI'}">${content||'<span style="color:var(--outv);font:14px GSF">empty</span>'}</div></div>`}
function builder(theme){P=PAL[theme];
 const letters='ℵℶℷℸאבגדהוזחטיכלמנסעפצקרשת'.slice(0,24);
 const h=st+`<div style="padding:4px 20px;display:flex;flex-direction:column;gap:10px">
  <div style="display:flex;align-items:center;gap:12px;font:400 22px GSF"><span style="font-size:24px">←</span>New symbol</div>
  <div style="height:110px;border-radius:24px;background:var(--card);display:flex;align-items:center;justify-content:center">
   <span style="display:inline-flex;align-items:center;font-size:56px"><span style="display:inline-flex;flex-direction:column;font:24px CMR;line-height:1.3;margin-right:2px"><span>&nbsp;</span><span>&nbsp;</span></span><span style="font-family:NCM">𝔹</span><span style="display:inline-flex;flex-direction:column;font:24px CMR;line-height:1.3;margin-left:2px"><span style="font-family:CMI">n</span><span>max</span></span></span></div>
  <div style="font:13px GSF;color:var(--onv);text-align:center">LaTeX: <span style="font-family:monospace">\\mathbb{B}_{\\text{max}}^{n}</span></div>
  ${title('Letter')}${group(alph,5)}
  <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:6px">${[...'𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ'].map((c,k)=>key(c,k==1,'NCM')).join('')}</div>
  ${title('Scripts')}
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px">${slot('Left superscript','')}${slot('Right superscript','n')}${slot('Left subscript','')}${slot('Right subscript','max',true,true)}</div>
  <div style="display:flex;align-items:center;justify-content:space-between"><div><div style="font:16px GSF">Italic</div><div style="font:12px GSF;color:var(--onv)">For the right subscript</div></div>${sw(false)}</div>
  <div style="font:12px GSF;color:var(--onv)">Tap a box, then a letter to put it there. Double-tap a box to type text from the keyboard (upright, like \\text).</div>
 </div>`;
 put(h,'phone',theme,'Symbol builder · connected letter group, named script boxes, italic toggle')}
function hebrew(theme){P=PAL[theme];
 const h=st+`<div style="padding:4px 20px;display:flex;flex-direction:column;gap:10px">
  <div style="display:flex;align-items:center;gap:12px;font:400 22px GSF"><span style="font-size:24px">←</span>New symbol</div>
  <div style="height:110px;border-radius:24px;background:var(--card);display:flex;align-items:center;justify-content:center;font-size:56px"><span style="font-family:NCH">ℵ</span><span style="font:24px CMR;align-self:flex-end;margin-bottom:26px">0</span></div>
  ${title('Letter')}${group(alph,2)}
  <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:6px">${[...'ℵℶℷℸאבגדהוזחטיכלמנסעפ'].map((c,k)=>key(c,k==0,'NCH')).join('')}</div>
  ${title('Greek, in Computer Modern')}${group(alph,1)}
  <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:6px">${[...'ΑΒΓΔΕΖΗΘΙΚΛΜΝΞ'].map(c=>key(c,false,'CMR')).join('')}${[...'αβγδεζηθικλμνϰ'].map(c=>key(c,false,c=='ϰ'?'NCM':'CMI')).join('')}</div>
 </div>
 <div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:340px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:14px">
  <div style="font:400 24px GSF">Right subscript</div>
  <div style="border:2px solid var(--pri);border-radius:6px;padding:14px 12px;font:17px GSF;position:relative"><span style="position:absolute;top:-9px;left:10px;background:var(--dig);padding:0 4px;font-size:12px;color:var(--pri)">Text</span>max|</div>
  <div style="font:13px GSF;color:var(--onv)">Typed text stays upright (like \\text{max}). Turn on Italic to slant it.</div>
  <div style="text-align:right;font:500 15px GSF;color:var(--pri)"><span style="margin-right:22px">Cancel</span>Done</div></div></div>`;
 put(h,'phone',theme,'Hebrew and Greek in New Computer Modern · double-tap a box to type')}
function letters3D(theme){P=PAL[theme];
 const seg=(items,sel)=>`<div style="display:flex;border:1px solid var(--outv);border-radius:99px;overflow:hidden;height:40px;font:18px CMI;flex:1">${items.map((t,k)=>`<span style="flex:1;display:flex;align-items:center;justify-content:center;${k?'border-left:1px solid var(--outv);':''}${k==sel?'background:var(--sec);color:var(--onsec)':''}">${k==sel?'<span style="font:14px GSF;margin-right:4px">✓</span>':''}${t}</span>`).join('')}</div>`;
 const h=st+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:18px">
  <div style="width:360px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:12px">
   <div style="font:400 24px GSF">3D settings</div>
   <div style="font:500 14px GSF;color:var(--onv)">Coordinates</div>
   <div style="display:flex;align-items:center;gap:6px">${seg(['x y z','r θ z','ρ θ φ'],0)}<span style="width:40px;height:40px;display:flex;align-items:center;justify-content:center;font-size:20px;color:var(--onv)">✎</span></div>
   <div style="font:12px GSF;color:var(--onv)">Same as choosing the variables for calculus: ✎ picks the letters.</div>
   <div style="text-align:right;font:500 15px GSF;color:var(--pri)"><span style="margin-right:22px">Cancel</span>Done</div></div>
  <div style="width:360px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:10px">
   <div style="font:400 24px GSF">Letters · Spherical</div>
   ${[['Distance from the origin',['ρ','r','R','s'],0],['Angle around the z-axis',['θ','φ','α','t'],0],['Angle down from the z-axis',['φ','θ','β','ψ'],0]].map(([r,ls,s])=>`<div style="font:13px GSF;color:var(--onv)">${r}</div><div style="display:flex;gap:6px">${ls.map((l,k)=>`<span style="width:44px;height:40px;border-radius:12px;display:flex;align-items:center;justify-content:center;font:20px CMI;background:${k==s?'var(--pc)':'var(--swb)'}">${l}</span>`).join('')}</div>`).join('')}
   <div style="text-align:right;font:500 15px GSF;color:var(--pri)"><span style="margin-right:22px">Cancel</span>Done</div></div></div>`;
 put(h,'phone',theme,'3D coordinates · the calculus-style chooser')}
function sliders(theme){P=PAL[theme];
 const track=(grad,f)=>`<div style="position:relative;height:44px;display:flex;align-items:center"><div style="position:absolute;left:0;height:24px;width:calc(${f*100}% - 8px);border-radius:12px 2px 2px 12px;background:${grad};background-size:292px 100%"></div><div style="position:absolute;right:0;height:24px;width:calc(${(1-f)*100}% - 8px);border-radius:2px 12px 12px 2px;background:${grad};background-size:292px 100%;background-position:right"></div><div style="position:absolute;left:calc(${f*100}% - 2px);width:4px;height:44px;border-radius:2px;background:var(--on)"></div></div>`;
 const hue='linear-gradient(90deg,#f00,#ff0,#0f0,#0ff,#00f,#f0f,#f00)';
 const shape=(d,sel)=>`<span style="width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;background:${sel?'var(--sec)':'var(--swb)'}"><svg width="20" height="20" viewBox="-10 -10 20 20">${d}</svg></span>`;
 const c='currentColor',fill=(p)=>`<path d="${p}" fill="${c}"/>`;
 const shapes=[fill('M7 0A7 7 0 1 1 -7 0A7 7 0 1 1 7 0'),fill('M-6-6H6V6H-6Z'),fill('M0-8L8 0L0 8L-8 0Z'),fill('M0-8L7.5 5H-7.5Z'),fill('M0 8L7.5-5H-7.5Z'),fill('M0-8L7.6-2.5L4.7 6.5H-4.7L-7.6-2.5Z'),fill('M7.5 0L3.7 6.5H-3.7L-7.5 0L-3.7-6.5H3.7Z'),fill('M0-9L2.3-3.2L8.6-2.8L3.7 1.2L5.3 7.3L0 3.9L-5.3 7.3L-3.7 1.2L-8.6-2.8L-2.3-3.2Z'),`<path d="M-6-6L6 6M-6 6L6-6" stroke="${c}" stroke-width="2.6" stroke-linecap="round"/>`,`<path d="M-7 0H7M0-7V7" stroke="${c}" stroke-width="2.6" stroke-linecap="round"/>`,`<path d="M0-7V7M-6-3.5L6 3.5M-6 3.5L6-3.5" stroke="${c}" stroke-width="2.4" stroke-linecap="round"/>`];
 const hollow=s=>s.replace(/fill="currentColor"/,'fill="none" stroke="currentColor" stroke-width="2.6"');
 const h=st+`<div style="padding:4px 20px;display:flex;flex-direction:column;gap:10px">
  <div style="font:400 22px GSF">Color</div>
  ${['Hue','Saturation','Lightness'].map((n,k)=>`<div style="font:13px GSF;color:var(--onv)">${n}</div>`+track(k==0?hue:k==1?'linear-gradient(90deg,#808080,#1f6fd1)':'linear-gradient(90deg,#000,#1f6fd1,#fff)',[.58,.72,.46][k])).join('')}
  <div style="font:12px GSF;color:var(--onv)">The track stops 6 dp before and after the thumb, as the M3 Expressive slider does.</div>
  <div style="height:1px;background:var(--outv);margin:6px 0"></div>
  ${title('Point')}
  <div style="display:flex;flex-wrap:wrap;gap:6px;color:var(--onv)">${shapes.map((s,k)=>shape(k<8?hollow(s):s,k==1)).join('')}</div>
  <div style="display:flex;align-items:center;justify-content:space-between"><span style="font:16px GSF">Filled</span>${sw(false)}</div>
  <div style="font:13px GSF;color:var(--onv)">Size: 6 dp</div>${track('var(--pri)',.3).replace(/background-size:292px 100%;?/g,'')}
  <div style="height:1px;background:var(--outv);margin:6px 0"></div>
  ${title('Folders')}
  <div style="border-radius:20px;background:var(--dig);height:48px;display:flex;align-items:center;gap:6px;padding:0 8px;font:16px GSF"><span style="font-size:18px;color:var(--on)">⌄</span><span style="color:var(--onv)">👁</span>Circles<span style="margin-left:auto;font-size:12px;color:var(--onv)">2</span></div>
  ${[['x²+y²=1','#5B6133'],['x²+y²=4','#3B665C']].map(([e,c])=>`<div style="display:flex;align-items:stretch"><span style="width:4px;margin:6px 5px;border-radius:2px;background:var(--pri)"></span><div style="flex:1;border-radius:20px;background:var(--card);height:52px;display:flex;align-items:center;gap:10px;padding:0 12px;font:21px CMI"><span style="width:18px;height:18px;border-radius:50%;background:${c}"></span>${e}</div></div>`).join('')}
 </div>`;
 put(h,'phone',theme,'Slider gap · points: shape + Filled · folders in the primary color')}
function saved(theme){P=PAL[theme];
 const icons={'2D':'📈','3D':'🧊','ℂ':'ℂ'};
 const cards=[['Pendulum','2D',['θ(t)=θ₀cos(ωt)','y=sin x'],'3 lines · 30 Sep 2026, 14:05'],['Saddle','3D',['z=x²−y²'],'1 line · 29 Sep 2026, 18:12'],['Zeta','ℂ',['f(z)=ζ(z)'],'1 line · 28 Sep 2026, 09:40'],['Circles','2D',['x²+y²=1','x²+y²=4'],'2 lines · 27 Sep 2026, 21:30'],['Torus','3D',['(√(x²+y²)−2)²+z²=1'],'1 line · 25 Sep 2026, 11:02'],['Möbius','ℂ',['f(z)=(z−i)/(z+i)'],'1 line · 22 Sep 2026, 16:48']];
 const chip=(t,s)=>`<span style="border-radius:8px;padding:7px 12px;font:500 14px GSF;${s?'background:var(--sec);color:var(--onsec)':'border:1px solid var(--outv);color:var(--onv)'}">${s?'✓ ':''}${t}</span>`;
 const h=`<div style="height:40px"></div><div style="display:flex;align-items:center;gap:14px;padding:0 28px;font:400 28px GSF"><span>←</span>Saved graphs</div>
 <div style="padding:16px 40px;display:flex;flex-direction:column;gap:16px">
  <div><span style="display:inline-flex;align-items:center;gap:8px;border-radius:99px;background:var(--pri);color:var(--onpri);padding:12px 22px;font:500 15px GSF">📈 Save the current graph</span></div>
  <div style="display:flex;gap:8px">${chip('All  6',true)}${chip('2D graphing  2')}${chip('3D graphing  2')}${chip('Complex plotting  2')}</div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px">${cards.map(([n,m,ls,meta])=>`<div style="position:relative;border-radius:24px;background:var(--dig);padding:14px 8px 4px 16px;display:flex;flex-direction:column;gap:6px">
   <div style="font:500 18px GSF">${n}</div>
   <div style="border-radius:12px;background:var(--bg);padding:6px 10px;font:18px CMI;min-height:52px">${ls.map(l=>`<div>${l}</div>`).join('')}</div>
   <div style="display:flex;align-items:center;font:12px GSF;color:var(--onv)"><span style="flex:1">${meta}</span><span style="width:40px;text-align:center;font-size:17px">✎</span><span style="width:40px;text-align:center;font-size:17px">🗑</span></div>
   <span style="position:absolute;top:10px;right:10px;width:34px;height:34px;border-radius:50%;background:var(--sec);color:var(--onsec);display:flex;align-items:center;justify-content:center;font:16px CMR">${icons[m]}</span></div>`).join('')}</div>
  <div style="font:13px GSF;color:var(--onv)">Opening a graph from another mode switches to that mode.</div>
 </div>`;
 put(h,'tablet',theme,'Saved graphs · every mode, full screen on a tablet, mode in the corner')}
function exportTab(theme){P=PAL[theme];
 const seg=(items,sel)=>`<div style="display:flex;border:1px solid var(--outv);border-radius:99px;overflow:hidden;font:500 14px GSF">${items.map((m,k)=>`<span style="flex:1;text-align:center;padding:10px 0;${k?'border-left:1px solid var(--outv);':''}${k==sel?'background:var(--sec);color:var(--onsec)':''}">${m}</span>`).join('')}</div>`;
 const field=(l,v)=>`<div style="flex:1;border:1px solid var(--outv);border-radius:6px;padding:14px 12px;font:16px GSF;position:relative"><span style="position:absolute;top:-9px;left:10px;background:var(--dig);padding:0 4px;font-size:12px;color:var(--onv)">${l}</span>${v}</div>`;
 const plot=`<svg viewBox="0 0 400 400" width="100%" height="100%" style="background:#fff"><rect x="50" y="20" width="330" height="330" fill="none" stroke="#000"/>${[0,1,2,3,4].map(i=>`<line x1="${50+i*82.5}" y1="350" x2="${50+i*82.5}" y2="345" stroke="#000"/><line x1="50" y1="${20+i*82.5}" x2="55" y2="${20+i*82.5}" stroke="#000"/>`).join('')}<path d="${Array.from({length:120},(_,i)=>{const x=-5+i*10/119;return (i?'L':'M')+(50+(x+5)*33).toFixed(1)+' '+(185-Math.sin(x)*110).toFixed(1)}).join('')}" fill="none" stroke="#0C5DA5" stroke-width="2"/><path d="${Array.from({length:120},(_,i)=>{const x=-5+i*10/119;return (i?'L':'M')+(50+(x+5)*33).toFixed(1)+' '+(185-Math.cos(x)*110).toFixed(1)}).join('')}" fill="none" stroke="#00B945" stroke-width="2"/><text x="215" y="385" font-family="CMI" font-size="20">x</text></svg>`;
 const h=`<div style="flex:1;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:940px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:16px">
  <div style="font:400 26px GSF">Export graph</div>
  <div style="display:flex;gap:24px"><div style="flex:1.1;aspect-ratio:1;border-radius:16px;overflow:hidden;border:1px solid var(--outv)">${plot}</div>
  <div style="flex:1;display:flex;flex-direction:column;gap:12px">${seg(['PDF','PNG','JPG','SVG'],0)}<div style="font:13px GSF;color:var(--onv)">Vector: sharp at any size, for documents and printing.</div>
   <div style="font:500 14px GSF;color:var(--onv)">Limits</div>
   <div style="display:flex;gap:8px">${field('x from','−5')}${field('x to','5')}</div><div style="display:flex;gap:8px">${field('y from','−1.5')}${field('y to','1.5')}</div>
   ${seg(['Light','Dark'],0)}
   <div style="font:13px GSF;color:var(--onv)">Saved as <span style="font-family:monospace">graph-2026-09-30_14-05-12.pdf</span></div></div></div>
  <div style="text-align:right;font:500 15px GSF;color:var(--pri)"><span style="margin-right:22px">Cancel</span><span style="margin-right:22px">Share</span>Save</div></div></div>`;
 put(h,'tablet',theme,'Export on a tablet · preview beside the options, file name with date and time')}
Promise.all(['GSF','Rob','CMR','CMI','NCM','NCH','NCI'].map(f=>document.fonts.load('400 20px '+f,f=='NCM'?'𝔸':f=='NCH'?'ℵ':'a'))).then(()=>{
 builder('light');hebrew('dark');letters3D('light');sliders('dark');
 const br=document.createElement('div');br.style.cssText='width:100%;height:0';document.getElementById('sheet').appendChild(br);
 saved('light');exportTab('dark');
 document.title='ready'});
