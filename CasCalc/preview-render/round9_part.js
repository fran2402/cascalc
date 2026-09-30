// ---- Round of 30 September (3): the symbol builder (SymbolBuilder.kt), 3D inequalities as
// solids with your own letters (Graph3DScreen.kt, Surface3D.solid), STL export, the contour legend.
Object.assign(ICON,{
 Back2:'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
 Bksp:'M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3 12.59L17.59 17 14 13.41 10.41 17 9 15.59 12.59 12 9 8.41 10.41 7 14 10.59 17.59 7 19 8.41 15.41 12 19 15.59z',
 Del:'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
 Tune:'M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z',
});
const cap9=t=>`<div style="font:500 22px GSF;color:#2b2a24;margin-bottom:14px">${t}</div>`;
function put9(html,cls,theme,capt){const w=document.createElement('div');w.innerHTML=cap9(capt)+`<div class="${cls} ${theme}" style="position:relative">${html}</div>`;document.getElementById('sheet').appendChild(w)}
const st9=`<div class="status" style="padding:0 30px"><span>12:30</span><span>▾ ▴ ▮</span></div>`;
const ital=c=>/^[a-zA-Zα-ωϵϑϕϱςϖϰ]$/.test(c);
/** A built symbol in CSS: scripts before and after, an accent above, bold. */
function symHTML({b,acc='',sub='',sup='',ps='',pp='',bold=false},size){
 const f=ital(b)?'CMI':'CMR', w=bold?'font-weight:700;':'';
 const sc=(t)=>`<span style="font:${size*.62}px CMR;display:inline-flex;flex-direction:column;line-height:1">`;
 const col=(top,bot)=>top||bot?`<span style="display:inline-flex;flex-direction:column;justify-content:space-between;height:${size*1.25}px;vertical-align:${-size*.28}px;font:${size*.6}px CMR;line-height:1;margin:0 1px">${`<span>${[...top].map(c=>ital(c)?`<i style="font-family:CMI;font-style:normal">${c}</i>`:c).join('')||'&nbsp;'}</span>`}${`<span>${[...bot].map(c=>ital(c)?`<i style="font-family:CMI;font-style:normal">${c}</i>`:c).join('')||'&nbsp;'}</span>`}</span>`:'';
 const accent=acc?`<span style="position:absolute;left:${ital(b)?58:50}%;top:${acc=='→'?-.46:-.36}em;transform:translateX(-50%) ${acc=='→'?'scale(.75)':''};font-family:CMR;${w}">${acc}</span>`:'';
 return `<span style="display:inline-flex;align-items:center;font-size:${size}px;white-space:nowrap">${col(pp,ps)}<span style="position:relative;font-family:${f};${w}">${b}${accent}</span>${col(sup,sub)}</span>`}
const chip9=(inner,sel,w)=>`<span style="min-width:${w||44}px;height:44px;padding:0 8px;box-sizing:border-box;border-radius:14px;display:inline-flex;align-items:center;justify-content:center;background:${sel?'var(--pc)':'var(--swb)'};${sel?'box-shadow:inset 0 0 0 2px var(--pri)':''}">${inner}</span>`;
const fchip=(t,sel)=>`<span style="border-radius:8px;padding:7px 12px;font:500 13px GSF;${sel?'background:var(--sec);color:var(--onsec)':'border:1px solid var(--outv);color:var(--onv)'}">${sel?'✓ ':''}${t}</span>`;
const step9=(n,t)=>`<div style="display:flex;align-items:center;gap:10px;margin-top:6px">${n?`<span style="border-radius:99px;background:var(--pri);color:var(--onpri);font:500 13px GSF;padding:2px 9px">${n}</span>`:''}<span style="font:500 17px GSF">${t}</span></div>`;
const GREEKALL=[..."αβγδεζηθικλμνξοπρστυφχψωϵϑϰϖϱςϕΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"];
const HEB=["ℵ","ℶ","ℷ","ℸ",..."אבגדהוזחטיכלמנסעפצקרשת"];
const ACCS=[['','None'],['˙'],['¨'],['ˆ'],['˜'],['¯'],['→'],['ˇ'],['˘'],['´'],['`'],['˚'],['⋯']];
function letterSection(alpha,letters,sel,bold){return step9('1','Letter')+`<div style="display:flex;flex-wrap:wrap;gap:6px">${['Latin','Greek','Hebrew','Calligraphic','Fraktur','Blackboard'].map(a=>fchip(a,a==alpha)).join('')}</div>
 <div style="display:flex;flex-wrap:wrap;gap:6px">${letters.map(l=>chip9(`<span style="font:22px ${ital(l)?'CMI':'CMR'};${bold?'font-weight:700':''}">${l}</span>`,l==sel)).join('')}</div>
 <div style="display:flex;align-items:center"><div style="flex:1"><div style="font:16px GSF">Bold</div><div style="font:13px GSF;color:var(--onv)">For vectors and matrices, as \\boldsymbol</div></div>${bold?`<span style="width:52px;height:32px;border-radius:16px;background:var(--pri);display:flex;align-items:center;justify-content:flex-end;padding-right:4px;box-sizing:border-box"><span style="width:24px;height:24px;border-radius:50%;background:var(--onpri)"></span></span>`:`<span style="width:52px;height:32px;border-radius:16px;border:2px solid var(--outv);display:flex;align-items:center;padding-left:6px;box-sizing:border-box"><span style="width:16px;height:16px;border-radius:50%;background:var(--outv)"></span></span>`}</div>`}
