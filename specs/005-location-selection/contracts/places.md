# HTTP

GET /api/place-search?query=... → JSON {status,candidates,message,attribution}。invalid query →400。
选中候选填框的 name 与 id 一起交给原 JourneyLookupRequest.selectedDestinationCandidateId 或 WorkCommuteInput.selectedWorkplaceCandidateId；不另发第三方 ID。后端按令牌取 routeValue，去返程交换既有 token。
保留 GET /api/locations；它与新接口共用选择令牌库，但旧响应无需变更。
