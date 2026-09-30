// ---- Round of 30 September (2): the colour picker (ColorPickerDialog), the colormap picker
// (ColormapPickerDialog), the point shapes (Markers.kt), 2 sin x spacing, the LaTeX legend in exports.
Object.assign(ICON,{
 Star:'M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z',
 StarO:'m22 9.24-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z',
 Search:'M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
});
const THEME={light:['#5B6133','#5E6044','#3B665C','#BA1A1A','#313129'],dark:['#C5CB86','#C7C8A9','#A2D0C3','#FFB4AB','#E5E3D6']};
const STD=['#E53935','#FB8C00','#FDD835','#43A047','#00897B','#1E88E5','#8E24AA','#D81B60','#B71C1C','#E65100','#F9A825','#1B5E20','#004D40','#0D47A1','#4A148C','#212121'];
const LABELS={twilight_shifted:'Dusk',gist_yarg:'Yarg',gist_gray:'Graphite',gist_heat:'Heat',gist_earth:'Earth',gist_stern:'Stern',gist_rainbow:'Spectrum',gist_ncar:'Ncar',nipy_spectral:'Nipy',CMRmap:'CMRmap'};
const cmLabel=n=>LABELS[n]||n[0].toUpperCase()+n.slice(1);
const grad=(n,rev)=>{const s=CMAPS[n].s.slice();if(rev)s.reverse();return `linear-gradient(90deg,${s.join(',')})`};
const cap=t=>`<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`;
function put(html,cls,theme,capt,style=''){const w=document.createElement('div');w.innerHTML=cap(capt)+`<div class="${cls} ${theme}" style="position:relative;${style}">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const st=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
const head=t=>`<div style="font:500 14px GSF;color:var(--pri);margin:14px 0 8px">${t}</div>`;
function swatches(theme,picked,size){
 const sw=(c)=>`<span style="flex:none;width:${size}px;height:${size}px;border-radius:50%;background:${c};${c==picked?'box-shadow:0 0 0 3px var(--dig),0 0 0 5px var(--on)':'box-shadow:inset 0 0 0 1px rgba(128,128,128,.35)'}"></span>`;
 return head('Theme')+`<div style="display:flex;flex-wrap:wrap;gap:8px">${THEME[theme].map(sw).join('')}</div>`+head('Standard')+`<div style="display:grid;grid-template-columns:repeat(8,${size}px);gap:8px">${STD.map(sw).join('')}</div>`}
function svSquare(hue,s,v,h){return head('Any color')+`<div style="position:relative;height:${h}px;border-radius:16px;background:linear-gradient(to top,#000,transparent),linear-gradient(to right,#fff,hsl(${hue},100%,50%))"><span style="position:absolute;left:calc(${s}% - 11px);top:calc(${100-v}% - 11px);width:16px;height:16px;border-radius:50%;border:3px solid #fff;box-shadow:0 0 0 1px rgba(0,0,0,.4)"></span></div>
 <div style="font:500 12px GSF;color:var(--onv);margin-top:10px">Hue</div><div style="position:relative;height:44px;display:flex;align-items:center"><span style="position:absolute;left:0;right:0;height:16px;border-radius:8px;background:linear-gradient(90deg,#f00,#ff0,#0f0,#0ff,#00f,#f0f,#f00)"></span><span style="position:absolute;left:${hue/3.6}%;width:4px;height:44px;border-radius:2px;background:var(--on)"></span></div>`}
function lineSection(color){const s=(n,dash,on)=>`<div style="flex:1;border-radius:14px;padding:10px 14px;display:flex;flex-direction:column;align-items:center;gap:6px;background:${on?'var(--sec)':'var(--swb)'}"><svg width="100%" height="10"><line x1="4" y1="5" x2="96%" y2="5" stroke="${color}" stroke-width="3" stroke-linecap="round" stroke-dasharray="${dash}"/></svg><span style="font:500 12px GSF;color:${on?'var(--onsec)':'var(--onv)'}">${n}</span></div>`;
 return head('Line')+`<div style="display:flex;gap:8px">${s('Solid','',true)}${s('Dashed','12 9',false)}${s('Dotted','0.1 7.5',false)}</div><div style="font:500 12px GSF;color:var(--onv);margin-top:10px">Thickness: 3.0 dp</div>${slider2(29)}`}
function slider2(p){return `<div style="position:relative;height:44px;display:flex;align-items:center"><span style="position:absolute;left:calc(${p}% + 10px);right:0;height:16px;border-radius:4px 8px 8px 4px;background:var(--sec)"></span><span style="position:absolute;left:0;width:calc(${p}% - 4px);height:16px;border-radius:8px 4px 4px 8px;background:var(--pri)"></span><span style="position:absolute;left:calc(${p}% + 1px);width:4px;height:44px;border-radius:2px;background:var(--pri)"></span></div>`}
function exactSection(){return head('Exact values')+`<div class="seg" style="height:40px">${['HSV','RGB','OKLab','Hex'].map((m,i)=>`<span class="${i==0?'sel':''}" style="flex:1;justify-content:center">${m}</span>`).join('')}</div>
 <div style="display:flex;gap:8px;margin-top:10px">${[['H (°)','211'],['S (%)','86'],['V (%)','90']].map(([l,v])=>`<div style="flex:1;position:relative;border:1px solid var(--outv);border-radius:6px;padding:14px 12px;font:16px GSF">${v}<span style="position:absolute;top:-9px;left:10px;padding:0 4px;background:var(--dig);font:12px GSF;color:var(--onv)">${l}</span></div>`).join('')}</div>`}
// The marks a point can take.
const MARKS=['circle','ring','square','diamond','triangle','cross','plus','star','pentagon','hexagon','triangleDown','asterisk','openSquare','openDiamond','openTriangle'];
function markSvg(m,c,r=8){const cx=11,cy=11;const P=(pts)=>pts.map((p,i)=>(i?'L':'M')+p.join(' ')).join('')+'Z';
 const poly=(n,rr,a0)=>Array.from({length:n},(_,k)=>[cx+rr*Math.cos(a0+k*2*Math.PI/n),cy+rr*Math.sin(a0+k*2*Math.PI/n)]);
 const sq=rr=>[[cx-rr*.85,cy-rr*.85],[cx+rr*.85,cy-rr*.85],[cx+rr*.85,cy+rr*.85],[cx-rr*.85,cy+rr*.85]];
 const di=rr=>[[cx,cy-rr*1.15],[cx+rr*1.15,cy],[cx,cy+rr*1.15],[cx-rr*1.15,cy]];
 const tr=rr=>[[cx,cy-rr*1.2],[cx+rr*1.1,cy+rr*.75],[cx-rr*1.1,cy+rr*.75]];
 const line=(a,b)=>`<line x1="${a[0]}" y1="${a[1]}" x2="${b[0]}" y2="${b[1]}" stroke="${c}" stroke-width="${r/2.5}" stroke-linecap="round"/>`;
 const hollow=f=>`<path d="${P(f(r))}${P(f(r*.55).reverse())}" fill="${c}" fill-rule="nonzero"/>`;
 let o='';switch(m){
  case 'circle':o=`<circle cx="${cx}" cy="${cy}" r="${r}" fill="${c}"/>`;break;
  case 'ring':o=`<circle cx="${cx}" cy="${cy}" r="${r*.78}" fill="none" stroke="${c}" stroke-width="${r*.45}"/>`;break;
  case 'square':o=`<path d="${P(sq(r))}" fill="${c}"/>`;break;
  case 'diamond':o=`<path d="${P(di(r))}" fill="${c}"/>`;break;
  case 'triangle':o=`<path d="${P(tr(r))}" fill="${c}"/>`;break;
  case 'cross':o=line([cx-r,cy-r],[cx+r,cy+r])+line([cx-r,cy+r],[cx+r,cy-r]);break;
  case 'plus':o=line([cx-r*1.15,cy],[cx+r*1.15,cy])+line([cx,cy-r*1.15],[cx,cy+r*1.15]);break;
  case 'asterisk':o=[0,1,2].map(k=>{const a=Math.PI/2+k*Math.PI/3;return line([cx-r*1.1*Math.cos(a),cy-r*1.1*Math.sin(a)],[cx+r*1.1*Math.cos(a),cy+r*1.1*Math.sin(a)])}).join('');break;
  case 'star':o=`<path d="${P(Array.from({length:10},(_,k)=>{const a=-Math.PI/2+k*Math.PI/5,rr=k%2?r*.55:r*1.3;return [cx+rr*Math.cos(a),cy+rr*Math.sin(a)]}))}" fill="${c}"/>`;break;
  case 'pentagon':o=`<path d="${P(poly(5,r*1.1,-Math.PI/2))}" fill="${c}"/>`;break;
  case 'hexagon':o=`<path d="${P(poly(6,r*1.05,0))}" fill="${c}"/>`;break;
  case 'triangleDown':o=`<path d="${P([[cx,cy+r*1.2],[cx-r*1.1,cy-r*.75],[cx+r*1.1,cy-r*.75]])}" fill="${c}"/>`;break;
  case 'openSquare':o=hollow(sq);break;case 'openDiamond':o=hollow(di);break;case 'openTriangle':o=hollow(tr);break}
 return `<svg width="22" height="22">${o}</svg>`}
function pointSection(sel){return head('Point')+`<div style="display:flex;flex-wrap:wrap;gap:6px">${MARKS.map((m,i)=>`<span style="width:38px;height:38px;border-radius:10px;display:flex;align-items:center;justify-content:center;background:${i==sel?'var(--sec)':'var(--swb)'}">${markSvg(m,i==sel?'var(--onsec)':'var(--onv)')}</span>`).join('')}</div>
 <div style="font:500 12px GSF;color:var(--onv);margin-top:10px">Size: 6 dp</div>${slider2(28)}`}
function pickerHeader(picked){return `<div style="display:flex;align-items:center;padding:0 24px"><span style="flex:1;font:400 24px GSF">Color</span><span style="display:flex;align-items:center;gap:8px;border-radius:99px;background:var(--swb);padding:6px 14px 6px 6px"><span style="width:28px;height:28px;border-radius:50%;background:${picked}"></span><span style="font:14px monospace">${picked}</span></span></div>`}
const buttons=`<div style="display:flex;padding:8px 24px 0;font:500 15px GSF;color:var(--pri)"><span>Default</span><span style="flex:1"></span><span style="margin-right:26px">Cancel</span><span>Done</span></div>`;
function phoneColor(theme){P=PAL[theme];const picked='#1E88E5';
 const h=st+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:388px;height:830px;overflow:hidden;border-radius:28px;background:var(--dig);padding:20px 0 12px;display:flex;flex-direction:column">
  ${pickerHeader(picked)}<div style="flex:1;overflow:hidden;padding:0 24px">${swatches(theme,picked,34)}${svSquare(211,86,90,170)}${lineSection(picked)}</div>${buttons}</div></div>`;
 put(h,'phone',theme,'Colour picker · standard colours first')}
