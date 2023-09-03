# DeviceApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deviceControllerDiscoveryCatalog**](DeviceApi.md#deviceControllerDiscoveryCatalog) | **POST** /api/device/discover | 
[**deviceControllerGetDeviceContentInstalled**](DeviceApi.md#deviceControllerGetDeviceContentInstalled) | **GET** /api/device/info/installed/{deviceId} | 
[**deviceControllerRegister**](DeviceApi.md#deviceControllerRegister) | **POST** /api/device/register | 


<a id="deviceControllerDiscoveryCatalog"></a>
# **deviceControllerDiscoveryCatalog**
> DiscoveryResDto deviceControllerDiscoveryCatalog(discoveryMessageDto)



This service message allow to device post the discovery context for getting offers softwares and maps for GetApp agent. 

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeviceApi()
val discoveryMessageDto : DiscoveryMessageDto =  // DiscoveryMessageDto | 
try {
    val result : DiscoveryResDto = apiInstance.deviceControllerDiscoveryCatalog(discoveryMessageDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DeviceApi#deviceControllerDiscoveryCatalog")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeviceApi#deviceControllerDiscoveryCatalog")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **discoveryMessageDto** | [**DiscoveryMessageDto**](DiscoveryMessageDto.md)|  |

### Return type

[**DiscoveryResDto**](DiscoveryResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deviceControllerGetDeviceContentInstalled"></a>
# **deviceControllerGetDeviceContentInstalled**
> DeviceContentResDto deviceControllerGetDeviceContentInstalled(deviceId)



This service message allow receiving the information for the installations carried out on the device using GetApp services. This message is sent by the device during init phase in order to check compatibility between the existing installations on this device.

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeviceApi()
val deviceId : kotlin.String = deviceId_example // kotlin.String | 
try {
    val result : DeviceContentResDto = apiInstance.deviceControllerGetDeviceContentInstalled(deviceId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DeviceApi#deviceControllerGetDeviceContentInstalled")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeviceApi#deviceControllerGetDeviceContentInstalled")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **deviceId** | **kotlin.String**|  |

### Return type

[**DeviceContentResDto**](DeviceContentResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="deviceControllerRegister"></a>
# **deviceControllerRegister**
> deviceControllerRegister(deviceRegisterDto)



This service message allow to device registration process for GetApp services.

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeviceApi()
val deviceRegisterDto : DeviceRegisterDto =  // DeviceRegisterDto | 
try {
    apiInstance.deviceControllerRegister(deviceRegisterDto)
} catch (e: ClientException) {
    println("4xx response calling DeviceApi#deviceControllerRegister")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeviceApi#deviceControllerRegister")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **deviceRegisterDto** | [**DeviceRegisterDto**](DeviceRegisterDto.md)|  |

### Return type

null (empty response body)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

