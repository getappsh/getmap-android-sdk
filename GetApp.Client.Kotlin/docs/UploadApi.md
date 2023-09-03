# UploadApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**uploadControllerGetLastVersion**](UploadApi.md#uploadControllerGetLastVersion) | **GET** /api/upload/lastVersion/{projectId} | 
[**uploadControllerUpdateUploadStatus**](UploadApi.md#uploadControllerUpdateUploadStatus) | **POST** /api/upload/updateUploadStatus | 
[**uploadControllerUploadArtifact**](UploadApi.md#uploadControllerUploadArtifact) | **POST** /api/upload/artifact | 
[**uploadControllerUploadManifest**](UploadApi.md#uploadControllerUploadManifest) | **POST** /api/upload/manifest | 


<a id="uploadControllerGetLastVersion"></a>
# **uploadControllerGetLastVersion**
> ComponentDto uploadControllerGetLastVersion(projectId)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = UploadApi()
val projectId : java.math.BigDecimal = 8.14 // java.math.BigDecimal | 
try {
    val result : ComponentDto = apiInstance.uploadControllerGetLastVersion(projectId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling UploadApi#uploadControllerGetLastVersion")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling UploadApi#uploadControllerGetLastVersion")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectId** | **java.math.BigDecimal**|  |

### Return type

[**ComponentDto**](ComponentDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="uploadControllerUpdateUploadStatus"></a>
# **uploadControllerUpdateUploadStatus**
> uploadControllerUpdateUploadStatus(updateUploadStatusDto)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = UploadApi()
val updateUploadStatusDto : UpdateUploadStatusDto =  // UpdateUploadStatusDto | 
try {
    apiInstance.uploadControllerUpdateUploadStatus(updateUploadStatusDto)
} catch (e: ClientException) {
    println("4xx response calling UploadApi#uploadControllerUpdateUploadStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling UploadApi#uploadControllerUpdateUploadStatus")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **updateUploadStatusDto** | [**UpdateUploadStatusDto**](UpdateUploadStatusDto.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a id="uploadControllerUploadArtifact"></a>
# **uploadControllerUploadArtifact**
> uploadControllerUploadArtifact(uploadArtifactDto)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = UploadApi()
val uploadArtifactDto : UploadArtifactDto =  // UploadArtifactDto | 
try {
    apiInstance.uploadControllerUploadArtifact(uploadArtifactDto)
} catch (e: ClientException) {
    println("4xx response calling UploadApi#uploadControllerUploadArtifact")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling UploadApi#uploadControllerUploadArtifact")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **uploadArtifactDto** | [**UploadArtifactDto**](UploadArtifactDto.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a id="uploadControllerUploadManifest"></a>
# **uploadControllerUploadManifest**
> uploadControllerUploadManifest(file, uploadToken)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = UploadApi()
val file : java.io.File = BINARY_DATA_HERE // java.io.File | 
val uploadToken : kotlin.String = uploadToken_example // kotlin.String | 
try {
    apiInstance.uploadControllerUploadManifest(file, uploadToken)
} catch (e: ClientException) {
    println("4xx response calling UploadApi#uploadControllerUploadManifest")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling UploadApi#uploadControllerUploadManifest")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **java.io.File**|  |
 **uploadToken** | **kotlin.String**|  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: Not defined

