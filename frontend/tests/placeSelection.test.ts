import { test } from 'node:test'
import { strict as assert } from 'node:assert'
import { isDirectTflLocation, mapPreviewUrl } from '../src/placeSelection.ts'

test('TfL direct endpoint formats do not require a third-party location ID',()=>{
 for(const value of ['da184de','DA18 4DE','51.5178,-0.0823','940GZZLULVT','9400ZZLULVT4','490000138K','910GABWD','1000138'])assert.equal(isDirectTflLocation(value),true,value)
 assert.equal(isDirectTflLocation('M&S Woolwich'),false)
 assert.equal(isDirectTflLocation('OSM:N12345'),false)
 assert.equal(isDirectTflLocation('940 GZZLULVT'),false)
})
test('invalid coordinate input is not treated as a confirmed direct endpoint',()=>{
 for(const value of ['91,0','0,181','NaN,1','Infinity,1'])assert.equal(isDirectTflLocation(value),false,value)
})
test('map marker preserves latitude and longitude order',()=>{
 const url=new URL(mapPreviewUrl(51.49,-0.067))
 assert.equal(url.searchParams.get('marker'),'51.49,-0.067')
 const bounds=url.searchParams.get('bbox')!.split(',').map(Number)
 assert.ok(bounds[0]<-.067&&bounds[2]>-.067)
 assert.ok(bounds[1]<51.49&&bounds[3]>51.49)
})