function accentSection(b,sel,bold){return step9('2','Accent')+`<div style="display:flex;flex-wrap:wrap;gap:6px">${ACCS.map(([a,n])=>chip9(n?`<span style="font:500 13px GSF">${n}</span>`:symHTML({b,acc:a,bold},24),a==sel&&!n||(n&&!sel))).join('')}</div>`}
const slot9=(t,c,act)=>`<div style="border-radius:16px;padding:8px 12px;background:${act?'var(--sec)':'var(--card)'};${act?'box-shadow:inset 0 0 0 2px var(--pri)':''}"><div style="font:500 12px GSF;color:${act?'var(--onsec)':'var(--onv)'}">${t}</div><div style="height:28px;display:flex;align-items:center;font:20px CMR">${c||'<span style="font:13px GSF;color:var(--onv);opacity:.6">empty</span>'}</div></div>`;
function scriptSection(sym,active,group){const pad=group==0?[..."0123456789+−=(),′″*†‡∘⋆⊥∥±∞·×"]:group==1?[..."abcdefghijklmnopqrstuvwxyz"]:[..."αβγδεζηθικλμνξπρστυφχψω"];
 return step9('3','Scripts')+`<div style="font:13px GSF;color:var(--onv)">Tap a box, then type into it. Scripts can go after the letter, or before it (as in ¹⁴₆C).</div>
 <div style="display:flex;align-items:center;gap:8px"><div style="flex:1;display:flex;flex-direction:column;gap:8px">${slot9('Before, above',sym.pp,active=='pp')}${slot9('Before, below',sym.ps,active=='ps')}</div><div style="width:64px;text-align:center">${symHTML({b:sym.b,acc:sym.acc,bold:sym.bold},36)}</div><div style="flex:1;display:flex;flex-direction:column;gap:8px">${slot9('Superscript',sym.sup,active=='sup')}${slot9('Subscript',sym.sub,active=='sub')}</div></div>
 <div style="display:flex;flex-wrap:wrap;gap:6px">${['Numbers and signs','Latin','Greek'].map((t,k)=>fchip(t,k==group)).join('')}</div>
 <div style="display:flex;flex-wrap:wrap;gap:6px">${pad.map(c=>chip9(`<span style="font:20px ${ital(c)?'CMI':'CMR'}">${c}</span>`,false)).join('')}${chip9(icon('Bksp',22),false)}${chip9('<span style="font:500 13px GSF">Clear</span>',false)}</div>`}
function preview9(sym,latex,big){return `<div style="border-radius:28px;background:var(--dig);padding:${big?36:22}px 0;text-align:center">${symHTML(sym,big?96:72)}</div>
 <div style="position:relative;border:1px solid var(--outv);border-radius:6px;padding:14px 12px;font:15px monospace;margin-top:4px">${latex}<span style="position:absolute;top:-9px;left:10px;padding:0 4px;background:var(--bg);font:12px GSF;color:var(--onv)">LaTeX</span></div><div style="font:12px GSF;color:var(--onv);padding:4px 14px">Type or paste the symbol in LaTeX, or build it below</div>`}
