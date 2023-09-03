# LoginApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**loginControllerGetRefreshToken**](LoginApi.md#loginControllerGetRefreshToken) | **POST** /api/login/refresh | 
[**loginControllerGetToken**](LoginApi.md#loginControllerGetToken) | **POST** /api/login | 


<a id="loginControllerGetRefreshToken"></a>
# **loginControllerGetRefreshToken**
> TokensDto loginControllerGetRefreshToken(refreshTokenDto)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = LoginApi()
val refreshTokenDto : RefreshTokenDto =  // RefreshTokenDto | 
try {
    val result : TokensDto = apiInstance.loginControllerGetRefreshToken(refreshTokenDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling LoginApi#loginControllerGetRefreshToken")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling LoginApi#loginControllerGetRefreshToken")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **refreshTokenDto** | [**RefreshTokenDto**](RefreshTokenDto.md)|  |

### Return type

[**TokensDto**](TokensDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="loginControllerGetToken"></a>
# **loginControllerGetToken**
> TokensDto loginControllerGetToken(userLoginDto)



### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = LoginApi()
val userLoginDto : UserLoginDto =  // UserLoginDto | 
try {
    val result : TokensDto = apiInstance.loginControllerGetToken(userLoginDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling LoginApi#loginControllerGetToken")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling LoginApi#loginControllerGetToken")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userLoginDto** | [**UserLoginDto**](UserLoginDto.md)|  |

### Return type

[**TokensDto**](TokensDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

