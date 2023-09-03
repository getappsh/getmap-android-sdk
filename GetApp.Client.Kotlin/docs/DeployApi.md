# DeployApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deployControllerUpdateDeployStatus**](DeployApi.md#deployControllerUpdateDeployStatus) | **POST** /api/deploy/updateDeployStatus | 


<a id="deployControllerUpdateDeployStatus"></a>
# **deployControllerUpdateDeployStatus**
> deployControllerUpdateDeployStatus(deployStatusDto)



This service message allows the consumer to report of the deploy status. When deploy done the device content relevant service will notify. Another option on this service are update delete content on the device.

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = DeployApi()
val deployStatusDto : DeployStatusDto =  // DeployStatusDto | 
try {
    apiInstance.deployControllerUpdateDeployStatus(deployStatusDto)
} catch (e: ClientException) {
    println("4xx response calling DeployApi#deployControllerUpdateDeployStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DeployApi#deployControllerUpdateDeployStatus")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **deployStatusDto** | [**DeployStatusDto**](DeployStatusDto.md)|  |

### Return type

null (empty response body)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