const buttons9=`<div style="display:flex;justify-content:flex-end;gap:12px;margin-top:8px"><span style="border:1px solid var(--outv);border-radius:99px;padding:10px 22px;font:500 14px GSF;color:var(--pri)">Cancel</span><span style="border-radius:99px;padding:10px 22px;font:500 14px GSF;background:var(--pri);color:var(--onpri)">Save and insert</span></div>`;
function savedList(){return step9('','Your symbols')+[{b:'x',acc:'ˆ',sub:'1'},{b:'θ',acc:'˙',sup:'2'},{b:'𝔤',sub:'i'}].map(s=>`<div style="display:flex;align-items:center;gap:8px"><span style="flex:1">${symHTML(s,28)}</span><span style="font:12px GSF;color:var(--onv)">${s.b=='x'?'\\hat{x}_{1}':s.b=='θ'?'\\dot{\\theta}^{2}':'\\mathfrak{g}_{i}'}</span><span style="color:var(--onv);display:flex;padding:8px">${icon('Del',22)}</span></div>`).join('')}
function tabletBuilder(theme){P=PAL[theme];
 const sym={b:'v',acc:'→',sub:'i',sup:'',ps:'',pp:'2',bold:true};
 const h=st9+`<div style="display:flex;align-items:center;gap:8px;padding:4px 12px"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">${icon('Back2',24)}</span><span style="font:400 26px GSF">Build a symbol</span></div>
 <div style="display:flex;gap:32px;padding:4px 24px;overflow:hidden"><div style="flex:1;display:flex;flex-direction:column;gap:12px">${preview9(sym,'{}^{2}\\vec{\\boldsymbol{v}}_{i}',true)}${letterSection('Latin',[...'abcdefghijklmnopqrstuvwxyz'],'v',true)}</div>
 <div style="flex:1;display:flex;flex-direction:column;gap:12px">${accentSection('v','→',true)}${scriptSection(sym,'sub',0)}${buttons9}</div></div>`;
 put9(h,'tablet',theme,'Tablet · symbol builder in two columns, with a LaTeX field')}
function phoneBuilder(theme,alpha){P=PAL[theme];
 const heb=alpha=='Hebrew';
 const sym=heb?{b:'ℵ',sub:'0'}:{b:'C',ps:'6',pp:'14'};
 const h=st9+`<div style="display:flex;align-items:center;gap:8px;padding:4px 4px"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">${icon('Back2',24)}</span><span style="font:400 22px GSF">Build a symbol</span></div>
 <div style="padding:0 20px;display:flex;flex-direction:column;gap:12px;height:820px;overflow:hidden">${preview9(sym,heb?'\\aleph_{0}':'{}_{6}^{14}C',false)}${heb?letterSection('Hebrew',HEB,'ℵ',false):letterSection('Greek',GREEKALL,'',false)}${heb?'':scriptSection(sym,'pp',0)}${heb?accentSection('ℵ','',false):''}</div>`;
 put9(h,'phone',theme,heb?'Hebrew letters (ℵ ℶ ℷ ℸ, then א–ת)':'Full Greek; scripts before the letter (¹⁴₆C)')}
