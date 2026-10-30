import {useRef,useState} from 'react'
import dataset from './samples.json'
import scoredSummary from './summary.json'
import {searchAreaConsensus} from '../api/client'
import type {AreaConsensus} from '../api/client'

type Sample=typeof dataset.samples[number]
type Run={id:string;rule:string;query:string;startedAt:string;elapsedMs:number;data?:AreaConsensus;error?:string}
const ruleLabels:Record<string,string>={'raw-title-location':'品牌＋完整岗位标题＋城市','brand-area':'品牌＋简化分店名','brand-store-city':'品牌＋原分店名＋城市','brand-area-city':'品牌＋简化分店名＋城市'}
const KEY='commute-fit-location-benchmark-20261029'
const compact=(s:string)=>s.replace(/\b(simply food|foodhall|superstore|express|extra|local|store|stores)\b/gi,' ').replace(/\brd\b/gi,'Road').replace(/\bst\b/gi,'Street').replace(/\s+/g,' ').trim()
const rules=(s:Sample)=>[
 {rule:'raw-title-location',query:`${s.title.toLowerCase().includes(s.brand.toLowerCase())?'':s.brand+' '}${s.title} ${s.city}`.trim()},
 {rule:'brand-area',query:`${s.brand} ${compact(s.store)}`},
 {rule:'brand-store-city',query:`${s.brand} ${s.store} ${s.city}`},
 {rule:'brand-area-city',query:`${s.brand} ${compact(s.store)} ${s.city}`},
]
export default function LocationBenchmarkPage(){
 const [runs,setRuns]=useState<Run[]>(()=>{try{return JSON.parse(localStorage.getItem(KEY)||'[]')}catch{return []}})
 const [busy,setBusy]=useState(false),[current,setCurrent]=useState('尚未开始')
 const stop=useRef(false)
 const save=(r:Run[])=>{setRuns([...r]);localStorage.setItem(KEY,JSON.stringify(r))}
 async function start(){
  stop.current=false;setBusy(true)
  const all=[...runs];const cached=new Map(all.filter(r=>r.data).map(r=>[r.query,r.data!]))
  try{for(const sample of dataset.samples){for(const task of rules(sample)){
   if(stop.current)break
   if(all.some(r=>r.id===sample.id&&r.rule===task.rule))continue
   setCurrent(`${sample.id} ${sample.brand} ${sample.store} · ${task.rule}`)
   const startedAt=new Date().toISOString(),start=performance.now()
   let run:Run={id:sample.id,...task,startedAt,elapsedMs:0}
   try{const data=cached.get(task.query)??await searchAreaConsensus(task.query);cached.set(task.query,data);run={...run,data}}
   catch(e){run.error=e instanceof Error?e.message:'request failed'}
   run.elapsedMs=Math.round(performance.now()-start);all.push(run);save(all)
   await new Promise(resolve=>setTimeout(resolve,1100))
  }if(stop.current)break}}finally{setBusy(false);setCurrent(stop.current?'已停止，可继续':'本批执行完成')}
 }
 function download(){const blob=new Blob([JSON.stringify({dataset,rules:['raw-title-location','brand-area','brand-store-city','brand-area-city'],exportedAt:new Date().toISOString(),runs},null,2)],{type:'application/json'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='location-benchmark-results.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
 return <main style={{maxWidth:1100,margin:'32px auto',padding:24,fontFamily:'system-ui'}}><h1>50 岗位地点搜索规则对照</h1><p>50 个不同门店 · 7 个品牌 · 4 种规则 · 固定现有三源聚合器。含历史招聘页，测试地点识别，不代表岗位仍开放。</p><p>来源数吻合不等于选对门店；需对照独立地址或地图核验后再统计正确率。</p><h2>已完成的独立核验 · 2026-10-29</h2><p>按官方地址附近 0.6 英里核验，衡量区域可用性。含历史邮编及机场地图参考；不代表精确分店匹配。原始候选覆盖不等于自动选对。</p><table><thead><tr><th>搜索规则</th><th>区域符合</th><th>超出范围</th><th>未自动定位</th></tr></thead><tbody>{Object.entries(scoredSummary.rules).map(([rule,stat])=><tr key={rule}><td>{ruleLabels[rule]}</td><td>{(stat as Record<string,unknown>)["within-area"] as number??0}/50</td><td>{(stat as Record<string,unknown>)["outside-area"] as number??0}</td><td>{(stat as Record<string,unknown>)["no-consensus"] as number??0}</td></tr>)}</tbody></table><p>简化分店名＋城市：三源候选联合覆盖 48/50；当前自动采用 27 个区域符合、1 个超出、22 个未定位。需优先修筛选和同路多分店歧义。本轮保留正式产品逻辑。</p><h2>本浏览器运行记录</h2><p>下表是响应数量，重跑会调用外部地点接口；查看报告无需重跑。</p><button disabled={busy} onClick={start}>开始／继续实际查询</button> <button disabled={!busy} onClick={()=>{stop.current=true}}>停止</button> <button onClick={download}>下载原始结果 JSON</button><p role="status">{current} · 已完成 {runs.length}/200</p><table><thead><tr><th>规则</th><th>已执行</th><th>有共同区域</th><th>请求异常</th></tr></thead><tbody>{['raw-title-location','brand-area','brand-store-city','brand-area-city'].map(rule=><tr key={rule}><td>{ruleLabels[rule]}</td><td>{runs.filter(r=>r.rule===rule).length}</td><td>{runs.filter(r=>r.rule===rule&&r.data?.areas.length).length}</td><td>{runs.filter(r=>r.rule===rule&&r.error).length}</td></tr>)}</tbody></table><details><summary>逐条摘要</summary><pre>{runs.map(r=>`${r.id}\t${r.rule}\t${r.query}\t${r.data?.areas[0]?.representative.name??'无区域'}\t${r.data?.areas[0]?.representative.postcode??''}\t${r.data?.areas[0]?.representative.lat??''},${r.data?.areas[0]?.representative.lon??''}\t${r.error??''}`).join('\n')}</pre></details><h2>采样清单与来源</h2><table><thead><tr><th>样本</th><th>品牌</th><th>岗位／门店</th><th>原始地点</th></tr></thead><tbody>{dataset.samples.map(s=><tr key={s.id}><td>{s.id}</td><td>{s.brand}</td><td><a href={s.source} target="_blank" rel="noreferrer">{s.title}</a><br/>{s.store}</td><td>{s.city} {s.expectedPostcode}</td></tr>)}</tbody></table></main>
}
