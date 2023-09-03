# GetMapApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getMapControllerCancelImportCreate**](GetMapApi.md#getMapControllerCancelImportCreate) | **POST** /api/map/import/create/cancel/{importRequestId} | 
[**getMapControllerCreateImport**](GetMapApi.md#getMapControllerCreateImport) | **POST** /api/map/import/create | 
[**getMapControllerGetImportStatus**](GetMapApi.md#getMapControllerGetImportStatus) | **GET** /api/map/import/status/{importRequestId} | 
[**getMapControllerGetMapProperties**](GetMapApi.md#getMapControllerGetMapProperties) | **GET** /api/map/properties/{importRequestId} | 
[**getMapControllerPostImportStatus**](GetMapApi.md#getMapControllerPostImportStatus) | **POST** /api/map/import/status | 


<a id="getMapControllerCancelImportCreate"></a>
# **getMapControllerCancelImportCreate**
> CreateImportResDto getMapControllerCancelImportCreate(importRequestId)



This service message allows the consumer to cancel import of a map stamp

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = GetMapApi()
val importRequestId : kotlin.String = importRequestId_example // kotlin.String | 
try {
    val result : CreateImportResDto = apiInstance.getMapControllerCancelImportCreate(importRequestId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling GetMapApi#getMapControllerCancelImportCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling GetMapApi#getMapControllerCancelImportCreate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **importRequestId** | **kotlin.String**|  |

### Return type

[**CreateImportResDto**](CreateImportResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getMapControllerCreateImport"></a>
# **getMapControllerCreateImport**
> CreateImportResDto getMapControllerCreateImport(createImportDto)



This service message allows the consumer to request to start export of a map stamp and tracking of the packaging process.

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = GetMapApi()
val createImportDto : CreateImportDto =  // CreateImportDto | 
try {
    val result : CreateImportResDto = apiInstance.getMapControllerCreateImport(createImportDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling GetMapApi#getMapControllerCreateImport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling GetMapApi#getMapControllerCreateImport")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **createImportDto** | [**CreateImportDto**](CreateImportDto.md)|  |

### Return type

[**CreateImportResDto**](CreateImportResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getMapControllerGetImportStatus"></a>
# **getMapControllerGetImportStatus**
> ImportStatusResDto getMapControllerGetImportStatus(importRequestId)



This service message allows the consumer to get status information and tracking of the packaging process.  

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = GetMapApi()
val importRequestId : kotlin.String = importRequestId_example // kotlin.String | 
try {
    val result : ImportStatusResDto = apiInstance.getMapControllerGetImportStatus(importRequestId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling GetMapApi#getMapControllerGetImportStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling GetMapApi#getMapControllerGetImportStatus")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **importRequestId** | **kotlin.String**|  |

### Return type

[**ImportStatusResDto**](ImportStatusResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getMapControllerGetMapProperties"></a>
# **getMapControllerGetMapProperties**
> MapDto getMapControllerGetMapProperties(importRequestId)



This service message allows the to get requested map information by is importRequestId.

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = GetMapApi()
val importRequestId : kotlin.String = importRequestId_example // kotlin.String | 
try {
    val result : MapDto = apiInstance.getMapControllerGetMapProperties(importRequestId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling GetMapApi#getMapControllerGetMapProperties")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling GetMapApi#getMapControllerGetMapProperties")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **importRequestId** | **kotlin.String**|  |

### Return type

[**MapDto**](MapDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getMapControllerPostImportStatus"></a>
# **getMapControllerPostImportStatus**
> getMapControllerPostImportStatus(importStatusDto)



Set Import Status

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = GetMapApi()
val importStatusDto : ImportStatusDto =  // ImportStatusDto | 
try {
    apiInstance.getMapControllerPostImportStatus(importStatusDto)
} catch (e: ClientException) {
    println("4xx response calling GetMapApi#getMapControllerPostImportStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling GetMapApi#getMapControllerPostImportStatus")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **importStatusDto** | [**ImportStatusDto**](ImportStatusDto.md)|  |

### Return type

null (empty response body)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

