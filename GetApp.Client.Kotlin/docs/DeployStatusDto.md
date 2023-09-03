
# DeployStatusDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**deployStatus** | [**inline**](#DeployStatus) |  | 
**type** | [**inline**](#Type) |  | 
**deviceId** | **kotlin.String** |  |  [optional]
**catalogId** | **kotlin.String** |  |  [optional]
**deployStop** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]
**deployStart** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]
**deployDone** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]
**deployEstimateTime** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional]
**currentTime** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]


<a id="DeployStatus"></a>
## Enum: deployStatus
Name | Value
---- | -----
deployStatus | Start, Done, installing, Continue, Pause, Cancelled, Error


<a id="Type"></a>
## Enum: type
Name | Value
---- | -----
type | software, map, cache