// ---- 3D: a solid (inequality) seen with the app's camera, and the letters i, j, k.
function proj(x,y,z,w,h,s){const yaw=.7,pitch=.55,cy=Math.cos(yaw),sy=Math.sin(yaw);const x1=x*cy-y*sy,y1=x*sy+y*cy;const cp=Math.cos(pitch),sp=Math.sin(pitch);const r=[x1,y1*cp-z*sp,y1*sp+z*cp];const k=3.2/(3.2+r[1]),S=.3*Math.min(w,h)*s;return [w/2+r[0]*S*k,h/2-r[2]*S*k,r[1]]}
function solidSvg(theme,w,h){const pal=theme=='dark'?[197,203,134]:[91,97,51];const bg=theme=='dark'?'#12130C':'#FCF9EE';
 // x²+y²+z² ≤ 4, z ≥ 0 in the box [−3,3]³ scaled to [−1,1]: a dome and its floor (a wall of the box? no: the cut z=0).
 const R=2/3,faces=[];const N=18,M=36;
 for(let i=0;i<N;i++)for(let j=0;j<M;j++){const t0=i/N*Math.PI/2,t1=(i+1)/N*Math.PI/2,p0=j/M*2*Math.PI,p1=(j+1)/M*2*Math.PI;
  const pt=(t,p)=>[R*Math.sin(t)*Math.cos(p),R*Math.sin(t)*Math.sin(p),R*Math.cos(t)-1+1e-9];
  faces.push({pts:[pt(t0,p0),pt(t1,p0),pt(t1,p1),pt(t0,p1)],wall:false})}
 // The box floor is at z = −1; the solid z ≥ 0 part is shown as dome over the plane z=0 → shift: dome sits on z=-1 floor here.
 const disc=[];for(let j=0;j<=48;j++){const p=j/48*2*Math.PI;disc.push([R*Math.cos(p),R*Math.sin(p),-1])}faces.push({pts:disc,wall:true});
 const light=[-.4,-.6,.7];const L=Math.hypot(...light);
 const out=faces.map(f=>{const q=f.pts.map(p=>proj(p[0],p[1],p[2],w,h,1));const d=q.reduce((a,b)=>a+b[2],0)/q.length;
  const a=f.pts[0],b=f.pts[1],c=f.pts[2]||f.pts[1];const u=[b[0]-a[0],b[1]-a[1],b[2]-a[2]],v=[c[0]-a[0],c[1]-a[1],c[2]-a[2]];const n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];const nl=Math.hypot(...n)||1;
  const lam=Math.abs((n[0]*light[0]+n[1]*light[1]+n[2]*light[2])/nl/L);const hgt=(f.pts.reduce((s,p)=>s+p[2],0)/f.pts.length+1)/1.4;
  const lo=pal.map(v=>v*.65),hi=pal.map(v=>v+(255-v)*.45);const base=lo.map((v,k)=>v+(hi[k]-v)*Math.min(1,Math.max(0,hgt)));const sh=.35+.65*lam;
  const col=f.wall?`rgba(${hi.map(Math.round)},.55)`:`rgb(${base.map(v=>Math.round(v*sh))})`;
  return {d,svg:`<path d="M${q.map(p=>p[0].toFixed(1)+' '+p[1].toFixed(1)).join('L')}Z" fill="${col}" stroke="${f.wall?'none':'rgba(0,0,0,.12)'}" stroke-width=".6"/>`}});
 out.sort((a,b)=>b.d-a.d);
 const edge=theme=='dark'?'#47473B':'#C8C7B5';let box='';const C=[-1,1];
 for(const a of C)for(const b of C){box+=[[[-1,a,b],[1,a,b]],[[a,-1,b],[a,1,b]],[[a,b,-1],[a,b,1]]].map(([p,q])=>{const P1=proj(...p,w,h,1),Q=proj(...q,w,h,1);return `<line x1="${P1[0]}" y1="${P1[1]}" x2="${Q[0]}" y2="${Q[1]}" stroke="${edge}"/>`}).join('')}
 const lab=(t,p)=>{const q=proj(...p,w,h,1);return `<text x="${q[0]}" y="${q[1]}" text-anchor="middle" font-size="18" fill="var(--onv)" style="font-family:CMI">${t}</text>`};
 return `<svg width="${w}" height="${h}" style="display:block">${box}${out.map(o=>o.svg).join('')}${lab('i',[1.2,-1,-.8])}${lab('j',[-1,1.2,-.8])}${lab('k',[-1,-1,1])}</svg>`}
function phone3D9(theme){P=PAL[theme];
 let h=st9+topRow(2,`<span style="color:var(--onv)">${icon('Rotate',24)}</span>`,'');
 h+=`<div style="height:520px;position:relative;overflow:hidden">${solidSvg(theme,412,520)}
  <div style="position:absolute;left:12px;top:12px;display:flex;flex-direction:column;gap:8px;align-items:flex-start"><span style="display:flex;align-items:center;border-radius:99px;background:var(--dig)"><span style="width:44px;height:44px;display:flex;align-items:center;justify-content:center">${icon('Remove',22)}</span><span style="font:15px CMR"><i style="font-family:CMI;font-style:normal">i</i>, <i style="font-family:CMI;font-style:normal">j</i> ∈ [−3, 3]</span><span style="width:44px;height:44px;display:flex;align-items:center;justify-content:center">${icon('Add',22)}</span></span>
  <div style="border-radius:14px;background:color-mix(in srgb,var(--bg) 82%,transparent);padding:6px 10px;display:flex;align-items:center;gap:8px"><svg width="24" height="14"><rect x="1" y="3" width="22" height="8" rx="2" fill="${PAL[theme].pri}"/></svg><span style="font:15px CMR"><i style="font-family:CMI;font-style:normal">i</i>² + <i style="font-family:CMI;font-style:normal">j</i>² + <i style="font-family:CMI;font-style:normal">k</i>² ≤ 4</span></div></div></div>`;
 h+=`<div style="display:flex;gap:8px;padding:8px 12px">${['i','j','k','r','θ','ρ','φ'].map(v=>`<span style="height:36px;min-width:52px;border-radius:99px;background:var(--tert);color:var(--ontert);display:flex;align-items:center;justify-content:center;font:20px CMI">${v}</span>`).join('')}</div>
  <div style="padding:0 12px"><div style="display:flex;align-items:center;border-radius:20px;background:var(--dig);padding:10px 8px"><span style="width:40px;display:flex;justify-content:center"><span style="width:18px;height:18px;border-radius:50%;background:${PAL[theme].pri}"></span></span><span style="flex:1;font:22px CMR"><i style="font-family:CMI;font-style:normal">i</i><sup>2</sup> + <i style="font-family:CMI;font-style:normal">j</i><sup>2</sup> + <i style="font-family:CMI;font-style:normal">k</i><sup>2</sup> ≤ 4, <i style="font-family:CMI;font-style:normal">k</i> ≥ 0<span style="display:inline-block;width:2px;height:24px;background:var(--pri);vertical-align:-4px"></span></span></div></div>`;
 put9(h,'phone',theme,'3D · an inequality is a solid; axes renamed i, j, k')}
