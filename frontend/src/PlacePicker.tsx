import { useEffect, useRef, useState } from 'react'
import { ApiFailure, searchPlaces } from './api/client'
import type { PlaceCandidate, PlaceSearchResult } from './api/client'
import { mapPreviewUrl } from './placeSelection'
import './placePicker.css'

export default function PlacePicker({value,token,lang,onChange,onSelect,searchVersion=0}:{
 value:string;token?:string;lang:'zh'|'en';onChange:(value:string)=>void;onSelect:(candidate:PlaceCandidate)=>void;searchVersion?:number
}) {
 const zh=lang==='zh',version=useRef(0)
 const [result,setResult]=useState<PlaceSearchResult|null>(null),[preview,setPreview]=useState<PlaceCandidate|null>(null),[chosen,setChosen]=useState<PlaceCandidate|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState('')
 useEffect(()=>{version.current++;setResult(null);setPreview(null);setError('');setBusy(false);if(!token)setChosen(null)},[value,token])
 useEffect(()=>()=>{version.current++},[])
 async function search(){
  if(value.trim().length<3)return
  const id=++version.current;setBusy(true);setError('');setPreview(null);setResult(null)
  try{const data=await searchPlaces(value.trim());if(id===version.current)setResult(data)}
  catch(e){
   if(id===version.current){
    const code=e instanceof ApiFailure?e.payload.code:''
    setError(code==='api_not_found'?(zh?'新版位置接口未加载，请重启本地服务。':'Restart the local backend to load place search.'):code==='invalid_request'?(zh?'请检查地点格式。坐标应为有效的纬度,经度；也可输入地点名或完整 postcode。':'Check the location format. Use valid latitude,longitude, a place name or a full postcode.'):(zh?'位置服务暂时不可用，请稍后重试或输入完整 postcode。':'Place search is unavailable. Retry or enter a full postcode.'))
   }
  }
  finally{if(id===version.current)setBusy(false)}
 }
 useEffect(()=>{if(searchVersion>0)void search()},[searchVersion])
 const precision=(p:string)=>({venue:zh?'门店／地点':'Venue',address:zh?'地址':'Address',postcode:zh?'邮区，非门店入口':'Postcode area',coordinate:zh?'坐标点':'Coordinates',station:zh?'交通站点':'Transport stop',street:zh?'仅街道':'Street only',locality:zh?'仅区域':'Area only'}[p]??p)
 const partialSearch=result?.message==='Address search unavailable or temporarily rate limited. Results may only include TfL locations.'
 return <div className="work-field place-picker"><label>{zh?'工作地点 / postcode':'Workplace / postcode'}<input value={value} maxLength={120} onChange={e=>onChange(e.target.value)} autoComplete="off"/></label>
 <div className="place-search-action"><button type="button" className="work-button secondary" disabled={busy||value.trim().length<3} onClick={search}>{busy?(zh?'搜索中…':'Searching…'):(zh?'搜索并确认位置':'Find and confirm location')}</button>{token&&<span className="place-confirmed">{zh?'已确认查询位置':'Journey location confirmed'}</span>}</div>
 {token&&chosen&&<small>{chosen.source} · {precision(chosen.precision)}</small>}
 {error&&<p role="alert">{error}</p>}
 {result&&<div className="place-results" aria-live="polite"><p>{result.status==='unavailable'?(zh?'服务不可用，请稍后重试。':'Service unavailable. Retry later.'):result.candidates.length?(zh?'选择具体候选，查看地址和地图后确认。':'Choose a candidate, review its address and map, then confirm.'):(zh?'没有匹配项，请补充街道地址或完整 postcode。':'No match. Add a street address or postcode.')}</p>
 {partialSearch&&<p role="status">{zh?'地址搜索暂不可用或受到限流；当前可能只有 TfL 地点，请勿把附近车站当成门店。':'Address search is unavailable or rate limited. Results may only include TfL locations; a nearby stop is not the workplace.'}</p>}
 {result.candidates.map((c,i)=><button type="button" className="place-candidate" key={c.id||i} onClick={()=>setPreview(c)} aria-pressed={preview===c}><strong>{c.name}</strong><span>{precision(c.precision)} · {c.source}</span>{!c.selectable&&<small>{zh?'精度不足，请细化地址。':'Refine this result before selecting.'}</small>}</button>)}
 <small>{result.attribution}</small><a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">© OpenStreetMap contributors</a></div>}
 {preview&&<section className="place-preview"><strong>{preview.name}</strong><p>{preview.address}</p><p>{precision(preview.precision)} · {preview.source}</p>
 {preview.source==='User station ID'&&<p>{zh?'这里只确认 ID 格式，站名尚未核验。':'Only the ID format is checked; the station name is unverified.'}</p>}
 {preview.precision==='station'&&preview.source!=='User station ID'&&<p>{zh?'这是交通站点，仅在它确实是你的目的地时选择；附近车站不等于工作门店。':'This is a transport stop. Select it only if it is your destination; a nearby station is not the workplace.'}</p>}
 {preview.precision==='postcode'&&<p>{zh?'邮编可用于查询，但可能覆盖多个地址，并不等于门店入口。':'A postcode may cover several addresses and does not identify a store entrance.'}</p>}
 {preview.lat!=null&&preview.lon!=null&&<><iframe title={zh?'候选位置地图':'Candidate map'} src={mapPreviewUrl(preview.lat,preview.lon)} loading="lazy" referrerPolicy="no-referrer"/><small>{preview.lat}, {preview.lon} · © OpenStreetMap contributors</small></>}
 <button type="button" className="work-button" disabled={!preview.selectable} onClick={()=>{setChosen(preview);onSelect(preview);setResult(null);setPreview(null)}}>{zh?'确认这个工作地点':'Confirm this workplace'}</button>
 </section>}
 </div>
}
