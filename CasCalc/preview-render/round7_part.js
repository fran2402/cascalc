// ---- Round of 30 September: tablet settings and acknowledgements (SectionedPage in Pages.kt),
// the data table editor (PointTableDialog in GraphCommon.kt), renaming a line and the legends.
Object.assign(ICON,{
 Palette:'M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z',
 Hist:'M13 3a9 9 0 0 0-9 9H1l3.9 3.9L8.8 12H6a7 7 0 1 1 2.05 4.95l-1.42 1.42A9 9 0 1 0 13 3zm-1 5v5l4.25 2.52.77-1.28-3.52-2.09V8z',
 Pin:'M20 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4V6h16v12zM7 9h2v6H7zm4 0h2v6h-2zm4 0h2v6h-2z',
 Touch:'M9 11.24V7.5C9 6.12 10.12 5 11.5 5S14 6.12 14 7.5v3.74c1.21-.81 2-2.18 2-3.74C16 5.01 13.99 3 11.5 3S7 5.01 7 7.5c0 1.56.79 2.93 2 3.74zm9.84 4.63-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6c0-.83-.67-1.5-1.5-1.5S10 6.67 10 7.5v10.74l-3.43-.72c-.08-.01-.15-.03-.24-.03-.31 0-.59.13-.79.33l-.79.8 4.94 4.94c.27.27.65.44 1.06.44h6.79c.75 0 1.33-.55 1.44-1.28l.75-5.27c.01-.07.02-.14.02-.2 0-.62-.38-1.16-.91-1.38z',
 Info:'M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z',
 Table:'M10 10.02h5V21h-5zM17 21h3c1.1 0 2-.9 2-2v-9h-5v11zm3-18H5c-1.1 0-2 .9-2 2v3h19V5c0-1.1-.9-2-2-2zM3 19c0 1.1.9 2 2 2h3V10H3v9z',
 Drop:'M7 10l5 5 5-5z',
 Font:'M9.93 13.5h4.14L12 7.98zM20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-4.05 16.5-1.14-3H9.17l-1.12 3H5.96l5.11-13h1.86l5.11 13h-2.09z',
 Books:'M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z',
 Dataset:'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-8 14H7v-4h4v4zm0-6H7V7h4v4zm6 6h-4v-4h4v4zm0-6h-4V7h4v4z',
 Fx:'M18 4H6v2l6.5 6L6 18v2h12v-3h-7l5-5-5-5h7z',
 Bulb:'M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z',
 Drag:'M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
 Back:'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
 Upload:'M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11z',
 Ext:'M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z',
});
// Lines' default colours now come from the theme (TonalScheme.graphColors for the olive theme).
PLOT.light=['#5D6500','#046F5C','#664AA4','#973562','#006690','#904800'];PLOT.dark=['#C4D168','#84DAC3','#CBBAFF','#FFA9CA','#82D1FF','#FFB37E'];
const RW={x:'var(--pri)',y:'var(--tert)',s:'var(--sec)',n:'var(--swb)'}, RF={x:'var(--onpri)',y:'var(--ontert)',s:'var(--onsec)',n:'var(--onv)'};
const sw=on=>`<span style="flex:none;width:52px;height:32px;border-radius:16px;display:flex;align-items:center;${on?'justify-content:flex-end;padding-right:4px;background:var(--pri)':'padding-left:6px;border:2px solid var(--outv)'}"><span style="width:${on?24:16}px;height:${on?24:16}px;border-radius:50%;background:${on?'var(--onpri)':'var(--outv)'}"></span></span>`;
const tog=(t,d,on)=>`<div style="display:flex;align-items:center;gap:16px"><div style="flex:1"><div style="font:17px GSF">${t}</div>${d?`<div style="font:14px GSF;color:var(--onv)">${d}</div>`:''}</div>${sw(on)}</div>`;
const segc=(t,opts,sel)=>`<div><div style="font:17px GSF;margin-bottom:8px">${t}</div><div style="display:flex;border:1px solid var(--outv);border-radius:99px;overflow:hidden;font:500 14px GSF">${opts.map((m,k)=>`<span style="flex:1;text-align:center;padding:9px 0;${k?'border-left:1px solid var(--outv);':''}${sel==k?'background:var(--sec);color:var(--onsec)':''}">${m}</span>`).join('')}</div></div>`;
function caption(t){return `<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`}
function frame(html,cls,theme,capt){const w=document.createElement('div');w.innerHTML=caption(capt)+`<div class="${cls} ${theme}" style="position:relative">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const status=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
// Tablet: the sections down the left, one shown on the right in a card.
function sectioned(theme,title,sections,chosen,body){
 P=PAL[theme];
 const nav=sections.map(([t,ic],i)=>`<div style="height:56px;border-radius:28px;display:flex;align-items:center;gap:14px;padding:0 18px;${i==chosen?'background:var(--sec);color:var(--onsec)':'color:var(--on)'}"><span style="color:${i==chosen?'var(--onsec)':'var(--onv)'};display:flex">${icon(ic,24)}</span><span style="font:500 17px GSF">${t}</span></div>`).join('');
 return status+`<div style="flex:1;display:flex;min-height:0;background:var(--card)">
  <div style="width:300px;padding:8px 12px;display:flex;flex-direction:column;gap:4px"><div style="display:flex;align-items:center;gap:8px;padding:4px 4px 14px"><span style="display:flex;width:48px;height:48px;align-items:center;justify-content:center">${icon('Back',24)}</span><span style="font:400 26px GSF;white-space:nowrap">${title}</span></div>${nav}</div>
  <div style="flex:1;margin:12px 16px 16px 0;border-radius:28px;background:var(--bg);padding:28px 32px;overflow:hidden"><div style="max-width:720px;margin:0 auto;display:flex;flex-direction:column;gap:18px"><div style="font:400 26px GSF">${sections[chosen][0]}</div>${body}</div></div></div>`}
function tabletSettings(theme){
 const body=[tog('Grid lines','The axes always show',true),tog('Legend','Each line’s name in the corner, and in exports. Hold a line to rename it',true),tog('Mark points on curves','Zeros, extrema and crossings of the tapped curve',true),segc('Starting view',['±5','±10','±20'],1),segc('Complex plot quality',['Standard','High'],0),segc('3D surface detail',['Low','Medium','High'],1)].join('');
 frame(sectioned(theme,'Settings',[['Appearance','Palette'],['Calculator','Calc'],['History','Hist'],['Numbers','Pin'],['Graphs','Chart'],['Touch and screen','Touch'],['About','Info']],4,body),'tablet',theme,'Tablet · Settings: sections on the left, one at a time in a card')}
function tabletSettingsAppearance(theme){
 const sw6=['#5B6133','#3F6FD8','#1E8A83','#3C8A3F','#C88A12','#C23B32','#C0476F','#7A4FC4','#777777'].map((c,i)=>`<span style="width:40px;height:40px;border-radius:50%;background:${c};${i==0?'box-shadow:0 0 0 3px var(--bg),0 0 0 5px var(--on)':''}"></span>`).join('');
 const body=[segc('Theme',['System','Light','Dark'],2),tog('Colors from your wallpaper','Material You dynamic color',false),`<div><div style="font:17px GSF;margin-bottom:10px">Theme color</div><div style="display:flex;gap:12px">${sw6}</div></div>`,segc('Maths size',['Small','Medium','Large'],1),segc('Keypad size',['Compact','Medium','Tall'],1),segc('Keyboard side',['Left','Right'],1),tog('Expressive motion','Springy animations; off for calmer ones',true)].join('');
 frame(sectioned(theme,'Settings',[['Appearance','Palette'],['Calculator','Calc'],['History','Hist'],['Numbers','Pin'],['Graphs','Chart'],['Touch and screen','Touch'],['About','Info']],0,body),'tablet',theme,'Tablet · Appearance, with “Keyboard side” (only shown on tablets)')}
function tabletAcks(theme){
 const card=(n,by,use,about,lic)=>`<div style="flex:1;border-radius:20px;background:var(--card);padding:12px 16px;display:flex;flex-direction:column;gap:3px"><div style="display:flex;align-items:center"><span style="flex:1;font:500 15px GSF">${n}</span><span style="color:var(--onv);display:flex">${icon('Ext',16)}</span></div><div style="font:13px GSF;color:var(--onv)">${by}</div><div style="font:15px GSF">Used for: ${use}</div><div style="font:13px GSF;color:var(--onv)">${about}</div><span style="align-self:flex-start;margin-top:4px;border-radius:99px;background:var(--sec);color:var(--onsec);font:500 11px GSF;padding:2px 8px">${lic}</span></div>`;
 const rows=[[card('Google Sans Flex','Google','keys, labels and text','A variable font with width, weight and roundness axes.','SIL Open Font License 1.1'),card('Roboto','Christian Robertson, Google','characters Google Sans Flex lacks','The Android system font.','SIL Open Font License 1.1')],
  [card('MathJax TeX fonts','The MathJax Consortium','the maths','Computer Modern: Main, Math Italic and Size2.','SIL Open Font License 1.1'),card('Material Symbols','Google','icons','Material Design’s icon set.','Apache License 2.0')]];
 const body=`<div style="font:14px GSF;color:var(--onv)">Every font is bundled with the app under the SIL Open Font License, whose text is in the app’s source.</div>`+rows.map(r=>`<div style="display:flex;gap:12px">${r.join('')}</div>`).join('');
 frame(sectioned(theme,'Acknowledgements',[['Fonts','Font'],['Libraries','Books'],['Data','Dataset'],['Numerical methods','Fx'],['Graphics','Chart'],['Inspiration','Bulb']],0,body),'tablet',theme,'Tablet · Acknowledgements: groups on the left, cards two across')}

// ---- Phones: the graph with its legend, the table, renaming.
const DATA=[[0,0.1,0.30,21.4],[0.5,1.2,0.25,21.6],[1,1.9,0.30,21.5],[1.5,2.3,0.35,21.9],[2,1.8,0.30,22.0],[2.5,1.1,0.25,22.3],[3,0.2,0.30,22.1],[3.5,-0.8,0.35,22.4],[4,-1.7,0.30,22.6],[4.5,-2.2,0.25,22.5],[5,-1.9,0.30,22.8],[5.5,-1.0,0.35,23.0]];
function plotMini(theme,w,h,view,fns,points,err){
 const pal=PLOT[theme],on=theme=='dark'?'#C8C7B5':'#47473B',grid=theme=='dark'?'71,71,59':'200,199,181';
 const [x0,x1,y0,y1]=view,sx=v=>(v-x0)/(x1-x0)*w,sy=v=>h-(v-y0)/(y1-y0)*h;
 let o=`<svg width="${w}" height="${h}" style="display:block">`;
 for(let v=Math.ceil(x0*2)/2;v<=x1;v+=.5)o+=`<line x1="${sx(v)}" y1="0" x2="${sx(v)}" y2="${h}" stroke="rgba(${grid},${v%1?.35:.9})"/>`;
 for(let v=Math.ceil(y0*2)/2;v<=y1;v+=.5)o+=`<line x1="0" y1="${sy(v)}" x2="${w}" y2="${sy(v)}" stroke="rgba(${grid},${v%1?.35:.9})"/>`;
 o+=`<line x1="0" y1="${sy(0)}" x2="${w}" y2="${sy(0)}" stroke="${on}" stroke-width="2"/><line x1="${sx(0)}" y1="0" x2="${sx(0)}" y2="${h}" stroke="${on}" stroke-width="2"/>`;
 for(let v=Math.ceil(x0);v<=x1;v++)if(v)o+=`<text x="${sx(v)}" y="${sy(0)+15}" text-anchor="middle" font-size="11" fill="${on}" style="font-family:GSF;font-weight:500">${String(v).replace('-','−')}</text>`;
 for(let v=Math.ceil(y0);v<=y1;v++)if(v)o+=`<text x="${sx(0)+5}" y="${sy(v)+4}" font-size="11" fill="${on}" style="font-family:GSF;font-weight:500">${String(v).replace('-','−')}</text>`;
 // The first line last, on top.
 const items=[...fns.map(([f,c])=>({f,c})),...(points?[{pts:points,c:points.c}]:[])];
 [...items].reverse().forEach(it=>{
  if(it.f){let p=[];for(let i=0;i<=w;i+=2){const x=x0+i/w*(x1-x0);p.push(`${i},${sy(it.f(x)).toFixed(1)}`)}o+=`<polyline points="${p.join(' ')}" fill="none" stroke="${pal[it.c]}" stroke-width="3" stroke-linejoin="round"/>`}
  else it.pts.forEach(([x,y,sd])=>{const X=sx(x),Y=sy(y);if(err){const a=sy(y-sd),b=sy(y+sd);o+=`<path d="M${X} ${a}V${b}M${X-4} ${a}h8M${X-4} ${b}h8" stroke="${pal[it.c]}" stroke-width="1.5"/>`}o+=`<circle cx="${X}" cy="${Y}" r="5" fill="${pal[it.c]}"/>`})});
 return o+'</svg>'}
// The legend: a faint panel, a sample of each line and its name in LaTeX's font.
function legendHTML(entries){return `<div style="position:absolute;left:10px;top:10px;border-radius:14px;background:color-mix(in srgb,var(--bg) 82%,transparent);padding:6px 10px;display:flex;flex-direction:column;gap:3px">${entries.map(e=>`<div style="display:flex;align-items:center;gap:8px"><svg width="24" height="14">${e.strip?e.strip.map((c,k)=>`<rect x="${k*4}" y="3" width="4.5" height="8" fill="${c}"/>`).join(''):''}${e.line?`<line x1="1" y1="7" x2="23" y2="7" stroke="${e.color}" stroke-width="2.5" stroke-linecap="round"/>`:''}${e.dot?`<circle cx="12" cy="7" r="4" fill="${e.color}"/>`:''}</svg>${e.text?`<span style="font:15px CMR;color:var(--on)">${e.text}</span>`:mv(e.row,14,P.on)}</div>`).join('')}</div>`}
function listRow(row,color,{active,table,handle=true,text}={}){
 const cur=active?{cursorRow:row,cursorIndex:row.length}:{};
 return `<div style="display:flex;align-items:center;border-radius:20px;background:${active?'var(--dig)':'var(--card)'};padding:6px 4px 6px 8px">
  <span style="width:40px;display:flex;justify-content:center"><span style="width:18px;height:18px;border-radius:50%;background:${color}"></span></span>
  <span style="flex:1;overflow:hidden;white-space:nowrap">${text?`<span style="font:13px GSF;color:var(--onv)">${text}</span><br>`:''}${mv(row,22,P.on,cur)}</span>
  ${table?`<span style="width:44px;display:flex;justify-content:center;color:var(--pri)">${icon('Table',24)}</span>`:''}
  <span style="width:44px;display:flex;justify-content:center;color:var(--onv)">${icon('Close',22)}</span>
  ${handle?`<span style="width:36px;display:flex;justify-content:center;color:var(--onv)">${icon('Drag',22)}</span>`:''}</div>`}
const circle=(ic,bg,fg,s=48)=>`<span style="width:${s}px;height:${s}px;border-radius:50%;background:${bg};color:${fg};display:flex;align-items:center;justify-content:center;box-shadow:0 2px 6px rgba(0,0,0,.25)">${icon(ic,22)}</span>`;
function graphPhone(theme,o={}){
 P=PAL[theme];const pal=PLOT[theme];
 const pts=DATA.map(d=>[d[0]-3,d[1],d[2]]);pts.c=2;
 const view=[-3.6,3.6,-3.3,3.3];
 let h=status+topRow(1,`<span style="color:var(--onv)">${icon('Focus',24)}</span>`,'');
 h+=`<div style="height:470px;position:relative;overflow:hidden">${plotMini(theme,412,470,view,[[x=>2*Math.sin(x),0],[x=>x*x/4-2,1]],pts,true)}
   ${legendHTML([{color:pal[0],line:1,row:R('2',func('sin',R('x')))},{color:pal[1],line:1,row:R(frac(R('x',pow(R('2'))),R('4')),S('−'),S('2'))},{color:pal[2],dot:1,text:'Measured <i style="font-family:CMI;font-style:normal">v</i>(<i style="font-family:CMI;font-style:normal">t</i>)'}])}
   <div style="position:absolute;left:0;right:0;bottom:10px;display:flex;align-items:center;gap:8px;padding:0 12px">${circle('Add','var(--sec)','var(--onsec)')}${circle('Upload','var(--sec)','var(--onsec)')}<span style="flex:1"></span>${circle('Share','var(--dig)','var(--on)')}</div></div>`;
 h+=`<div style="display:flex;flex-direction:column;gap:6px;padding:8px 12px">${listRow(R('y',S('='),S('2'),func('sin',R('x'))),pal[0])}${listRow(R('y',S('='),frac(R('x',pow(R('2'))),R('4')),S('−'),S('2')),pal[1])}${listRow(R(S('['),S('('),S('−'),S('3'),S(','),S('0'),S('.'),S('1'),S(')'),S(','),S('('),S('−'),S('2'),S('.'),S('5'),S(','),S('1'),S('.'),S('2'),S(')'),S(','),S('…')),pal[2],{table:true,text:'12 points'})}</div>`;
 if(o.rename){h+=`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center;z-index:5"><div style="width:340px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:14px">
   <div style="font:400 24px GSF">Name in the legend</div>
   <div style="position:relative;border:2px solid var(--pri);border-radius:6px;padding:14px 12px;font:15px monospace">Measured $v(t)$<span style="display:inline-block;width:2px;height:18px;background:var(--pri);vertical-align:-3px"></span><span style="position:absolute;top:-9px;left:10px;padding:0 4px;background:var(--dig);font:12px GSF;color:var(--pri)">Name</span></div>
   <div style="font:12px GSF;color:var(--onv);margin-top:-8px;padding-left:14px">Maths between $ signs, in LaTeX: $\\sin x$</div>
   <div style="border-radius:14px;background:var(--swb);padding:12px 14px;font:20px CMR">Measured <span style="font-family:CMI">v</span>(<span style="font-family:CMI">t</span>)</div>
   <div style="display:flex;justify-content:flex-end;gap:22px;font:500 15px GSF;color:var(--pri)"><span>Default</span><span>Cancel</span><span>Save</span></div></div></div>`}
 frame(h,'phone',theme,o.rename?'Hold a line → rename it (LaTeX between $ signs)':'2D · legend top left; table button on the data line')}
function tablePhone(theme,o={}){
 P=PAL[theme];
 const cols=[['t','x'],['v','y'],['dv','s','σ(y)'],['T','n']];
 const chip=(c)=>{const r=c[1];return `<div style="height:32px;border-radius:99px;background:${RW[r]};color:${RF[r]};display:flex;align-items:center;padding:0 6px 0 12px"><span style="flex:1;${r=='n'?'font:500 13px GSF':'font:17px CMI'}">${r=='n'?'Not used':(c[2]||r)}</span>${icon('Drop',18)}</div>`};
 let h=status+`<div style="display:flex;align-items:center;padding:4px 4px 4px 4px"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">${icon('Close',24)}</span><div style="flex:1"><div style="font:22px GSF">Data table</div><div style="font:13px GSF;color:var(--onv)">12 points · 4 columns</div></div><span style="border-radius:99px;background:var(--pri);color:var(--onpri);font:500 14px GSF;padding:10px 20px;margin-right:8px">Done</span></div>
  <div style="font:13px GSF;color:var(--onv);padding:2px 16px">Tap a column’s role to choose x, y, σ(x) or σ(y). Columns without a role are kept but not plotted.</div>
  <div style="display:flex;padding-top:10px"><span style="width:44px"></span><div style="flex:1;overflow:hidden;display:flex;gap:6px">${cols.map(c=>`<div style="flex:none;width:104px;display:flex;flex-direction:column;gap:5px">${chip(c)}<span style="font:13px GSF;color:var(--onv);padding:0 6px">${c[0]}</span></div>`).join('')}</div><span style="width:40px"></span></div>
  <div style="height:1px;background:var(--line);margin-top:6px"></div>
  <div style="display:flex;flex-direction:column;gap:4px;padding:6px 0">`;
 DATA.forEach((d,i)=>{h+=`<div style="display:flex;align-items:center"><span style="width:44px;padding-left:12px;font:500 12px GSF;color:var(--onv)">${i+1}</span><div style="flex:1;overflow:hidden;display:flex;gap:6px">${d.map((v,k)=>`<span style="flex:none;width:104px;height:40px;box-sizing:border-box;border-radius:10px;background:var(--swb);padding:9px 10px;font:17px CMR;color:${k==3?'var(--onv)':'var(--on)'}${i==6&&k==2&&o.bad?';outline:1.5px solid var(--err)':''}">${i==6&&k==2&&o.bad?'0,3o':String(v).replace('-','−')}</span>`).join('')}</div><span style="width:40px;display:flex;justify-content:center;color:var(--onv)">${icon('Close',18)}</span></div>`});
 h+=`</div><div style="position:absolute;left:0;right:0;bottom:0;padding:10px 8px 18px;display:flex;gap:8px;background:var(--bg)"><span style="border-radius:99px;background:var(--sec);color:var(--onsec);font:500 14px GSF;padding:10px 18px;display:flex;align-items:center;gap:6px">${icon('Add',18)} Row</span><span style="border-radius:99px;border:1px solid var(--outv);color:var(--pri);font:500 14px GSF;padding:10px 18px;display:flex;align-items:center;gap:6px">${icon('Add',18)} Column</span></div>`;
 if(o.menu){h+=`<div style="position:absolute;left:${412-200}px;top:246px;width:190px;border-radius:12px;background:var(--dig);box-shadow:0 4px 16px rgba(0,0,0,.4);padding:8px 0;z-index:6;font:15px GSF">${[['x','  (across)'],['y','  (up)'],['σ(x)','  (error bars)'],['σ(y)','  (error bars)']].map(([a,b])=>`<div style="padding:11px 16px"><span style="font:17px CMI">${a}</span><span style="color:var(--onv)">${b}</span></div>`).join('')}<div style="padding:11px 16px">Not used</div><div style="height:1px;background:var(--outv);margin:4px 0"></div><div style="padding:11px 16px;color:var(--err)">Remove column</div></div>`}
 frame(h,'phone',theme,o.menu?'Table · picking a column’s role':'Table · full screen: x, y and σ(y) picked, T kept unused')}
function phone3DLegend(theme){
 P=PAL[theme];const pal=PLOT[theme];
 let h=status+topRow(2,`<span style="color:var(--onv)">${icon('Rotate',24)}</span>`,'');
 const c=hex(pal[0]),lo=mix(c,[0,0,0],.35),hi=mix(c,[255,255,255],.45);
 const strip=[0,1,2,3,4,5].map(k=>`rgb(${mix(lo,hi,k/5).map(Math.round)})`);
 h+=`<div style="height:560px;position:relative;overflow:hidden">${svg3d(G3,theme)}
   <div style="position:absolute;left:12px;top:12px;display:flex;flex-direction:column;gap:8px;align-items:flex-start">
    <span style="display:flex;align-items:center;border-radius:99px;background:var(--dig);color:var(--on)"><span style="width:44px;height:44px;display:flex;align-items:center;justify-content:center">${icon('Remove',22)}</span><span style="font:16px CMR"><i style="font-family:CMI;font-style:normal">x</i>, <i style="font-family:CMI;font-style:normal">y</i> ∈ [−3, 3]</span><span style="width:44px;height:44px;display:flex;align-items:center;justify-content:center">${icon('Add',22)}</span></span>
    <div style="position:relative">${legendHTML([{strip,row:R(func('sin',R('x')),func('cos',R('y')))}]).replace('position:absolute;left:10px;top:10px;','')}</div></div></div>`;
 h+=`<div style="display:flex;flex-direction:column;gap:6px;padding:8px 12px">${listRow(R('z',S('='),func('sin',R('x')),func('cos',R('y'))),pal[0])}</div>`;
 frame(h,'phone',theme,'3D · legend under the range (resize) control')}
function exportShot(){const w=document.createElement('div');w.innerHTML=caption('Exported (real output of the export code): SciencePlots colours, legend, error bars')+`<img src="export_legend.png" style="width:412px;height:412px;border-radius:8px;box-shadow:0 10px 30px rgba(0,0,0,.25)">`;document.getElementById('sheet').appendChild(w)}
Promise.all(['GSF','Rob','CMR','CMI'].map(f=>document.fonts.load('400 20px '+f))).then(()=>{
 tabletSettings('light');tabletAcks('dark');tabletSettingsAppearance('dark');
 const br=document.createElement('div');br.style.cssText='grid-column:1/-1;height:0';document.getElementById('sheet').appendChild(br);
 graphPhone('light');graphPhone('dark',{rename:true});tablePhone('light');tablePhone('dark',{menu:true});phone3DLegend('light');exportShot();
 document.title='ready'});
