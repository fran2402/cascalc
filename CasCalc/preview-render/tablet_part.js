// ---- Tablets (CalculatorScreen.kt / GraphScaffold in GraphCommon.kt): the keyboard in its own column,
// on the side Settings › "Keyboard side on tablets" picks. 1280 × 800 dp, landscape.
const TW=1280,TH_=800,KW=Math.min(440,Math.max(300,Math.round(TW*0.32))),MAINH=Math.round(Math.min(88,Math.max(44,TH_*0.075)));
function tabKeypad(o){
 const MK=[['AC','ac'],['( )','o'],[`<span style="font-family:CMI">${o.v||'x'}</span>`,'o'],['÷','o'],['7','d'],['8','d'],['9','d'],['×','o'],['4','d'],['5','d'],['6','d'],['−','o'],['1','d'],['2','d'],['3','d'],['+','o'],['0','d'],['.','d'],[I.bs,'d'],['<svg width="30" height="30" viewBox="0 0 24 24" fill="currentColor"><path d="M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7z"/></svg>','eq']];
 const rowsT=TABKEYS[o.tab];const cols=rowsT[0].length;
 // No handle and no hide button: on a tablet the keyboard is always there, its keys at the bottom.
 return `<div style="width:${KW}px;flex:none;height:100%;display:flex;flex-direction:column;justify-content:flex-end;background:var(--card);padding-bottom:10px">
  <div class="ctrl"><span class="seg"><span class="${o.deg?'':'sel'}">Rad</span><span class="${o.deg?'sel':''}">Deg</span></span><span style="margin-left:auto">${icon('Undo',24)}</span><span style="opacity:.38">${icon('Redo',24)}</span>${I.left+I.right}</div>
  <div class="tabs" style="overflow:hidden;gap:2px">${TABS.map((t,i)=>`<span class="${i==o.tab?'sel':''}" style="width:36px;flex:none;padding:0;justify-content:center">${label(t,i==o.tab?P.onsec:P.onv,20)}</span>`).join('')}</div>
  <div class="grid" style="grid-template-columns:repeat(${cols},1fr)">${rowsT.flat().map(k=>`<div class="k fn" style="height:50px">${label(k,P.onsec,18)}</div>`).join('')}</div>
  <div class="grid main" style="margin-bottom:0">${MK.map(([l,c])=>`<div class="k mk ${c}" style="height:${MAINH}px;font-size:30px">${l}</div>`).join('')}</div></div>`}
function tabletFrame(theme,mode,left,body,caption){
 P=PAL[theme];const w=document.createElement('div');w.style.cssText='display:flex;flex-direction:column;gap:14px';
 w.innerHTML=`<div style="font:500 22px GSF;color:#2b2a24">${caption}</div><div class="tablet ${theme}"><div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>${topRow(mode,mode==0?`<span>${I.hist}</span>`:`<span style="color:var(--onv)">${icon(mode==2?'Rotate':'Focus',24)}</span>`,I.more)}<div style="flex:1;display:flex;min-height:0;flex-direction:${left?'row':'row-reverse'}">${body}</div></div>`;
 document.getElementById('sheet').appendChild(w)}
function tabletCalc(theme,left,o){
 P=PAL[theme];
 let d=`<div style="flex:1;display:flex;flex-direction:column;justify-content:flex-end;padding:0 28px 18px;min-width:0">`;
 o.cards.forEach(k=>{const c=EX[k];d+=`<div class="card" style="padding:14px 12px 18px 24px;margin-top:10px"><div style="display:flex"><div class="cexpr" style="flex:1;overflow:hidden">${mv(MT.decode(c.e),20,P.onv)}</div><span style="color:var(--onv)">${icon('Share',22)}</span></div><div class="cres" style="padding-right:12px">${c.a?`<span class="chip">${I.chev}≈</span>`:''}${stmt(k)?'<span style="margin-left:auto"></span>':'<span class="eqs">'+approxSign(k)+'</span>'}<span class="res">${mv(MT.decode(c.x),24,P.on)}</span></div></div>`});
 const inp=MT.decode(EX[o.input].e);
 d+=`<div class="input" style="font-size:48px;padding-top:22px">${mv(inp,32,P.on,{cursorRow:inp,cursorIndex:inp.length})}</div><div class="prev">${(stmt(o.input)?'':'<span style="font-size:18px;font-family:CMR">'+approxSign(o.input)+' </span>')+mv(MT.decode(EX[o.input].x),19,P.onv)}</div></div>`;
 return (left?tabKeypad(o)+d:d+tabKeypad(o))}
