
# DeliveryStatusDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**deliveryStatus** | [**inline**](#DeliveryStatus) |  | 
**type** | [**inline**](#Type) |  | 
**deviceId** | **kotlin.String** |  |  [optional]
**catalogId** | **kotlin.String** |  |  [optional]
**downloadStop** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]
**downloadStart** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]
**downloadDone** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]
**bitNumber** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional]
**downloadSpeed** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional]
**downloadData** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional]
**downloadEstimateTime** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional]
**currentTime** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  [optional]


<a id="DeliveryStatus"></a>
## Enum: deliveryStatus
Name | Value
---- | -----
deliveryStatus | Start, Done, Error, Cancelled, Pause, Continue, Download


<a id="Type"></a>
## Enum: type
Name | Value
---- | -----
type | software, map, cache



