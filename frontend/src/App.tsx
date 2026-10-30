import WorkCommutePage from './WorkCommutePage'
import JourneyLookupPage from './features/journey/JourneyLookupPage'
import SimpleCommutePage from './SimpleCommutePage'
import LocationBenchmarkPage from './benchmark/LocationBenchmarkPage'
import PlaceSearchDemoPage from './PlaceSearchDemoPage'

export default function App() {
  if(window.location.pathname==='/place-benchmark')return <LocationBenchmarkPage />
  if(window.location.pathname==='/place-demo')return <PlaceSearchDemoPage />
  if(window.location.pathname==='/journey')return <JourneyLookupPage />
  if(['/advanced','/work-commute'].includes(window.location.pathname))return <WorkCommutePage />
  return <SimpleCommutePage />
}