function phoneColorPoint(theme){P=PAL[theme];const picked=THEME[theme][2];
 const h=st+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:388px;height:830px;overflow:hidden;border-radius:28px;background:var(--dig);padding:20px 0 12px;display:flex;flex-direction:column">
  ${pickerHeader(picked)}<div style="flex:1;overflow:hidden;padding:0 24px"><div style="margin-top:-160px">${swatches(theme,picked,34)}${svSquare(170,40,40,170)}${lineSection(picked)}${pointSection(7)}</div></div>${buttons}</div></div>`;
 put(h,'phone',theme,'…scrolled: 15 point shapes')}
function tabletColor(theme){P=PAL[theme];const picked='#8E24AA';
 const h=st+`<div style="flex:1;background:var(--bg)"></div><div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:920px;border-radius:28px;background:var(--dig);padding:20px 0 12px">
  ${pickerHeader(picked)}<div style="display:flex;gap:28px;padding:0 24px"><div style="flex:1">${swatches(theme,picked,40)}${svSquare(291,80,67,220)}</div><div style="flex:1">${exactSection()}${lineSection(picked)}</div></div>${buttons}</div></div>`;
 put(h,'tablet',theme,'Tablet · colour picker in two columns')}
function cmCard(n,{chosen,rev,star}={}){return `<div style="flex:1;min-width:0;border-radius:16px;padding:8px;background:${chosen?'var(--sec)':'var(--swb)'};${chosen?'box-shadow:inset 0 0 0 2px var(--pri)':''}"><div style="height:26px;border-radius:8px;background:${grad(n,rev)}"></div><div style="display:flex;align-items:center;margin-top:4px"><span style="flex:1;font:500 14px GSF;padding-left:2px;white-space:nowrap;overflow:hidden">${cmLabel(n)}</span>${star===undefined?'':`<span style="color:${star?'var(--pri)':'var(--onv)'};display:flex">${icon(star?'Star':'StarO',22)}</span>`}</div></div>`}
function cmGrid(names,per,opt){let o='';for(let i=0;i<names.length;i+=per){const r=names.slice(i,i+per);o+=`<div style="display:flex;gap:8px;margin-bottom:8px">${r.map(n=>cmCard(n,opt(n))).join('')}${'<div style="flex:1"></div>'.repeat(per-r.length)}</div>`}return o}
const FAV=['classic','twilight','twilight_shifted','viridis','plasma','magma','cividis','turbo'];
function cmChosen(n,rev,h){return `<div style="height:${h}px;border-radius:18px;background:${grad(n,rev)};display:flex;align-items:center;padding-left:14px"><span style="font:500 16px GSF;color:#fff;background:rgba(0,0,0,.35);border-radius:10px;padding:4px 10px">${cmLabel(n)}${rev?' (reversed)':''}</span></div>
 <div style="display:flex;align-items:center;gap:12px;margin:10px 0"><div style="flex:1"><div style="font:17px GSF">Reversed</div><div style="font:13px GSF;color:var(--onv)">Run the colors the other way round</div></div>${`<span style="flex:none;width:52px;height:32px;border-radius:16px;display:flex;align-items:center;${rev?'justify-content:flex-end;padding-right:4px;background:var(--pri)':'padding-left:6px;border:2px solid var(--outv)'}"><span style="width:${rev?24:16}px;height:${rev?24:16}px;border-radius:50%;background:${rev?'var(--onpri)':'var(--outv)'}"></span></span>`}</div>`}
const yoursHead=`<div style="display:flex;align-items:center;margin:6px 0 8px"><span style="flex:1;font:500 15px GSF;color:var(--pri)">Your colormaps</span><span style="font:500 14px GSF;color:var(--pri)">Edit</span></div>`;
function browse(per,kind,names){return `<div style="font:500 15px GSF;color:var(--pri);margin:10px 0 8px">All colormaps</div>
 <div style="display:flex;align-items:center;gap:10px;border:1px solid var(--outv);border-radius:6px;padding:12px;color:var(--onv);font:16px GSF">${icon('Search',22)}Search by name</div>
 <div style="display:flex;flex-wrap:wrap;gap:6px;margin:10px 0">${['All','Perceptually uniform','Sequential','Sequential (2)','Diverging','Cyclic','Qualitative','Miscellaneous'].map(c=>`<span style="border-radius:8px;padding:6px 12px;font:500 13px GSF;${c==kind?'background:var(--sec);color:var(--onsec)':'border:1px solid var(--outv);color:var(--onv)'}">${c==kind?'✓ ':''}${c}</span>`).join('')}</div>`+cmGrid(names,per,n=>({star:FAV.includes(n),chosen:n=='viridis'}))}
function phoneColormap(theme){P=PAL[theme];
 const div=Object.keys(CMAPS).filter(n=>CMAPS[n].c=='Diverging').slice(0,8);
 const h=st+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:388px;height:830px;overflow:hidden;border-radius:28px;background:var(--dig);padding:20px 0 12px;display:flex;flex-direction:column">
  <div style="font:400 24px GSF;padding:0 24px 12px">Colormap</div><div style="flex:1;overflow:hidden;padding:0 24px">${cmChosen('viridis',false,52)}${yoursHead}${cmGrid(FAV,2,n=>({chosen:n=='viridis'}))}${browse(2,'Diverging',div)}</div><div style="display:flex;justify-content:flex-end;padding:8px 24px 0;font:500 15px GSF;color:var(--pri)">Done</div></div></div>`;
 put(h,'phone',theme,'Colormap · chosen, yours, then browse')}
