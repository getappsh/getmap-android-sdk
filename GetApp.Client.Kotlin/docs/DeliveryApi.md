# DeliveryApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deliveryControllerGetPreparedDeliveryStatus**](DeliveryApi.md#deliveryControllerGetPreparedDeliveryStatus) | **GET** /api/delivery/preparedDelivery/{catalogId} | 
[**deliveryControllerPrepareDelivery**](DeliveryApi.md#deliveryControllerPrepareDelivery) | **POST** /api/delivery/prepareDelivery | 
[**deliveryControllerUpdateDownloadStatus**](DeliveryApi.md#deliveryControllerUpdateDownloadStatus) | **POST** /api/delivery/updateDownloadStatus | 


<a id="deliveryControllerGetPreparedDeliveryStatus"></a>
# **deliveryControllerGetPreparedDeliveryStatus**
> PrepareDeliveryResDto deliveryControllerGetPreparedDeliveryStatus(catalogId)



Get status of prepared delivery

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeliveryApi()
val catalogId : kotlin.String = catalogId_example // kotlin.String | 
try {
    val result : PrepareDeliveryResDto = apiInstance.deliveryControllerGetPreparedDeliveryStatus(catalogId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DeliveryApi#deliveryControllerGetPreparedDeliveryStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeliveryApi#deliveryControllerGetPreparedDeliveryStatus")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **catalogId** | **kotlin.String**|  |

### Return type

[**PrepareDeliveryResDto**](PrepareDeliveryResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="deliveryControllerPrepareDelivery"></a>
# **deliveryControllerPrepareDelivery**
> PrepareDeliveryResDto deliveryControllerPrepareDelivery(prepareDeliveryReqDto)



Prepare delivery

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeliveryApi()
val prepareDeliveryReqDto : PrepareDeliveryReqDto =  // PrepareDeliveryReqDto | 
try {
    val result : PrepareDeliveryResDto = apiInstance.deliveryControllerPrepareDelivery(prepareDeliveryReqDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DeliveryApi#deliveryControllerPrepareDelivery")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeliveryApi#deliveryControllerPrepareDelivery")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **prepareDeliveryReqDto** | [**PrepareDeliveryReqDto**](PrepareDeliveryReqDto.md)|  |

### Return type

[**PrepareDeliveryResDto**](PrepareDeliveryResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deliveryControllerUpdateDownloadStatus"></a>
# **deliveryControllerUpdateDownloadStatus**
> deliveryControllerUpdateDownloadStatus(deliveryStatusDto)



This service message allows the consumer to report of the delivery status

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeliveryApi()
val deliveryStatusDto : DeliveryStatusDto =  // DeliveryStatusDto | 
try {
    apiInstance.deliveryControllerUpdateDownloadStatus(deliveryStatusDto)
} catch (e: ClientException) {
    println("4xx response calling DeliveryApi#deliveryControllerUpdateDownloadStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeliveryApi#deliveryControllerUpdateDownloadStatus")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **deliveryStatusDto** | [**DeliveryStatusDto**](DeliveryStatusDto.md)|  |

### Return type

null (empty response body)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

