import { useState } from 'react'
import { ApiFailure, searchPlacesByProvider, searchAreaConsensus } from './api/client'
import type { PlaceCandidate, PlaceSearchResult, AreaConsensus } from './api/client'
import './placeSearchDemo.css'

type PlaceSearchProvider='consensus'|'geoapify'|'locationiq'|'arcgis'|'tomtom'

function distanceMeters(a:PlaceCandidate,b:PlaceCandidate){
 if(a.lat==null||a.lon==null||b.lat==null||b.lon==null)return 0
 const rad=Math.PI/180, dLat=(b.lat-a.lat)*rad, dLon=(b.lon-a.lon)*rad
 const h=Math.sin(dLat/2)**2+Math.cos(a.lat*rad)*Math.cos(b.lat*rad)*Math.sin(dLon/2)**2
 return 2*6371008.8*Math.asin(Math.sqrt(Math.min(1,h)))
}

const providers:{id:PlaceSearchProvider;name:string;description:string}[]=[
 {id:'consensus',name:'三家联合区域评估',description:'同时查询 Geoapify、ArcGIS、TomTom（三个独立数据家族；Geoapify 属 OSM 家族、品牌识别强）；跨来源候选两两距离不超过 0.6 英里（约 966 米）。三家支持优先，两家支持也保留；用于附近区域通勤初筛。'},
 {id:'locationiq',name:'LocationIQ',description:'英国邮编走专用 postcode 搜索，其余输入走 Forward Geocoding；需要后端配置 LOCATIONIQ_API_KEY。'},
 {id:'arcgis',name:'ArcGIS 英国门店与地址搜索',description:'直接调用 World Geocoding 公开端点作非持久化对比测试；原样查询、保留上游排序。可比较 marks and spencer woolwich 与 M&S Woolwich Simply Food；首条结果不保证正确。'},
 {id:'tomtom',name:'TomTom Places Search',description:'调用 TomTom Orbis Places Discover，查询英国地址和商业地点；每月 5,000 次免费请求，无需信用卡。需要 TOMTOM_API_KEY。'},
 {id:'geoapify',name:'Geoapify 英国地点搜索',description:'调用 Geocoding Search（filter=countrycode:gb）；免费 3,000 次/天、无需信用卡。带品牌实体归一化，能识别 M&S 这类缩写；返回多家同品牌门店，需结合街道/区域词或 postcode 区分。需要 GEOAPIFY_API_KEY。'},
]
export default function PlaceSearchDemoPage(){
 const [query,setQuery]=useState('M&S Woolwich Simply Food'),[provider,setProvider]=useState<PlaceSearchProvider>('consensus')
 const [consensus,setConsensus]=useState<AreaConsensus|null>(null)
 const [result,setResult]=useState<PlaceSearchResult|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[queryUsed,setQueryUsed]=useState(''),[selected,setSelected]=useState<PlaceCandidate|null>(null)
 async function run(e:React.FormEvent){e.preventDefault();const term=query.trim();if(term.length<3)return;setBusy(true);setConsensus(null);setError('');setResult(null);setSelected(null);setQueryUsed(term)
  try{if(provider==='consensus'){
    const assessment=await searchAreaConsensus(term);setConsensus(assessment)
    const candidates=assessment.providers.flatMap(p=>p.result.candidates)
    setResult({status:candidates.length?'matches':assessment.providers.every(p=>p.result.status==='unavailable')?'unavailable':'no-matches',candidates,message:'三家原始候选：以下结果未按区域共识过滤，便于核对。',attribution:assessment.providers.map(p=>p.result.attribution).join('；')})
   }else setResult(await searchPlacesByProvider(provider,term))}catch(e){setError(e instanceof ApiFailure?e.payload.messageZh??e.payload.messageEn??e.payload.code??'请求失败':'无法连接本地接口。请确认后端已启动。')}
  finally{setBusy(false)}
 }
 const selectedProvider=providers.find(p=>p.id===provider)!
 const threeSourceAreas=consensus?.status==='ambiguous'?consensus.areas.filter(a=>a.support===3).reduce<typeof consensus.areas>((chosen,area)=>{
  if(chosen.every(existing=>distanceMeters(existing.representative,area.representative)>consensus.maxDistanceMeters))chosen.push(area)
  return chosen
 },[]):[]
 const multipleThreeSourceAreas=threeSourceAreas.length>=2
 return <main className="place-demo"><header><a href="/">Commute Fit</a><span>地点搜索实验室</span></header><section className="place-demo-hero"><p className="eyebrow">独立测试页面</p><h1>检查工作地点附近区域</h1><p>输入同一地点的不同写法，分别调用底层服务，比较是否能找到正确的门店地址和 postcode。</p></section>
  <form className="place-demo-form" onSubmit={run}><label>搜索内容<input autoFocus value={query} onChange={e=>setQuery(e.target.value)} placeholder="例如：M&S Woolwich Simply Food 或 SE18 6FL" /></label><p className="place-demo-input-hint">可输入品牌 + 门店名、单独门店名、区域、postcode 或完整地址。</p><label>选择要实际调用的接口<select disabled={busy} value={provider} onChange={e=>{setProvider(e.target.value as PlaceSearchProvider);setResult(null);setError('');setSelected(null);setConsensus(null);setQueryUsed('')}}>{providers.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select></label><p className="place-demo-input-hint">{selectedProvider.description}</p><button disabled={busy||query.trim().length<3}>{busy?'正在查询…':'调用接口'}</button></form>
  <aside className="place-demo-note"><strong>本次测试</strong><p>接口：{selectedProvider.name}　·　查询词：{queryUsed||query.trim()||'尚未查询'}</p></aside>
  {error&&<p role="alert" className="place-demo-error">{error}</p>}
  {consensus&&<section className="place-demo-results" aria-label="联合区域评估结果"><h2>{multipleThreeSourceAreas?'发现多个三源共识区域':consensus.status==='ambiguous'?'发现分店位置歧义':consensus.status==='text-match'?'找到文字匹配候选（没有多源共识）':consensus.status==='supported'?'已自动确定参考区域':'暂未确定参考区域'}</h2><p>{multipleThreeSourceAreas?'以下各区域均由三家来源支持，分别展示代表坐标，供后续通勤比较。':consensus.message}</p><p>区域内任意两点最大距离：0.6 英里（约 966 米）。这是近似工作区域，不代表已核实具体门店。</p>
   <div className="place-demo-list">{(multipleThreeSourceAreas?threeSourceAreas:consensus.areas.slice(0,1)).map((a,i)=><article className="place-demo-candidate" key={i}><h3>{multipleThreeSourceAreas?`共识区域 ${i+1}`:consensus.status==='supported'?'自动采用的区域':'最强共识候选（暂不自动采用）'} · {a.support} 家支持</h3><p>{a.sources.join(' + ')} · 最大相距 {Math.round(a.maxDistanceMeters)} 米</p><p>{a.representative.name} · {a.representative.postcode||'未提供邮编'}</p><p>{a.representative.address}</p><p>代表坐标：{a.representative.lat?.toFixed(5)}, {a.representative.lon?.toFixed(5)}</p><details><summary>查看此区域的 {a.members.length} 个来源坐标</summary>{a.members.map((m,j)=><p key={j}>{m.source}：{m.name} · {m.postcode||'无邮编'} · {m.lat?.toFixed(5)}, {m.lon?.toFixed(5)}</p>)}</details>{consensus.status==='supported'&&<p>后续通勤查询采用上述代表坐标。</p>}</article>)}</div>
   {consensus.status==='text-match'&&consensus.selectedCandidate&&<article className="place-demo-candidate"><h3>地址文字匹配候选（单来源/无坐标共识）</h3><p>{consensus.selectedCandidate.name} · {consensus.selectedCandidate.postcode||'未提供邮编'}</p><p>{consensus.selectedCandidate.address}</p><p>{consensus.selectedCandidate.source} · {consensus.selectedCandidate.lat?.toFixed(5)}, {consensus.selectedCandidate.lon?.toFixed(5)}</p><p>低置信度结果，仅用于这次 Demo 核对；不是多源确认。</p><button type="button" className="candidate-select" onClick={()=>setSelected(consensus.selectedCandidate!)}>标记此文字匹配候选</button></article>}
   <h3>三个来源的实际响应</h3>{consensus.providers.map(p=><p key={p.provider}><strong>{p.provider}</strong>：{p.result.status==='unavailable'?'服务不可用':p.result.status==='no-matches'?'无匹配':`${p.result.candidates.length} 条候选`} · {p.result.message}</p>)}
   {(consensus.corroborated??[]).length>0&&<details><summary>地址不完整、通过坐标补充的候选（{(consensus.corroborated??[]).length}）</summary>{(consensus.corroborated??[]).map((e,i)=><p key={i}>{e.candidate.source} · {e.candidate.name}：{e.reason}</p>)}</details>}
   <details><summary>查看未参与共识的候选（{consensus.excluded.length}）</summary><p>名称核对使用关键词与品牌别名规则，可能漏掉缩写、不同拼写；请结合原始结果确认。</p>{consensus.excluded.map((e,i)=><p key={i}>{e.candidate.source} · {e.candidate.name}：{e.reason}</p>)}</details>
  </section>}
  {result&&<section className="place-demo-results"><div className="place-demo-results-head"><div><p className="eyebrow">接口响应</p><h2>{result.candidates.length} 条候选结果</h2></div><span className={'place-demo-status '+result.status}>{result.status==='matches'?'有匹配':result.status==='no-matches'?'无匹配':'服务不可用'}</span></div><p>{result.message}</p>{!result.candidates.length&&<div className="place-demo-empty">没有返回候选。可以切换接口，或更改搜索词继续测试。</div>}
   <div className="place-demo-list">{result.candidates.map((c,i)=><article className="place-demo-candidate" key={c.id||i}><div className="candidate-top"><span className="candidate-index">{String(i+1).padStart(2,'0')}</span><div><h3>{c.name}</h3><p>{c.address||'接口未提供地址'}</p></div><span className={'precision precision-'+c.precision}>{c.precision}</span></div><dl><div><dt>Postcode</dt><dd>{c.postcode||'未提供'}</dd></div><div><dt>数据来源</dt><dd>{c.source}</dd></div><div><dt>可选择</dt><dd>{c.selectable?'可以':'不可以，需细化地点'}</dd></div><div><dt>坐标</dt><dd>{c.lat!=null&&c.lon!=null?`${c.lat.toFixed(5)}, ${c.lon.toFixed(5)}`:'未提供'}</dd></div></dl><button type="button" className="candidate-select" disabled={!c.selectable} onClick={()=>setSelected(c)}>{selected?.id===c.id?'已标记':'标记此结果'}</button></article>)}</div><small className="place-demo-attribution">{result.attribution}</small></section>}
  {selected&&<section className="place-demo-selected"><strong>已标记候选</strong><p>{selected.name}</p><p>{selected.postcode||selected.address||'没有 postcode 或街道地址'}</p><small>仅供测试记录，不保存位置，也不发起路线查询。</small></section>}
  <footer>测试页 · 使用本地后端地点搜索接口 · 不会保存搜索内容</footer>
 </main>
}