function tabletColormap(theme){P=PAL[theme];
 const pu=Object.keys(CMAPS).filter(n=>['Perceptually uniform','Cyclic'].includes(CMAPS[n].c)&&n!='classic').concat(['coolwarm','RdBu','Spectral','PiYG','cubehelix','CMRmap']);
 const h=st+`<div style="flex:1;background:var(--bg)"></div><div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:1000px;height:720px;overflow:hidden;border-radius:28px;background:var(--dig);padding:20px 0 12px;display:flex;flex-direction:column">
  <div style="font:400 24px GSF;padding:0 24px 12px">Colormap</div><div style="flex:1;overflow:hidden;display:flex;gap:28px;padding:0 24px"><div style="flex:.9">${cmChosen('magma',true,64)}${yoursHead}${cmGrid(FAV,3,n=>({chosen:n=='magma',rev:n=='magma'}))}</div><div style="flex:1.1">${browse(3,'All',pu)}</div></div><div style="display:flex;justify-content:flex-end;padding:8px 24px 0;font:500 15px GSF;color:var(--pri)">Done</div></div></div>`;
 put(h,'tablet',theme,'Tablet · colormap: yours on the left, all maps on the right')}
function spacingStrip(){const w=document.createElement('div');P=PAL.light;
 const thin={t:'sym',s:' '};
 w.innerHTML=cap('Calculator · thin space before functions, as LaTeX')+`<div class="light" style="width:412px;border-radius:24px;background:var(--bg);padding:18px 22px;display:flex;flex-direction:column;gap:10px;box-shadow:0 10px 30px rgba(0,0,0,.2)">
  <div style="font:500 13px GSF;color:var(--onv)">Before</div><div style="text-align:right">${mv(R('2',func('sin',R('x')),S('+'),'3',func('log',R('a'),R('x'))),30,P.on)}</div>
  <div style="font:500 13px GSF;color:var(--onv)">Now</div><div style="text-align:right">${mv(R('2',thin,func('sin',R('x')),S('+'),'3',thin,func('log',R('a'),R('x'))),30,P.on)}</div></div>`;
 document.getElementById('sheet').appendChild(w)}
function exportShots(){const w=document.createElement('div');w.innerHTML=cap('Export · legend set in LaTeX, new colours')+`<img src="export_legend.png" style="width:412px;height:412px;border-radius:8px;box-shadow:0 10px 30px rgba(0,0,0,.25)">`;document.getElementById('sheet').appendChild(w)}
Promise.all(['GSF','Rob','CMR','CMI'].map(f=>document.fonts.load('400 20px '+f))).then(()=>{
 tabletColor('dark');tabletColormap('light');
 const br=document.createElement('div');br.style.cssText='width:100%;height:0';document.getElementById('sheet').appendChild(br);
 phoneColor('light');phoneColorPoint('dark');phoneColormap('dark');exportShots();spacingStrip();
 document.title='ready'});