// A plot drawn straight from the functions, at the tablet's size.
function plot2d(theme,w,h,fns,view){
 const pal=PLOT[theme],on=theme=='dark'?'#C8C7B5':'#47473B',grid=theme=='dark'?'71,71,59':'200,199,181';
 const [x0,x1,y0,y1]=view,sx=v=>(v-x0)/(x1-x0)*w,sy=v=>h-(v-y0)/(y1-y0)*h;
 let o=`<svg width="${w}" height="${h}" style="display:block">`;
 for(let v=Math.ceil(x0*2)/2;v<=x1;v+=.5)o+=`<line x1="${sx(v)}" y1="0" x2="${sx(v)}" y2="${h}" stroke="rgba(${grid},${v%1?.35:.9})"/>`;
 for(let v=Math.ceil(y0*2)/2;v<=y1;v+=.5)o+=`<line x1="0" y1="${sy(v)}" x2="${w}" y2="${sy(v)}" stroke="rgba(${grid},${v%1?.35:.9})"/>`;
 o+=`<line x1="0" y1="${sy(0)}" x2="${w}" y2="${sy(0)}" stroke="${on}" stroke-width="2"/><line x1="${sx(0)}" y1="0" x2="${sx(0)}" y2="${h}" stroke="${on}" stroke-width="2"/>`;
 for(let v=Math.ceil(x0);v<=x1;v++)if(v)o+=`<text x="${sx(v)}" y="${sy(0)+16}" text-anchor="middle" font-size="12" fill="${on}" style="font-family:GSF;font-weight:500">${String(v).replace('-','−')}</text>`;
 for(let v=Math.ceil(y0);v<=y1;v++)if(v)o+=`<text x="${sx(0)+6}" y="${sy(v)+4}" font-size="12" fill="${on}" style="font-family:GSF;font-weight:500">${String(v).replace('-','−')}</text>`;
 // The first line is drawn last, so it's on top.
 [...fns].reverse().forEach(([f,c])=>{let pts=[];for(let i=0;i<=w;i+=2){const x=x0+i/w*(x1-x0),y=f(x);pts.push(`${i},${sy(y).toFixed(1)}`)}o+=`<polyline points="${pts.join(' ')}" fill="none" stroke="${pal[c]}" stroke-width="3" stroke-linejoin="round"/>`});
 return o+'</svg>'}
function tabletGraph(theme,left,o){
 P=PAL[theme];const pw=TW-KW-320,ph=TH_-96;
 const list=`<div style="width:320px;flex:none;display:flex;flex-direction:column;gap:6px;padding:10px 10px;background:var(--bg);border-${left?'right':'left'}:1px solid var(--line)">
   ${o.rows.map(([code,c,act])=>fnRow(code,c,'y',act)).join('')}${(o.sliders||[]).map(s=>slider(...s)).join('')}
   <div style="flex:1"></div>
   <div style="display:flex;align-items:center;gap:8px"><span style="width:48px;height:48px;border-radius:50%;background:var(--sec);color:var(--onsec);display:flex;align-items:center;justify-content:center">${icon('Add',24)}</span>
   <span style="width:48px;height:48px;border-radius:50%;background:var(--swb);color:var(--on);display:flex;align-items:center;justify-content:center"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15h8v2H8zm0-4h8v2H8z"/></svg></span></div></div>`;
 const canvas=`<div style="flex:1;position:relative;overflow:hidden;min-width:0">${plot2d(theme,pw,ph,o.fns,o.view)}</div>`;
 // The frame reverses the row when the keyboard is on the right: plot, lines, keyboard.
 return tabKeypad(o)+list+canvas}
Promise.all(['GSF','Rob','CMR','CMI'].map(f=>document.fonts.load('400 20px '+f))).then(()=>{
 const a=2,b=1.5;
 tabletFrame('dark',0,false,tabletCalc('dark',false,{tab:3,cards:['intByParts','solveQuad','limSinc'],input:'intNumeric'}),'Calculator · keyboard on the right (default)');
 tabletFrame('light',0,true,tabletCalc('light',true,{tab:2,cards:['factorQuartic','eigvecs'],input:'sumSquares'}),'Calculator · keyboard on the left');
 tabletFrame('dark',1,true,tabletGraph('dark',true,{tab:1,rows:[[ROWS.asinbx,0,true],[ROWS.para,1]],sliders:[['a','2',60],['b','1.5',57.5]],fns:[[x=>a*Math.sin(b*x),0],[x=>x*x/4-2,1]],view:[-6.2,6.2,-3.9,3.9]}),'2D graph · keyboard left → lines → plot');
 tabletFrame('light',1,false,tabletGraph('light',false,{tab:0,rows:[[ROWS.sinx,0,true],[ROWS.para,1]],fns:[[Math.sin,0],[x=>x*x/4-2,1]],view:[-6.2,6.2,-3.9,3.9]}),'2D graph · keyboard right: plot → lines → keyboard');
 document.title='ready'});
