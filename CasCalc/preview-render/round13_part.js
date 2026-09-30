// ---- Round 13 (graph list and graphs): folders with colours, the M3 FAB menu, area between
// curves under the legend, line styles, scalar fields, the complex grid, 3D coordinate chips,
// the tablet's resizable list and the export legend's TeX integrals.
const s2=document.createElement('style');
s2.textContent='@font-face{font-family:CMS2;src:url(../app/src/main/res/font/cm_size2.otf)}';
document.head.appendChild(s2);
const cap=t=>`<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`;
function put(html,cls,theme,c){const w=document.createElement('div');w.innerHTML=cap(c)+`<div class="${cls} ${theme}" style="position:relative">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const st=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
const PATHS={add:'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',close:'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',fx:'M8 19c2 0 2.6-2 3-5l1.3-8c.4-2.2 1.2-3 2.7-3M7 9h7',note:'M3 18h12v-2H3v2zM3 6v2h18V6H3zm0 7h18v-2H3v2z',folder:'M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z',folderNew:'M20 6h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-1 8h-3v3h-2v-3h-3v-2h3V9h2v3h3v2z',table:'M10 10.02h5V21h-5zM17 21h3c1.1 0 2-.9 2-2v-9h-5v11zm3-18H5c-1.1 0-2 .9-2 2v3h19V5c0-1.1-.9-2-2-2zM3 19c0 1.1.9 2 2 2h3V10H3v9z',eye:'M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z',drag:'M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',down:'M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z',right:'M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z',share:'M16 5l-1.42 1.42-1.59-1.59V16h-1.98V4.83L9.42 6.42 8 5l4-4 4 4zm4 5v11c0 1.1-.9 2-2 2H6c-1.1 0-2-.9-2-2V10c0-1.11.89-2 2-2h3v2H6v11h12V10h-3V8h3c1.1 0 2 .89 2 2z',tune:'M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z'};
const ic=(k,s=22,c='currentColor',extra='')=>`<svg width="${s}" height="${s}" viewBox="0 0 24 24" ${extra}><path d="${PATHS[k]}" fill="${k=='fx'?'none':c}" ${k=='fx'?`stroke="${c}" stroke-width="2" stroke-linecap="round"`:''}/></svg>`;
const G={mint:'#5B6133',blue:'#4C8DF6',orange:'#E8710A',green:'#1E9E54'};
// A line row: dot, maths, drag handle; depth bars in folder colours.
function lineRow(math,color,bars=[],active=false){return `<div style="display:flex;align-items:stretch">${bars.map(c=>`<span style="width:4px;margin:7px 5px;border-radius:2px;background:${c}"></span>`).join('')}<div style="flex:1;display:flex;align-items:center;gap:8px;border-radius:20px;background:${active?'var(--dig)':'var(--card)'};height:54px;padding:0 6px 0 10px"><span style="width:26px;height:26px;border-radius:50%;background:${color}"></span><span style="flex:1;font:21px CMI">${math}</span><span style="color:var(--onv);display:flex">${ic('drag',22)}</span></div></div>`}
function folderRow(name,color,open,count,bars=[]){const tint=`color-mix(in srgb, ${color} 16%, var(--dig))`;return `<div style="display:flex;align-items:stretch">${bars.map(c=>`<span style="width:4px;margin:7px 5px;border-radius:2px;background:${c}"></span>`).join('')}<div style="flex:1;display:flex;align-items:center;gap:6px;border-radius:20px;background:${tint};height:52px;padding:0 6px 0 8px"><span style="display:flex">${ic(open?'down':'right',24,'var(--on)')}</span><span style="display:flex">${ic('folder',22,color)}</span><span style="flex:1;font:500 15px GSF">${name}</span>${open?'':`<span style="font:500 12px GSF;color:var(--onv)">${count}</span>`}<span style="color:var(--onv);display:flex;padding:0 6px">${ic('eye',20)}</span><span style="color:var(--onv);display:flex;padding:0 4px">${ic('close',20)}</span><span style="color:var(--onv);display:flex">${ic('drag',22)}</span></div></div>`}
function fabMenu(open,items){
 const pills=open?`<div style="position:absolute;left:12px;bottom:78px;display:flex;flex-direction:column;gap:8px;align-items:flex-start">${items.map(([t,k])=>`<span style="display:flex;align-items:center;gap:12px;height:56px;padding:0 24px 0 18px;border-radius:28px;background:var(--pc);color:var(--onpc);font:500 16px GSF;box-shadow:0 2px 5px rgba(0,0,0,.25)">${ic(k,24)}${t}</span>`).join('')}</div>`:'';
 return pills+`<div style="position:absolute;left:12px;bottom:12px;width:48px;height:48px;border-radius:${open?24:16}px;background:${open?'var(--pri)':'var(--pc)'};color:${open?'var(--onpri)':'var(--onpc)'};display:flex;align-items:center;justify-content:center;box-shadow:0 3px 8px rgba(0,0,0,.3)">${ic(open?'close':'add',24)}</div>`}
// A simple graph panel (SVG) over [x0,x1]×[y0,y1].
function graph(w,h,x0,x1,y0,y1,body,theme){P=PAL[theme];const sx=x=>(x-x0)/(x1-x0)*w, sy=y=>(y1-y)/(y1-y0)*h;
 let g='';for(let x=Math.ceil(x0);x<=x1;x++)g+=`<line x1="${sx(x)}" y1="0" x2="${sx(x)}" y2="${h}" stroke="var(--outv)" stroke-width="1" opacity=".9"/>`;
 for(let y=Math.ceil(y0);y<=y1;y++)g+=`<line x1="0" y1="${sy(y)}" x2="${w}" y2="${sy(y)}" stroke="var(--outv)" stroke-width="1" opacity=".9"/>`;
 g+=`<line x1="0" y1="${sy(0)}" x2="${w}" y2="${sy(0)}" stroke="var(--onv)" stroke-width="2"/><line x1="${sx(0)}" y1="0" x2="${sx(0)}" y2="${h}" stroke="var(--onv)" stroke-width="2"/>`;
 for(let x=Math.ceil(x0);x<=x1;x++)if(x)g+=`<text x="${sx(x)}" y="${sy(0)+15}" font-family="GSF" font-size="11" fill="var(--onv)" text-anchor="middle">${x<0?'−'+(-x):x}</text>`;
 for(let y=Math.ceil(y0);y<=y1;y++)if(y)g+=`<text x="${sx(0)+5}" y="${sy(y)+4}" font-family="GSF" font-size="11" fill="var(--onv)">${y<0?'−'+(-y):y}</text>`;
 return `<svg width="${w}" height="${h}" style="display:block">${g}${body(sx,sy)}</svg>`}
const pathOf=(f,a,b,sx,sy,n=160)=>{let d='';for(let k=0;k<=n;k++){const x=a+(b-a)*k/n;d+=(k?'L':'M')+sx(x).toFixed(1)+' '+sy(f(x)).toFixed(1)}return d};

function phoneList(theme){P=PAL[theme];
 const blue=G.blue, orange='#C2410C';
 const list=`<div style="padding:8px 12px 90px;display:flex;flex-direction:column;gap:6px">
  ${lineRow('y = sin x',G.mint)}
  ${folderRow('Circles',blue,true)}
  ${lineRow('x²+y²=1','#7A5A2C',[blue])}
  ${lineRow('x²+y²=4',G.orange,[blue])}
  ${folderRow('Data',orange,false,3)}
  ${lineRow('y = e<sup style="font-size:13px">−x</sup>',G.green)}
 </div>`;
 const h=st+`<div style="height:300px;background:var(--bg);border-bottom:1px solid var(--outv)">${graph(412,300,-4,4,-2.5,2.5,(sx,sy)=>`<path d="${pathOf(Math.sin,-4,4,sx,sy)}" stroke="${G.mint}" stroke-width="3" fill="none"/><circle cx="${sx(0)}" cy="${sy(0)}" r="${(sx(1)-sx(0))}" stroke="#7A5A2C" stroke-width="3" fill="none"/><circle cx="${sx(0)}" cy="${sy(0)}" r="${(sx(2)-sx(0))}" stroke="${G.orange}" stroke-width="3" fill="none"/><path d="${pathOf(x=>Math.exp(-x),-0.9,4,sx,sy)}" stroke="${G.green}" stroke-width="3" fill="none"/>`,theme)}</div>`+list+
 `<div style="position:absolute;inset:332px 0 0 0;background:rgba(0,0,0,.18)"></div>`+fabMenu(true,[['Table','table'],['Folder','folderNew'],['Note','note'],['Line','fx']]);
 put(h,'phone',theme,'+ opens an M3 FAB menu · folders coloured, lines outside stay outside')}

function phoneFolderDialog(theme){P=PAL[theme];
 const sw=(c,on)=>`<span style="width:40px;height:40px;border-radius:50%;background:${c};display:flex;align-items:center;justify-content:center;${on?'box-shadow:0 0 0 3px var(--on)':''};color:#fff;font-size:18px">${on?'✓':''}</span>`;
 const styles=[['Solid',null],['Dashed','12 9'],['Dotted','0.1 7.5'],['Dash-dot','12 7.5 0.1 7.5'],['Long dash','27 10.5'],['Dash-dot-dot','12 7.5 0.1 7.5 0.1 7.5']];
 const h=st+`<div style="position:absolute;inset:32px 0 0 0;background:rgba(0,0,0,.5);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px">
 <div style="width:360px;border-radius:28px;background:var(--dig);padding:22px;display:flex;flex-direction:column;gap:12px">
  <div style="font:400 24px GSF">Folder</div>
  <div style="border:2px solid var(--pri);border-radius:6px;padding:13px 12px;font:16px GSF;position:relative"><span style="position:absolute;top:-9px;left:10px;background:var(--dig);padding:0 4px;font-size:12px;color:var(--pri)">Name</span>Circles|</div>
  <div style="font:500 14px GSF;color:var(--onv)">Colour</div>
  <div style="display:flex;flex-wrap:wrap;gap:10px">${sw('var(--pri)')}${sw('#7A5A2C')}${sw('#4C8DF6',true)}${sw('#E8710A')}${sw('#1E9E54')}${sw('#165C99')}${sw('#0BB04B')}${sw('#F9950F')}${sw('#ED310C')}${sw('#7C5E8B')}</div>
  <div style="display:flex;gap:8px"><span style="border:1px solid var(--outv);border-radius:99px;padding:9px 14px;font:500 14px GSF;color:var(--pri)">⇥ Into folder above</span></div>
  <div style="text-align:right;font:500 15px GSF;color:var(--pri)"><span style="margin-right:22px">Cancel</span>Done</div>
 </div>
 <div style="width:360px;border-radius:28px;background:var(--dig);padding:18px 22px;display:flex;flex-direction:column;gap:8px">
  <div style="font:500 14px GSF;color:var(--pri)">Line</div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px">${styles.map(([n,d],k)=>`<div style="border-radius:14px;background:${k==3?'var(--sec)':'var(--swb)'};padding:10px 0 8px;display:flex;flex-direction:column;align-items:center;gap:4px"><svg width="80" height="12"><line x1="4" y1="6" x2="76" y2="6" stroke="${G.blue}" stroke-width="3" stroke-linecap="round" ${d?`stroke-dasharray="${d}"`:''}/></svg><span style="font:500 12px GSF;color:${k==3?'var(--onsec)':'var(--onv)'}">${n}</span></div>`).join('')}</div>
 </div></div>`;
 put(h,'phone',theme,'Long-press a folder: name, colour, nesting · six line styles (matplotlib)')}

function phoneArea(theme){P=PAL[theme];
 const f=x=>Math.sin(x)+1, gfn=x=>0.3*x*x;
 const a=-0.62, b=1.62;
 const body=(sx,sy)=>{let top='',bot='';for(let k=0;k<=80;k++){const x=a+(b-a)*k/80;top+=(k?'L':'M')+sx(x)+' '+sy(f(x))}for(let k=80;k>=0;k--){const x=a+(b-a)*k/80;bot+='L'+sx(x)+' '+sy(gfn(x))}
  return `<path d="${top+bot}Z" fill="${G.mint}" opacity=".28"/>`+[a,b].map(x=>`<line x1="${sx(x)}" y1="0" x2="${sx(x)}" y2="600" stroke="${G.mint}" stroke-width="1.3" stroke-dasharray="6 5"/>`).join('')+
  `<path d="${pathOf(f,-3,3,sx,sy)}" stroke="${G.mint}" stroke-width="3" fill="none"/><path d="${pathOf(gfn,-3,3,sx,sy)}" stroke="${G.blue}" stroke-width="3" fill="none"/>`+
  [a,b].map(x=>`<circle cx="${sx(x)}" cy="${sy(f(x))}" r="8" fill="var(--bg)" stroke="${G.mint}" stroke-width="2.5"/>`).join('')};
 const legend=`<div style="position:absolute;left:10px;top:44px;display:flex;flex-direction:column;gap:8px">
  <div style="border-radius:14px;background:color-mix(in srgb, var(--bg) 82%, transparent);padding:6px 10px;display:flex;flex-direction:column;gap:3px;font:15px CMI">
   <div style="display:flex;align-items:center;gap:8px"><svg width="24" height="10"><line x1="1" y1="5" x2="23" y2="5" stroke="${G.mint}" stroke-width="2.5"/></svg>y = sin x + 1</div>
   <div style="display:flex;align-items:center;gap:8px"><svg width="24" height="10"><line x1="1" y1="5" x2="23" y2="5" stroke="${G.blue}" stroke-width="2.5" stroke-dasharray="7.5 5"/></svg>y = 0.3x²</div></div>
  <div style="border-radius:20px;background:var(--dig);padding:10px 8px 10px 16px;display:flex;align-items:center;gap:6px"><div><div style="font:17px CMR;display:flex;align-items:center;gap:3px;padding:6px 0"><span style="font:11px CMS2;line-height:1">∫</span><span style="display:inline-flex;flex-direction:column;font:11px CMR;line-height:1.5;margin-left:-1px;margin-top:-2px"><span>1.62</span><span>−0.62</span></span><span style="font-family:CMI">f</span>(<i style="font-family:CMI;font-style:normal">x</i>) − <span style="font-family:CMI">g</span>(<i style="font-family:CMI;font-style:normal">x</i>) d<i style="font-family:CMI;font-style:normal">x</i></div><div style="font:13px GSF;color:var(--onv)">area between the curves ≈ 2.314</div></div><span style="color:var(--onv);display:flex">${ic('close',20)}</span></div>
 </div>`;
 const bubble=`<div style="position:absolute;left:34px;top:452px;border-radius:99px;background:var(--inv);color:var(--oninv);display:flex;align-items:center;gap:6px;padding:4px 4px 4px 14px;font:13px GSF">intersection (1.62, 2.00)<span style="width:32px;height:32px;border-radius:50%;background:var(--pc);display:flex;align-items:center;justify-content:center;color:var(--inv)">◔</span><span style="width:32px;height:32px;border-radius:50%;background:var(--pri);display:flex;align-items:center;justify-content:center"><svg width="20" height="20" viewBox="0 0 24 24"><path d="M3 18C8 18 10 6 21 5M3 8c6 0 9 11 18 12" stroke="var(--inv)" stroke-width="2" fill="none"/><path d="M12 12.3C14.6 8.5 17 6.2 21 5v15c-4-.4-6.6-3.3-9-7.7z" fill="var(--inv)" opacity=".5"/></svg></span></div>`;
 const h=st+`<div style="position:relative;height:640px">${graph(412,640,-3,3,-1.5,4.8,body,theme)}${legend}${bubble}</div>`+
 `<div style="position:absolute;left:0;right:0;bottom:24px;text-align:center;font:13px GSF;color:var(--onv)">Drag the ○ edges; the value follows. Sliders move it too.</div>`;
 put(h,'phone',theme,'Area between curves (from an intersection) · card under the legend')}

function phoneField(theme){P=PAL[theme];
 const W=412,H=560;
 const vir=[[68,1,84],[72,40,120],[62,74,137],[49,104,142],[38,130,142],[31,158,137],[53,183,121],[109,205,89],[180,222,44],[253,231,37]];
 const col=t=>{t=Math.max(0,Math.min(1,t))*9;const i=Math.min(8,Math.floor(t)),u=t-i;return vir[i].map((c,k)=>Math.round(c+(vir[i+1][k]-c)*u))};
 const cvs=document.createElement('canvas');cvs.width=W/3;cvs.height=H/3;const ctx=cvs.getContext('2d');const img=ctx.createImageData(cvs.width,cvs.height);
 for(let j=0;j<cvs.height;j++)for(let i=0;i<cvs.width;i++){const x=-3+6*(i+.5)/cvs.width,y=4.08-8.16*(j+.5)/cvs.height;const v=x*x-y*y;const [r,g,b]=col((v+7)/14);const k=4*(j*cvs.width+i);img.data[k]=r;img.data[k+1]=g;img.data[k+2]=b;img.data[k+3]=240}
 ctx.putImageData(img,0,0);const url=cvs.toDataURL();
 const body=(sx,sy)=>`<image href="${url}" x="0" y="0" width="${W}" height="${H}" preserveAspectRatio="none" style="image-rendering:auto"/>`;
 const g=graph(W,H,-3,3,-4.08,4.08,body,theme).replace('<svg ','<svg ');
 // put grid over image: rebuild so image is below grid
 const svgWithImage=graph(W,H,-3,3,-4.08,4.08,()=>'',theme).replace(/(<svg[^>]*>)/,`$1<image href="${url}" x="0" y="0" width="${W}" height="${H}" preserveAspectRatio="none"/>`);
 const legend=`<div style="position:absolute;left:10px;top:44px;border-radius:14px;background:color-mix(in srgb, var(--bg) 82%, transparent);padding:6px 10px;display:flex;align-items:center;gap:8px;font:15px CMI"><span style="width:24px;height:8px;background:linear-gradient(90deg,rgb(68,1,84),rgb(49,104,142),rgb(31,158,137),rgb(109,205,89),rgb(253,231,37))"></span>x² − y²</div>`;
 const bubble=`<div style="position:absolute;left:170px;top:190px;border-radius:99px;background:var(--inv);color:var(--oninv);padding:6px 14px;font:13px GSF">value 2.61  (1.90, 0.95)</div><div style="position:absolute;left:${(1.9+3)/6*W-7}px;top:${(4.08-0.95)/8.16*H+32-7}px;width:14px;height:14px;border-radius:50%;background:#fff;border:3px solid #222"></div>`;
 const row=`<div style="padding:10px 12px"><div style="display:flex;align-items:center;gap:8px;border-radius:20px;background:var(--dig);height:56px;padding:0 10px"><span style="width:26px;height:26px;border-radius:50%;background:conic-gradient(rgb(68,1,84),rgb(49,104,142),rgb(31,158,137),rgb(109,205,89),rgb(253,231,37),rgb(68,1,84));box-shadow:inset 0 0 0 7px var(--dig)"></span><span style="font:21px CMI">x² − y²</span></div></div>`;
 put(st+`<div style="position:relative">${svgWithImage}</div>${legend}${bubble}${row}`,'phone',theme,'2D scalar field f(x, y) with a colormap (long-press its dot to pick one)')}

function tablet(theme){P=PAL[theme];
 const W=1280,H=800;
 const keypad=`<div style="width:340px;background:var(--card);padding:16px;display:grid;grid-template-columns:repeat(4,1fr);gap:8px;align-content:end">${'7 8 9 ÷ 4 5 6 × 1 2 3 − 0 . = +'.split(' ').map(k=>`<span style="height:62px;border-radius:99px;background:${/\d|\./.test(k)?'var(--dig)':'var(--sec)'};display:flex;align-items:center;justify-content:center;font:26px GSF">${k}</span>`).join('')}</div>`;
 const list=`<div style="width:360px;padding:14px 10px;display:flex;flex-direction:column;gap:6px;background:var(--bg)">${lineRow('f(z) = Γ<span style="font-family:CMR">′</span>(z)','conic-gradient(red,yellow,lime,cyan,blue,magenta,red)')}${lineRow('∫<sub style="font:11px CMR">1</sub><sup style="font:11px CMR;margin-left:-6px">z</sup> Γ(t) dt','conic-gradient(#440154,#31688e,#35b779,#fde725,#440154)')}</div>`;
 const handle=`<div style="width:16px;display:flex;align-items:center;justify-content:center;background:var(--bg)"><span style="width:6px;height:64px;border-radius:3px;background:var(--pri)"></span></div>`;
 // complex plane: hue by arg of Γ′ approximated by a simple function for the mockup
 const cw=560,ch=H-40;const cvs=document.createElement('canvas');cvs.width=cw/4;cvs.height=ch/4;const ctx=cvs.getContext('2d');const img=ctx.createImageData(cvs.width,cvs.height);
 for(let j=0;j<cvs.height;j++)for(let i=0;i<cvs.width;i++){const x=-4+8*(i+.5)/cvs.width,y=5.4-10.8*(j+.5)/cvs.height;
  // (z^2+1)/(z-1.5) as a stand-in phase portrait
  const nr=x*x-y*y+1,ni=2*x*y,dr=x-1.5,di=y;const d=dr*dr+di*di;const re=(nr*dr+ni*di)/d,im=(ni*dr-nr*di)/d;const h=(Math.atan2(im,re)/(2*Math.PI)+1)%1;const m=Math.hypot(re,im);const l=0.5+Math.atan(Math.log(m)/1.5)/Math.PI*0.8;
  const c=(n)=>{const k=(n+h*12)%12;return l-l*Math.min(1-l,l)*0+ -Math.min(l,1-l)*Math.max(-1,Math.min(k-3,9-k,1))};const kk=4*(j*cvs.width+i);img.data[kk]=c(0)*255;img.data[kk+1]=c(8)*255;img.data[kk+2]=c(4)*255;img.data[kk+3]=255}
 ctx.putImageData(img,0,0);const url=cvs.toDataURL();
 const plane=graph(cw,ch,-4,4,-5.4,5.4,()=>'',theme).replace(/(<svg[^>]*>)/,`$1<image href="${url}" x="0" y="0" width="${cw}" height="${ch}" preserveAspectRatio="none"/>`).replace(/fill="var\(--onv\)"( text-anchor="middle")?>/g,(m)=>m.replace('fill="var(--onv)"','fill="var(--onv)" style="paint-order:stroke;stroke:var(--bg);stroke-width:3px"'));
 const labelsI=plane.replace(/(<text x="[^"]*" y="[^"]*" font-family="GSF" font-size="11" fill="var\(--onv\)" style="[^"]*">)(−?\d)(<\/text>)/g,(m,a,b,c)=>m);
 const h=`<div style="height:24px"></div><div style="display:flex;height:${H-24}px">${keypad}${list}${handle}<div style="flex:1;position:relative;overflow:hidden">${labelsI}
  <div style="position:absolute;right:16px;bottom:16px;display:flex;gap:6px;background:var(--dig);border-radius:99px;padding:6px">${['Cartesian','Cylindrical','Spherical'].map((n,k)=>`<span style="width:40px;height:40px;border-radius:50%;background:${k==0?'var(--pri)':'transparent'};color:${k==0?'var(--onpri)':'var(--on)'};display:flex;align-items:center;justify-content:center;font:11px GSF">${['xyz','r θ z','ρ θ φ'][k]}</span>`).join('')}<span style="font:11px GSF;color:var(--onv);align-self:center;padding-right:8px">3D chips</span></div>
  <div style="position:absolute;left:200px;top:260px;border-radius:99px;background:var(--inv);color:var(--oninv);padding:6px 14px;font:13px GSF">hold & drag · f(1.2 + 0.8i) = 0.41 − 1.37i</div>
 </div></div>`;
 put(h,'tablet',theme,'Tablet: drag the pill to resize the list · complex plane in the theme’s grid colours · hold-and-drag readout')}

function exportLegend(){
 const row=(math)=>`<div style="display:flex;align-items:center;gap:14px;font:24px CMR">${math}</div>`;
 const int=(lo,hi,loop)=>`<span style="position:relative;display:inline-flex;align-items:center"><span style="font:15px CMS2;line-height:1;display:inline-block;height:34px;padding-top:7px">${loop?'∮':'∫'}</span>${hi?`<span style="position:absolute;left:15px;top:-9px;font:16px CMR">${hi}</span>`:''}${lo?`<span style="position:absolute;left:10px;bottom:-11px;font:16px CMR;white-space:nowrap">${lo}</span>`:''}</span>`;
 const h=`<div style="background:#fff;padding:26px 30px;display:flex;flex-direction:column;gap:22px;border-radius:6px;box-shadow:0 10px 30px rgba(0,0,0,.2);width:520px">
  ${row(`<svg width="40" height="10"><line x1="1" y1="5" x2="39" y2="5" stroke="#165c99" stroke-width="3"/></svg>${int('0','<i style="font-family:CMI;font-style:normal">x</i>')}<span style="margin-left:${22}px"><i style="font-family:CMI;font-style:normal">e</i><sup style="font-size:15px">−<i style="font-family:CMI;font-style:normal">t</i><sup style="font-size:11px">2</sup></sup> d<i style="font-family:CMI;font-style:normal">t</i></span>`)}
  ${row(`<svg width="40" height="10"><line x1="1" y1="5" x2="39" y2="5" stroke="#0bb04b" stroke-width="3" stroke-dasharray="12 7.5 0.1 7.5" stroke-linecap="round"/></svg>${int('|<i style="font-family:CMI;font-style:normal">z</i>|=1','',true)}<span style="margin-left:${40}px"><span style="display:inline-flex;flex-direction:column;align-items:center;font-size:20px;vertical-align:middle"><i style="font-family:CMI;font-style:normal">e</i><sup></sup><span style="border-top:1.5px solid #000;padding:0 4px"><i style="font-family:CMI;font-style:normal">z</i><sup style="font-size:13px">3</sup></span></span> d<i style="font-family:CMI;font-style:normal">z</i></span>`)}
  ${row(`<svg width="40" height="10"><line x1="1" y1="5" x2="39" y2="5" stroke="#f9950f" stroke-width="3" stroke-dasharray="0.1 7.5" stroke-linecap="round"/></svg><span>dotted</span><svg width="40" height="10"><line x1="1" y1="5" x2="39" y2="5" stroke="#ed310c" stroke-width="3" stroke-dasharray="12 9" stroke-linecap="round"/></svg><span>dashed</span>`)}
 </div>`;
 const w=document.createElement('div');w.innerHTML=cap('Export legend: TeX’s own ∫ and ∮ (Size2) with limits · dotted ≠ dashed')+h;document.getElementById('sheet').appendChild(w)}

Promise.all(['GSF','CMR','CMI','CMS2'].map(f=>document.fonts.load('400 20px '+f,f=='CMS2'?'∫∮':'a'))).then(()=>{
 phoneList('light');phoneFolderDialog('dark');phoneArea('light');phoneField('dark');
 const br=document.createElement('div');br.style.cssText='width:100%;height:0';document.getElementById('sheet').appendChild(br);
 tablet('dark');exportLegend();
 document.title='ready'});
