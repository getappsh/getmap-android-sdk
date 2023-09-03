# OfferingApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**offeringControllerGetOfferingOfComp**](OfferingApi.md#offeringControllerGetOfferingOfComp) | **GET** /api/offering/component/{catalogId} | 


<a id="offeringControllerGetOfferingOfComp"></a>
# **offeringControllerGetOfferingOfComp**
> ComponentDto offeringControllerGetOfferingOfComp(catalogId)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = OfferingApi()
val catalogId : kotlin.String = catalogId_example // kotlin.String | 
try {
    val result : ComponentDto = apiInstance.offeringControllerGetOfferingOfComp(catalogId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling OfferingApi#offeringControllerGetOfferingOfComp")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling OfferingApi#offeringControllerGetOfferingOfComp")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **catalogId** | **kotlin.String**|  |

### Return type

[**ComponentDto**](ComponentDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

