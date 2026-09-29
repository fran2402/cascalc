// A line-by-line port of MathView.kt's layout rules to SVG, so the preview
// shows exactly where the app puts every exponent, bar and bracket.
// Units are dp (1 CSS px = 1 dp; screenshots are taken at 2x).
(function(){
const cv=document.createElement('canvas').getContext('2d');
const scale=l=>l==0?1:l==1?.7:l==2?.56:.5;
const THIN='\u2009';
let ENV=null;
const em=l=>ENV.size*scale(l);
const mathAxis=l=>em(l)*.28;
const R=Math.round, max=Math.max;
const esc=s=>s.replace(/&/g,'&amp;').replace(/</g,'&lt;');

function runs(text){const out=[];for(const ch of text){const fb=!(GSF_HAS.has(ch.codePointAt(0))||/\s/.test(ch));const last=out[out.length-1];if(last&&last.fb==fb)last.s+=ch;else out.push({s:ch,fb})}return out}
// Font runs, as MathGlyphs (Computer Modern) / GlyphFallback (Google Sans Flex) do in the app.
function cmRuns(text,italic){const out=[];for(const ch of text){const cp=ch.codePointAt(0);
  const mainHas=italic?CMI_HAS.has(cp):CMR_HAS.has(cp),otherHas=italic?CMR_HAS.has(cp):CMI_HAS.has(cp);
  const fam=/\s/.test(ch)||mainHas?(italic?'CMI':'CMR'):otherHas?(italic?'CMR':'CMI'):'Rob';
  const last=out[out.length-1];if(last&&last.fam==fam)last.s+=ch;else out.push({s:ch,fam})}return out}
// Google Sans Flex, then Roboto, then Computer Modern (GlyphFallback in the app).
function gsRuns(text){const out=[];for(const ch of text){const cp=ch.codePointAt(0);
  const fam=/\s/.test(ch)||GSF_HAS.has(cp)?'GSF':ROB_HAS.has(cp)?'Rob':'CMR';
  const last=out[out.length-1];if(last&&last.fam==fam)last.s+=ch;else out.push({s:ch,fam})}return out}
function mathText(text,l,{italic=false,color=null}={}){
  const px=em(l);let w=0;const rs=ENV.cm?cmRuns(text,italic):gsRuns(text);
  const base=ENV.cm?(italic?'CMI':'CMR'):'GSF';
  cv.font=`400 ${px}px ${base}`;const m=cv.measureText('Hg');const asc=m.fontBoundingBoxAscent,desc=m.fontBoundingBoxDescent;
  rs.forEach(r=>{cv.font=`400 ${px}px ${r.fam}`;r.w=cv.measureText(r.s).width;w+=r.w});
  return {w:R(w),h:R(asc+desc),a:R(asc),paint(x,y,o){let xx=x;rs.forEach(r=>{
    // Draw each run without its leading/trailing spaces (SVG would collapse them), advancing by the measured widths.
    cv.font=`400 ${px}px ${r.fam}`;const lead=r.s.length-r.s.trimStart().length;const t=r.s.trim();
    const dx=lead?cv.measureText(r.s.slice(0,lead)).width:0;
    const style=r.fam=='GSF'?`font-family:GSF;font-variation-settings:'ROND' 100${italic?';font-style:oblique 10deg':''}`:`font-family:${r.fam}`;
    if(t)o.push(`<text x="${xx+dx}" y="${y+R(asc)}" font-size="${px}" style="${style}" fill="${color||ENV.color}">${esc(t)}</text>`);xx+=r.w})}};
}
function axisRow(bs,l){
  const asc=max(...bs.map(b=>b.a),R(em(l)*.7)),desc=max(...bs.map(b=>b.h-b.a),R(em(l)*.22));
  return {w:bs.reduce((s,b)=>s+b.w,0),h:asc+desc,a:asc,paint(x,y,o){let xx=x;bs.forEach(b=>{b.paint(xx,y+asc-b.a,o);xx+=b.w})}};
}
function stroke(d,sw,color){return `<path d="${d}" fill="none" stroke="${color||ENV.color}" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round"/>`}

// ---- rows
function rowBox(row,l){
  const active=ENV.cursorRow===row;const e=em(l);
  const parts=[];
  if(row.length==0&&row.parent) parts.push({b:placeholder(row,l,active)});
  row.forEach((n,i)=>parts.push(nodeBox(n,row,i,l)));
  const P=parts.map(p=>p.b?p:{b:p});
  // pass 1
  const axes=P.map(p=>p.b.paren?0:p.b.a);
  // Same rule as RowView in MathView.kt.
  const symIdx=P.map((p,k)=>p.b.sym?k:-1).filter(k=>k>=0);
  const textAsc=symIdx.length?max(...symIdx.map(k=>axes[k])):R(e*.78);
  const baseIdx=P.map((p,k)=>(!p.b.pow&&!p.b.paren)?k:-1).filter(k=>k>=0);
  const baseAsc=baseIdx.length?max(...baseIdx.map(k=>axes[k])):textAsc;
  P.forEach((p,k)=>{ if(p.b.pow&&k>0){const prevAsc=P[k-1].b.paren?max(baseAsc,textAsc):axes[k-1];const extra=prevAsc-textAsc;if(extra>0)axes[k]+=extra} });
  let asc=R(e*.78),desc=R(e*.24);
  P.forEach((p,k)=>{if(!p.b.paren){asc=max(asc,axes[k]);desc=max(desc,p.b.h-axes[k])}});
  // Brackets sized pair by pair to what's between them, inner pairs first (as RowView in MathView.kt).
  const off=(row.length==0&&row.parent)?1:0,br=k=>(row[k-off]||{}).s;
  const partner=P.map(()=>-1),st=[];
  P.forEach((p,k)=>{if(!p.b.paren)return;const t=br(k);if('([{'.includes(t))st.push(k);else if(')]}'.includes(t)&&st.length){const j=st.pop();partner[j]=k;partner[k]=j}});
  const span=k=>{const q=partner[k];if(q>=0)return [Math.min(k,q)+1,Math.max(k,q)];return '([{'.includes(br(k))?[k+1,P.length]:[0,k]};
  const done=P.map(()=>false);
  P.map((p,k)=>k).filter(k=>P[k].b.paren).sort((a,b)=>{const x=span(a),y=span(b);return (x[1]-x[0])-(y[1]-y[0])}).forEach(k=>{
    if(done[k])return;let a=R(e*.78),d=R(e*.24);const [s0,s1]=span(k);
    for(let j=s0;j<s1;j++){if(P[j].b.paren){if(done[j]){const pad=R(e*.06);a=max(a,axes[j]+pad);d=max(d,P[j].b.h-axes[j]+pad)}}else{a=max(a,axes[j]);d=max(d,P[j].b.h-axes[j])}}
    [k,partner[k]].filter(q=>q>=0).forEach(q=>{P[q].b=P[q].b.make(R(e*.34),a+d);axes[q]=a;done[q]=true})});
  P.forEach((p,k)=>{if(p.b.paren){asc=max(asc,axes[k]);desc=max(desc,p.b.h-axes[k])}});
  const w=P.reduce((s,p)=>s+p.b.w,0);
  return {w,h:asc+desc,a:asc,paint(x,y,o){
    let xx=x,cx=null;const off=(row.length==0&&row.parent)?1:0;
    P.forEach((p,k)=>{if(active&&k-off==ENV.cursorIndex&&row.length)cx=xx;p.b.paint(xx,y+asc-axes[k],o);xx+=p.b.w});
    if(active){if(row.length&&ENV.cursorIndex>=row.length)cx=xx;if(!row.length)cx=row.parent?x+R(e*.12):x+2;
      const ch=max(asc+desc,R(e*1.05));o.push(`<rect x="${cx-1}" y="${y+(asc+desc-ch)/2}" width="2" height="${ch}" fill="${ENV.accent}"/>`)}
  }};
}
function placeholder(row,l,active){
  const e=em(l),w=R(e*(ENV.dots?.36:.56)),h=R(e*.7),gap=R(e*.06);
  return {w:w+2*gap,h,a:h,paint(x,y,o){const c=active?ENV.accent:ENV.faint;
    if(ENV.dots)o.push(`<circle cx="${x+gap+w/2}" cy="${y+h*.62}" r="${w*.2}" fill="${c}"/>`);
    else o.push(`<rect x="${x+gap+.75}" y="${y+.75}" width="${w-1.5}" height="${h-1.5}" rx="${w*.12}" fill="none" stroke="${c}" stroke-width="1.5"/>`)}};
}
const SPACED=new Set(['+','−','×','=','mod','<','>','≤','≥','or',':=','≠','≈','∈','→','↦','±']);
function nodeBox(n,row,i,l){
  switch(n.t){
    case 'sym':{const t=n.s;
      if(t=='('||t==')')return {paren:true,make:(w,h)=>delim('paren',t=='(',l,w,h)};
      if(t==THIN){const w=R(em(l)*.2);return {w,h:0,a:0,paint(){}}}
      const spaced=SPACED.has(t)&&l==0&&!(t=='−'&&i==0);
      if(/^C[0-9]+$/.test(t)){const b=scripts(l,mathText('C',l,{italic:true}),mathText(t.slice(1),l+1),null);b.sym=true;return b}
      // Same rules as SymView: variables (and y′) italic.
      if(t=='ans'){const b=mathText('Ans',l);b.sym=true;return b}
      // A symbol from the symbol builder (CustomSymbolView in MathView.kt): accent above the letter, then scripts.
      if(t[0]=='\uE000'){const [base,acc,sub,sup]=t.slice(1).split('\u001F');
        const GL={Dot:'˙',DoubleDot:'¨',Hat:'ˆ',Tilde:'˜',Bar:'¯',Vector:'→',Check:'ˇ',Breve:'˘',Acute:'´',Grave:'`'};
        const it=base.length==1&&/[a-zA-Zα-ω]/.test(base);let core=mathText(base,l,{italic:it});
        if(acc){const vec=acc=='Vector',b0=core,a1=mathText(GL[acc],l),a0=vec?{w:a1.w,h:a1.h,a:a1.a,paint(x,y,o){const cx=x+a1.w/2,cy=y+a1.h/2;o.push(`<g transform="translate(${cx} ${cy}) scale(.7) translate(${-cx} ${-cy})">`);a1.paint(x,y,o);o.push('</g>')}}:a1,ov=R(a1.h*.55+(!vec&&'acegmnopqrsuvwxyzαγεηικμνοπρστυφχψω'.includes(base)?em(l)*.22:0)),top=Math.max(0,a0.h-ov),w=Math.max(b0.w,a0.w),sh=it?R(em(l)*.08):0;
          core={w,h:top+b0.h,a:top+b0.a,paint(x,y,o){b0.paint(x+(w-b0.w)/2,y+top,o);a0.paint(x+(w-a0.w)/2+sh,y,o)}}}
        const row=s=>[...s].map(c=>({t:'sym',s:c}));
        const b=(sub||sup)?scripts(l,core,sub?rowBox(row(sub),l+1):null,sup?rowBox(row(sup),l+1):null):core;b.sym=true;return b}
      // An upright letter from LaTeX (\mathrm{m}), marked with U+2060.
      if(t.length==2&&t[0]=='\u2060'){const b=mathText(t[1],l);b.sym=true;return b}
      // Primes raised and small in the maths display (SymView in MathView.kt).
      if(ENV.cm&&/^′+$/.test(t)){const b=scripts(l,{w:0,h:0,a:0,paint(){}},null,mathText(t,l+1));b.sym=true;return b}
      if(ENV.cm&&/^[a-zA-Z]′+$/.test(t)){const b=scripts(l,mathText(t[0],l,{italic:true}),null,mathText(t.slice(1),l+1));b.sym=true;return b}
      const italic=/^[a-zA-Zα-ωπ][′]*$/.test(t)&&t!='T'&&!(t=='d'&&!ENV.cm);
      const tb=mathText(t,l,{italic}),pad=spaced?R(em(l)*.16):0,after=t==','?R(em(l)*.25):0;const b={w:tb.w+2*pad+after,h:tb.h,a:tb.a,sym:true,paint(x,y,o){tb.paint(x+pad,y,o)}};return b;}
    case 'const':{
      // Pieces as in ConstView (Constants.kt): text with subscript/superscript; a following power stacks on the last subscript.
      const k=(typeof CONSTS_ALL!=='undefined'&&CONSTS_ALL.find(c=>c.id==n.id))||{p:[{t:n.id,sub:'',sup:'',it:false}]};
      const nx=row[i+1],last=k.p[k.p.length-1],pw=nx&&nx.t=='pow'&&last.sub&&!last.sup;
      const parts=k.p.map((p,j)=>scripts(l,mathText(p.t,l,{italic:p.it}),p.sub?mathText(p.sub,l+1):null,(j==k.p.length-1&&pw)?rowBox(nx.exp,l+1):(p.sup?mathText(p.sup,l+1):null)));
      if(nx&&nx.t=='pow'&&last.sup){const f=fenced('paren',l,axisRow(parts,l));f.sym=true;return f}
      const b=axisRow(parts,l);b.sym=true;return b}
    case 'frac':{const cl=l==0?0:l+1;return frac(l,true,rowBox(n.num,cl),rowBox(n.den,cl))}
    case 'pow':{const pv=row[i-1];const pk=pv&&pv.t=='const'&&typeof CONSTS_ALL!=='undefined'&&CONSTS_ALL.find(c=>c.id==pv.id);const lp=pk&&pk.p[pk.p.length-1];
      if(lp&&lp.sub&&!lp.sup){return {w:0,h:0,a:0,pow:true,paint(){}}}const b=powBox(n,l);b.pow=true;return b}
    case 'sqrt':return radical(l,null,rowBox(n.arg,l));
    case 'root':return radical(l,rowBox(n.idx,l+2),rowBox(n.arg,l));
    case 'func':return funcBox(n,l);
    case 'big':return bigOp(n,l);
    case 'int':return integral(n,l);
    case 'diff':return deriv(n,l);
    // A base with a subscript and/or superscript (Scripted in MathTree.kt).
    case 'scr':return scripts(l,rowBox(n.base,l),n.sub.length?rowBox(n.sub,l+1):null,n.sup.length?rowBox(n.sup,l+1):null);
    case 'binom':{const cl=l==0?0:l+1;return fenced('paren',l,frac(l,false,rowBox(n.n,cl),rowBox(n.k,cl)))}
    case 'mat':return fenced('bracket',l,matrix(n,l));
  }
}
function scripts(l,b,sb,sp){
  const e=em(l),subDrop=R(e*.22),supRise=R(e*.42);
  const asc=max(b.a,sp?supRise+sp.a:0,sb?sb.a-subDrop:0);
  const desc=max(b.h-b.a,sb?subDrop+sb.h-sb.a:0);
  const w=b.w+max(sb?sb.w:0,sp?sp.w:0);
  return {w,h:asc+desc,a:asc,paint(x,y,o){b.paint(x,y+asc-b.a,o);if(sb)sb.paint(x+b.w,y+asc+subDrop-sb.a,o);if(sp)sp.paint(x+b.w,y+asc-supRise-sp.a,o)}};
}
function powBox(p,l){
  const ex=rowBox(p.exp,l+1),raise=R(em(l)*.38),a=ex.a+raise,pad=R(em(l)*.04);
  return {w:ex.w+pad,h:max(ex.h,a),a,paint(x,y,o){ex.paint(x+pad,y,o)}};
}
function frac(l,line,n,d){
  const e=em(l),t=max(1,R(e*.055)),gap=R(e*(line?.1:.04)),pad=R(e*.12),w=max(n.w,d.w)+2*pad;
  const lineY=n.h+gap,a=lineY+Math.floor(t/2)+R(mathAxis(l));
  return {w,h:lineY+t+gap+d.h,a,paint(x,y,o){n.paint(x+Math.floor((w-n.w)/2),y,o);if(line)o.push(`<rect x="${x}" y="${y+lineY}" width="${w}" height="${t}" fill="${ENV.color}"/>`);d.paint(x+Math.floor((w-d.w)/2),y+lineY+t+gap,o)}};
}
function radical(l,idx,b){
  const e=em(l),sign=R(e*.5),top=R(e*.16),h=b.h+top;
  const shift=max(0,(idx?idx.w:0)-R(sign*.45));
  const w=shift+sign+b.w+R(e*.06);
  const idxLift=idx?max(0,idx.h-R(h*.42)):0;
  return {w,h:h+idxLift,a:b.a+top+idxLift,paint(x,y,o){
    const cx=x+shift,cy=y+idxLift,cw=w-shift,sw=max(1,h*.045),tick=h*.62,s=e*.5;
    o.push(stroke(`M${cx+sw} ${cy+tick} L${cx+s*.3} ${cy+tick-s*.12} L${cx+s*.62} ${cy+h-sw} L${cx+s} ${cy+sw/2} L${cx+cw} ${cy+sw/2}`,sw));
    b.paint(cx+sign,cy+top,o);if(idx)idx.paint(x+max(0,shift+R(sign*.45)-idx.w),y,o)}};
}
function funcBox(f,l){
  if(f.name=='abs')return fenced('bar',l,rowBox(f.args[0],l));
  if(f.name=='floor')return fenced('floor',l,rowBox(f.args[0],l));
  if(f.name=='ceil')return fenced('ceil',l,rowBox(f.args[0],l));
  if(f.name=='round')return fenced('floor',l,rowBox(f.args[0],l),'ceil');
  if(f.name=='frac')return fenced('brace',l,rowBox(f.args[0],l));
  const argsBox=()=>{const inner=[];f.args.forEach((a,i)=>{if(i)inner.push(mathText(', ',l));inner.push(rowBox(a,l))});return fenced('paren',l,axisRow(inner,l))};
  if(f.name=='residue')return axisRow([scripts(l,mathText('Res',l),rowBox(f.args[1],l+1),null),fenced('paren',l,rowBox(f.args[0],l))],l);
  if(f.name=='invnorm')return axisRow([scripts(l,mathText('Φ',l),null,mathText('−1',l+1)),argsBox()],l);
  if(f.name=='var')return axisRow([scripts(l,mathText('s',l,{italic:true}),null,mathText('2',l+1)),argsBox()],l);
  if(f.name=='binomcdf')return axisRow([scripts(l,mathText('Bin',l),mathText('≤',l+1),null),argsBox()],l);
  if(f.name=='contour')return contour(f,l);
  if(f.name=='lim'){const top=mathText('lim',l),bot=rowBox(f.args[1],l+1),w=max(top.w,bot.w);
    const blk={w,h:top.h+bot.h,a:top.a,paint(x,y,o){top.paint(x+Math.floor((w-top.w)/2),y,o);bot.paint(x+Math.floor((w-bot.w)/2),y+top.h-R(em(l)*.12),o)}};
    // A body that's already one bracketed group (maybe with powers) gets no extra brackets.
    const bd=f.args[0];let bracketed=false;if(bd.length&&bd[0].s=='('){let d=0;for(let k=0;k<bd.length;k++){if(bd[k].s=='(')d++;if(bd[k].s==')')d--;if(d==0){bracketed=bd.slice(k+1).every(n=>n.t=='pow');break}}}
    return axisRow([blk,mathText('\u2009',l),bracketed?rowBox(bd,l):fenced('paren',l,rowBox(bd,l))],l)}
  if(f.name=='log')return axisRow([scripts(l,mathText('log',l),rowBox(f.args[0],l+1),null),fenced('paren',l,rowBox(f.args[1],l))],l);
  if(f.args.length==1&&f.args[0].length==1&&(f.args[0][0].t=='mat'||(f.args[0][0].t=='func'&&f.args[0][0].name=='abs')))return axisRow([mathText(f.name,l),rowBox(f.args[0],l)],l);
  const inner=[];f.args.forEach((a,i)=>{if(i)inner.push(mathText(', ',l));inner.push(rowBox(a,l))});
  return axisRow([mathText(({gamma:'Γ',zeta:'ζ',grad:'∇',div:'∇·',curl:'∇×',laplacian:'∇²',jacobian:'J',hessian:'H',perm:'P',normpdf:'φ',normcdf:'Φ',binompdf:'Bin',poissonpdf:'Pois',total:'Σ',sd:'s',psd:'σ',median:'med'})[f.name]||f.name,l),fenced('paren',l,axisRow(inner,l))],l);
}
function bigOp(b,l){
  const e=em(l),up=rowBox(b.up,l+1),lo=axisRow([rowBox(b.v,l+1),mathText('=',l+1),rowBox(b.lo,l+1)],l+1);
  const symW=R(e*.95),symH=R(e*1.15),g=R(e*.08),w=max(up.w,symW,lo.w)+R(e*.15),symY=up.h+g;
  const block={w,h:symY+symH+g+lo.h,a:symY+Math.floor(symH/2)+R(mathAxis(l)),paint(x,y,o){
    up.paint(x+Math.floor((w-up.w)/2),y,o);
    const sx=x+Math.floor((w-symW)/2),sy=y+symY,sw=max(1.2,symH*.07),W=symW,H=symH;
    const d=b.kind=='sum'?`M${sx+W*.92} ${sy+H*.12} L${sx+W*.9} ${sy+sw/2} L${sx+W*.08} ${sy+sw/2} L${sx+W*.55} ${sy+H*.5} L${sx+W*.08} ${sy+H-sw/2} L${sx+W*.9} ${sy+H-sw/2} L${sx+W*.92} ${sy+H*.88}`
      :`M${sx+W*.04} ${sy+sw/2} L${sx+W*.96} ${sy+sw/2} M${sx+W*.22} ${sy+sw/2} L${sx+W*.22} ${sy+H} M${sx+W*.78} ${sy+sw/2} L${sx+W*.78} ${sy+H}`;
    o.push(`<path d="${d}" fill="none" stroke="${ENV.color}" stroke-width="${sw}" stroke-linejoin="round"/>`);
    lo.paint(x+Math.floor((w-lo.w)/2),sy+symH+g,o)}};
  return axisRow([block,rowBox(b.body,l)],l);
}
// Same as cursorInside in MathView.kt.
function cursorInside(node){let r=ENV.cursorRow;while(r){const owner=r.parent;if(!owner)return false;if(owner===node)return true;r=owner.parentRow}return false}
// ∮: the ∫ path with a ring, the circle underneath, then f dz (ContourView in MathView.kt).
function contour(f,l){
  const e=em(l),symH=R(e*1.6),symW=R(symH*0.4),lo=rowBox(f.args[1],l+1),loX=R(symW*.45);
  const h=symH+max(0,lo.h-R(symH*.25)),w=max(symW,loX+lo.w)+R(e*.1),ax=R(symH/2+e*.25);
  const inner=f.args[1][0],v=(inner&&inner.t=='func'&&inner.args[0].find(s=>s.t=='sym'&&/^[a-zA-Z]$/.test(s.s)))||{s:'z'};
  const block={w,h,a:ax,paint(x,y,o){
    o.push(opGlyph('∮',x,y,symH));
    lo.paint(x+loX,y+h-lo.h,o)}};
  return axisRow([block,rowBox(f.args[0],l),mathText('\u00a0d',l),mathText(v.s,l,{italic:true})],l)}
// TeX's display-size ∫ and ∮ from MathJax_Size2, scaled so the ink fills W × H (OperatorGlyph in MathView.kt).
function opGlyph(ch,x,y,H){const ts=H/2.222;return `<text x="${x-0.055*ts}" y="${y+1.36*ts}" font-size="${ts}" style="font-family:CMS2" fill="${ENV.color}">${ch}</text>`}
function integral(n,l){
  const show=n.lo.length||n.up.length||cursorInside(n);
  const none={w:0,h:0,a:0,paint(){}};
  const e=em(l),symH=R(e*1.6),symW=R(symH*0.4),up=show?rowBox(n.up,l+1):none,lo=show?rowBox(n.lo,l+1):none;
  const upX=R(symW*1.15),loX=R(symW*.55),top=max(0,up.h-R(symH*.3)),h=top+symH+max(0,lo.h-R(symH*.3));
  const w=max(upX+up.w,loX+lo.w)+R(e*.1);
  const block={w,h,a:top+Math.floor(symH/2)+R(mathAxis(l)),paint(x,y,o){
    const sx=x,sy=y+top,W=symW,H=symH,sw=max(1.2,W*.13);
    o.push(opGlyph('∫',sx,sy,H));
    up.paint(x+upX,y,o);lo.paint(x+loX,y+h-lo.h,o)}};
  return axisRow([block,rowBox(n.body,l),mathText('\u00a0d',l),varBox(n.v,l,cursorInside(n))],l);
}
function varBox(v,l,editing){const b=rowBox(v,l);if(!editing)return b;
  return {w:b.w,h:b.h,a:b.a,paint(x,y,o){o.push(`<rect x="${x}" y="${y}" width="${b.w}" height="${b.h}" rx="3" fill="${ENV.accent}" fill-opacity=".16"/>`);b.paint(x,y,o)}}}
function deriv(d,l){
  const ord=d.ord||[];const showOrd=ord.length||cursorInside(d);
  const ordTxt=ord.map(s=>s.s).join('')||'n';
  const dd=d.partial?'∂':'d';
  const num=showOrd?scripts(l,mathText(dd,l),null,rowBox(ord,l+1)):mathText(dd,l);
  const ed=cursorInside(d);const den=showOrd?axisRow([mathText(dd,l),scripts(l,varBox(d.v,l,ed),null,mathText(ordTxt,l+1))],l):axisRow([mathText(dd,l),varBox(d.v,l,ed)],l);
  const f=frac(l,true,num,den);
  const bar=(()=>{const e=em(l),h=R(e*1.4),w=R(e*.22);const b=delim('bar',false,l,w,h);return {w,h,a:R(h*.72),paint:b.paint}})();
  const vname=(d.v.map(s=>s.s).join('')||'x');
  const sub=axisRow([mathText(vname,l+1,{italic:true,color:ENV.faint}),mathText('=',l+1),rowBox(d.at,l+1)],l+1);
  const parts=[f,fenced('paren',l,rowBox(d.body,l))];
  if(d.at.length||cursorInside(d))parts.push(scripts(l,bar,sub,null));
  return axisRow(parts,l);
}
function matrix(m,l){
  const e=em(l),ps=m.cells.map(c=>rowBox(c,l));
  const colW=[...Array(m.c)].map((_,c)=>max(...[...Array(m.r)].map((_,r)=>ps[r*m.c+c].w)));
  const ra=[...Array(m.r)].map((_,r)=>max(...[...Array(m.c)].map((_,c)=>ps[r*m.c+c].a)));
  const rd=[...Array(m.r)].map((_,r)=>max(...[...Array(m.c)].map((_,c)=>ps[r*m.c+c].h-ps[r*m.c+c].a)));
  const hg=R(e*.7),vg=R(e*.22),w=colW.reduce((a,b)=>a+b,0)+hg*(m.c-1)+R(e*.2);
  const h=ra.reduce((s,v,i)=>s+v+rd[i],0)+vg*(m.r-1);
  return {w,h,a:Math.floor(h/2)+R(mathAxis(l)),paint(x,y,o){let yy=y;for(let r=0;r<m.r;r++){let xx=x+R(e*.1);for(let c=0;c<m.c;c++){const p=ps[r*m.c+c];p.paint(xx+Math.floor((colW[c]-p.w)/2),yy+ra[r]-p.a,o);xx+=colW[c]+hg}yy+=ra[r]+rd[r]+vg}}};
}
function fenced(kind,l,c,right){
  const e=em(l),pad=R(e*.06),h=max(c.h,R(e))+2*pad,dw=R(e*(kind=='bar'?.22:.34)),top=Math.floor((h-c.h)/2);
  const L=delim(kind,true,l,dw,h),Rr=delim(right||kind,false,l,dw,h);
  return {w:dw*2+c.w,h,a:c.a+top,paint(x,y,o){L.paint(x,y,o);c.paint(x+dw,y+top,o);Rr.paint(x+dw+c.w,y,o)}};
}
function delim(kind,left,l,w,h){
  return {w,h,a:h,paint(x,y,o){
    const sw=max(1,em(l)*.065),inner=x+(left?w*.8:w*.2),outer=x+(left?w*.25:w*.75);let d;
    if(kind=='paren'){const inset=sw+em(l)*.1,bulge=x+(left?w*.02:w*.98);d=`M${inner} ${y+inset} Q${bulge} ${y+h/2} ${inner} ${y+h-inset}`}
    else if(kind=='bracket')d=`M${inner} ${y+sw/2} L${outer} ${y+sw/2} L${outer} ${y+h-sw/2} L${inner} ${y+h-sw/2}`;
    else if(kind=='bar')d=`M${x+w/2} ${y+sw} L${x+w/2} ${y+h-sw}`;
    else if(kind=='brace'){const tip=x+(left?w*.12:w*.88),mid=x+w*.5,ins=sw+em(l)*.06;
      d=`M${inner} ${y+ins} C${mid} ${y+ins} ${mid} ${y+h*.38} ${mid} ${y+h*.42} Q${mid} ${y+h*.5} ${tip} ${y+h*.5} Q${mid} ${y+h*.5} ${mid} ${y+h*.58} C${mid} ${y+h*.62} ${mid} ${y+h-ins} ${inner} ${y+h-ins}`}
    else if(kind=='floor')d=`M${outer} ${y+sw/2} L${outer} ${y+h-sw/2} L${inner} ${y+h-sw/2}`;
    else d=`M${inner} ${y+sw/2} L${outer} ${y+sw/2} L${outer} ${y+h-sw/2}`;
    o.push(stroke(d,sw))}};
}
const CONSTS={c0:{sym:'c',sub:'0'},G:{sym:'G'},h:{sym:'h'},qe:{sym:'e',sup:'−'},kB:{sym:'k',sub:'B'},me:{sym:'m',sub:'e'},mp:{sym:'m',sub:'p'},alpha:{sym:'α'},NA:{sym:'N',sub:'A'},R:{sym:'R'},g0:{sym:'g',sub:'0'},eps0:{sym:'ε',sub:'0'}};

// ---- tree builders (mirror MathTree.kt)
function link(row,parent){row.parent=parent;row.forEach(n=>{n.parentRow=row;['num','den','exp','arg','idx','v','lo','up','body','at','n','k','ord','base','sub','sup'].forEach(key=>{if(Array.isArray(n[key]))link(n[key],n)});(n.args||[]).forEach(a=>link(a,n));(n.cells||[]).forEach(c=>link(c,n))});return row}
function decode(text){let i=0;
  const until=c=>{const st=i;while(text[i]!=c)i++;const r=text.slice(st,i);i++;return r};
  function row(){const r=[];while(i<text.length&&text[i]!='|'&&text[i]!='}'){
    if(text[i]=="'"){i++;let s='';while(text[i]!=';'){if(text[i]=='\\')i++;s+=text[i++]}i++;r.push({t:'sym',s})}
    else if(text[i]=='$'){i++;r.push({t:'const',id:until(';')})}
    else{const kind=until('{');const sl=[];while(true){sl.push(row());if(text[i++]=='}')break}
      if(kind=='frac')r.push({t:'frac',num:sl[0],den:sl[1]});
      else if(kind=='pow')r.push({t:'pow',exp:sl[0]});
      else if(kind=='sqrt')r.push({t:'sqrt',arg:sl[0]});
      else if(kind=='root')r.push({t:'root',idx:sl[0],arg:sl[1]});
      else if(kind.startsWith('fn:'))r.push({t:'func',name:kind.slice(3),args:sl});
      else if(kind=='sum'||kind=='prod')r.push({t:'big',kind:kind=='sum'?'sum':'prod',v:sl[0],lo:sl[1],up:sl[2],body:sl[3]});
      else if(kind=='int')r.push({t:'int',lo:sl[0],up:sl[1],body:sl[2],v:sl[3]});
      else if(kind=='diff'||kind=='pdiff')r.push({t:'diff',v:sl[0],body:sl[1],at:sl[2],ord:sl[3]||[],partial:kind=='pdiff'});
      else if(kind=='binom')r.push({t:'binom',n:sl[0],k:sl[1]});
      else if(kind=='scr')r.push({t:'scr',base:sl[0],sub:sl[1],sup:sl[2]});
      else if(kind.startsWith('mat:')||kind.startsWith('gmat:')){const [rr,cc]=kind.split(':')[1].split('x').map(Number);r.push({t:'mat',r:rr,c:cc,cells:sl})}
    }}return r}
  return row()}
window.MT={decode,
  R:(...xs)=>{const r=[];xs.forEach(x=>typeof x=='string'?[...x].forEach(c=>r.push({t:'sym',s:c})):r.push(x));return r},
  S:s=>({t:'sym',s}),
  frac:(num,den)=>({t:'frac',num,den}), pow:exp=>({t:'pow',exp}), sqrt:arg=>({t:'sqrt',arg}), root:(idx,arg)=>({t:'root',idx,arg}),
  func:(name,...args)=>({t:'func',name,args}), sum:(v,lo,up,body)=>({t:'big',kind:'sum',v,lo,up,body}),
  prod:(v,lo,up,body)=>({t:'big',kind:'prod',v,lo,up,body}), int:(lo,up,body,v)=>({t:'int',lo,up,body,v}),
  diff:(v,body,at)=>({t:'diff',v,body,at}), binom:(n,k)=>({t:'binom',n,k}), mat:(r,c,cells)=>({t:'mat',r,c,cells:cells||[...Array(r*c)].map(()=>[])}),
  cst:id=>({t:'const',id}),
  /** Renders a row to an inline SVG string. */
  render(row,{size,color,accent,faint,dots=false,cm=true,cursorRow=null,cursorIndex=0}){
    link(row,null);
    ENV={size,color,accent:accent||color,faint:faint||color,dots,cm,cursorRow,cursorIndex};
    const b=rowBox(row,0),o=[];b.paint(0,0,o);
    return `<svg class="mv" width="${b.w}" height="${b.h}" viewBox="0 0 ${b.w} ${b.h}" style="overflow:visible;vertical-align:${-(b.h-b.a)}px">${o.join('')}</svg>`;
  },
  THIN,
};
})();