function lettersDialog(theme){P=PAL[theme];
 const rowL=(n,ls)=>`<div style="display:flex;align-items:center;gap:6px"><span style="flex:1;font:15px GSF">${n}</span>${ls.map(l=>`<span style="width:56px;height:56px;border:1px solid var(--outv);border-radius:6px;display:flex;align-items:center;justify-content:center;font:18px CMI">${l}</span>`).join('')}</div>`;
 const seg=(opts,sel,fam)=>`<div style="display:flex;border:1px solid var(--outv);border-radius:99px;overflow:hidden;font:500 14px ${fam||'GSF'}">${opts.map((m,k)=>`<span style="flex:1;text-align:center;padding:9px 0;${k?'border-left:1px solid var(--outv);':''}${sel==k?'background:var(--sec);color:var(--onsec)':''}">${m}</span>`).join('')}</div>`;
 const h=st9+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:360px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:10px">
  <div style="font:400 24px GSF">Graph settings</div>
  <div style="font:500 14px GSF;color:var(--onv)">Coordinates</div>${seg(['i j k','r θ k','ρ θ φ'],0,'CMI')}
  <div style="font:500 14px GSF;color:var(--onv);margin-top:4px">Letters</div>${rowL('Cartesian',['i','j','k'])}${rowL('Cylindrical',['r','θ','k'])}${rowL('Spherical',['ρ','θ','φ'])}
  <div style="font:500 14px GSF;color:var(--pri)">Reset letters</div>
  <div style="font:500 14px GSF;color:var(--onv);margin-top:4px">Surface detail</div>${seg(['Low','Medium','High'],1)}
  <div style="text-align:right;font:500 15px GSF;color:var(--pri);margin-top:6px"><span style="margin-right:22px">Cancel</span>Done</div></div></div>`;
 put9(h,'phone',theme,'3D settings · your own letters for each system')}
function stlDialog(theme){P=PAL[theme];
 const h=st9+`<div style="position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center"><div style="width:360px;border-radius:28px;background:var(--dig);padding:24px;display:flex;flex-direction:column;gap:10px">
  <div style="font:400 24px GSF">Export graph</div>
  <div style="aspect-ratio:1;border-radius:16px;overflow:hidden;background:#fff;border:1px solid var(--outv)">${solidSvg('light',310,310).replace(/var\(--onv\)/g,'#000')}</div>
  <div style="display:flex;border:1px solid var(--outv);border-radius:99px;overflow:hidden;font:500 13px GSF">${['PDF','PNG','JPG','SVG','STL'].map((m,k)=>`<span style="flex:1;text-align:center;padding:9px 0;${k?'border-left:1px solid var(--outv);':''}${k==4?'background:var(--sec);color:var(--onsec)':''}">${m}</span>`).join('')}</div>
  <div style="font:12px GSF;color:var(--onv)">3D model for printing: each surface closed off into a solid inside the box (the floor under z = f(x, y)), 100 mm across. The preview shows the graph.</div>
  <div style="text-align:right;font:500 15px GSF;color:var(--pri);margin-top:6px"><span style="margin-right:22px">Cancel</span><span style="margin-right:22px">Share</span>Save</div></div></div>`;
 put9(h,'phone',theme,'Export · STL for 3D printing')}
function export9(){const w=document.createElement('div');w.innerHTML=cap9('Export · ∮ and Σ in the legend, set in LaTeX')+`<img src="export_legend9.png" style="width:412px;height:412px;border-radius:8px;box-shadow:0 10px 30px rgba(0,0,0,.25)">`;document.getElementById('sheet').appendChild(w)}
Promise.all(['GSF','Rob','CMR','CMI'].map(f=>document.fonts.load('400 20px '+f))).then(()=>{
 tabletBuilder('light');
 const br=document.createElement('div');br.style.cssText='width:100%;height:0';document.getElementById('sheet').appendChild(br);
 phoneBuilder('dark','Greek');phoneBuilder('light','Hebrew');phone3D9('light');lettersDialog('dark');stlDialog('light');export9();
 document.title='ready'});
