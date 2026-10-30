export function isDirectTflLocation(value:string) {
 const compact=value.replace(/\s/g,'').toUpperCase()
 if(/^(?:GIR0AA|[A-Z]{1,2}\d[A-Z\d]?\d[A-Z]{2})$/.test(compact))return true
 const id=value.trim().toUpperCase()
 if(/^\d{7}$/.test(id)||/^(?:940|910|490)[A-Z0-9]{4,12}$/.test(id))return true
 const m=value.trim().match(/^(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)$/)
 return !!m&&Number.isFinite(Number(m[1]))&&Number.isFinite(Number(m[2]))&&Math.abs(Number(m[1]))<=90&&Math.abs(Number(m[2]))<=180
}
export function mapPreviewUrl(lat:number,lon:number) {
 const bbox=[Math.max(-180,lon-.003),Math.max(-90,lat-.002),Math.min(180,lon+.003),Math.min(90,lat+.002)].join(',')
 return 'https://www.openstreetmap.org/export/embed.html?bbox='+encodeURIComponent(bbox)+'&layer=mapnik&marker='+encodeURIComponent(lat+','+lon)
}
