# ProjectManagementApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**projectManagementControllerAddMemberToProject**](ProjectManagementApi.md#projectManagementControllerAddMemberToProject) | **POST** /api/projectManagement/project/{projectId}/member | Add member to Project
[**projectManagementControllerCreateProject**](ProjectManagementApi.md#projectManagementControllerCreateProject) | **POST** /api/projectManagement/project | Create Project
[**projectManagementControllerCreateToken**](ProjectManagementApi.md#projectManagementControllerCreateToken) | **POST** /api/projectManagement/project/{projectId}/createToken | Create Upload token for a Project
[**projectManagementControllerEditMember**](ProjectManagementApi.md#projectManagementControllerEditMember) | **PUT** /api/projectManagement/project/{projectId}/member/{memberId} | Edit member details
[**projectManagementControllerGetDeviceByPlatform**](ProjectManagementApi.md#projectManagementControllerGetDeviceByPlatform) | **GET** /api/projectManagement/devices/platform/{platform} | Get all devices in platform
[**projectManagementControllerGetDevicesByCatalogId**](ProjectManagementApi.md#projectManagementControllerGetDevicesByCatalogId) | **GET** /api/projectManagement/devices/catalogId/{catalogId} | Get all devices with catalogId
[**projectManagementControllerGetDevicesByProject**](ProjectManagementApi.md#projectManagementControllerGetDevicesByProject) | **GET** /api/projectManagement/devices/project/{projectId} | Get all devices using component of the projectId
[**projectManagementControllerGetProjectConfigOption**](ProjectManagementApi.md#projectManagementControllerGetProjectConfigOption) | **GET** /api/projectManagement/projectConfigOption | Get project&#39;s config option
[**projectManagementControllerGetProjectReleases**](ProjectManagementApi.md#projectManagementControllerGetProjectReleases) | **GET** /api/projectManagement/project/{projectId}/projectReleases | Get project release
[**projectManagementControllerGetUserProjects**](ProjectManagementApi.md#projectManagementControllerGetUserProjects) | **GET** /api/projectManagement/project | Get all User&#39;s projects
[**projectManagementControllerRemoveMemberFromProject**](ProjectManagementApi.md#projectManagementControllerRemoveMemberFromProject) | **DELETE** /api/projectManagement/project/{projectId}/member/{memberId} | Remove member from Project
[**projectManagementControllerSetProjectConfigOption**](ProjectManagementApi.md#projectManagementControllerSetProjectConfigOption) | **POST** /api/projectManagement/projectConfigOption | Set project&#39;s config option


<a id="projectManagementControllerAddMemberToProject"></a>
# **projectManagementControllerAddMemberToProject**
> MemberProjectResDto projectManagementControllerAddMemberToProject(projectId, projectMemberDto)

Add member to Project

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val projectId : kotlin.Any =  // kotlin.Any | 
val projectMemberDto : ProjectMemberDto =  // ProjectMemberDto | 
try {
    val result : MemberProjectResDto = apiInstance.projectManagementControllerAddMemberToProject(projectId, projectMemberDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerAddMemberToProject")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerAddMemberToProject")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectId** | [**kotlin.Any**](.md)|  |
 **projectMemberDto** | [**ProjectMemberDto**](ProjectMemberDto.md)|  |

### Return type

[**MemberProjectResDto**](MemberProjectResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="projectManagementControllerCreateProject"></a>
# **projectManagementControllerCreateProject**
> ProjectResDto projectManagementControllerCreateProject(projectDto)

Create Project

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val projectDto : ProjectDto =  // ProjectDto | 
try {
    val result : ProjectResDto = apiInstance.projectManagementControllerCreateProject(projectDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerCreateProject")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerCreateProject")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectDto** | [**ProjectDto**](ProjectDto.md)|  |

### Return type

[**ProjectResDto**](ProjectResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="projectManagementControllerCreateToken"></a>
# **projectManagementControllerCreateToken**
> ProjectTokenDto projectManagementControllerCreateToken(projectId)

Create Upload token for a Project

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val projectId : kotlin.Any =  // kotlin.Any | 
try {
    val result : ProjectTokenDto = apiInstance.projectManagementControllerCreateToken(projectId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerCreateToken")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerCreateToken")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectId** | [**kotlin.Any**](.md)|  |

### Return type

[**ProjectTokenDto**](ProjectTokenDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerEditMember"></a>
# **projectManagementControllerEditMember**
> MemberResDto projectManagementControllerEditMember(memberId, projectId, editProjectMemberDto)

Edit member details

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val memberId : kotlin.Any =  // kotlin.Any | 
val projectId : kotlin.Any =  // kotlin.Any | 
val editProjectMemberDto : EditProjectMemberDto =  // EditProjectMemberDto | 
try {
    val result : MemberResDto = apiInstance.projectManagementControllerEditMember(memberId, projectId, editProjectMemberDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerEditMember")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerEditMember")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **memberId** | [**kotlin.Any**](.md)|  |
 **projectId** | [**kotlin.Any**](.md)|  |
 **editProjectMemberDto** | [**EditProjectMemberDto**](EditProjectMemberDto.md)|  |

### Return type

[**MemberResDto**](MemberResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="projectManagementControllerGetDeviceByPlatform"></a>
# **projectManagementControllerGetDeviceByPlatform**
> kotlin.collections.List&lt;DeviceResDto&gt; projectManagementControllerGetDeviceByPlatform(platform)

Get all devices in platform

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val platform : kotlin.String = platform_example // kotlin.String | 
try {
    val result : kotlin.collections.List<DeviceResDto> = apiInstance.projectManagementControllerGetDeviceByPlatform(platform)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerGetDeviceByPlatform")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerGetDeviceByPlatform")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **platform** | **kotlin.String**|  |

### Return type

[**kotlin.collections.List&lt;DeviceResDto&gt;**](DeviceResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerGetDevicesByCatalogId"></a>
# **projectManagementControllerGetDevicesByCatalogId**
> kotlin.collections.List&lt;DeviceResDto&gt; projectManagementControllerGetDevicesByCatalogId(catalogId)

Get all devices with catalogId

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val catalogId : java.math.BigDecimal = 8.14 // java.math.BigDecimal | 
try {
    val result : kotlin.collections.List<DeviceResDto> = apiInstance.projectManagementControllerGetDevicesByCatalogId(catalogId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerGetDevicesByCatalogId")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerGetDevicesByCatalogId")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **catalogId** | **java.math.BigDecimal**|  |

### Return type

[**kotlin.collections.List&lt;DeviceResDto&gt;**](DeviceResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerGetDevicesByProject"></a>
# **projectManagementControllerGetDevicesByProject**
> kotlin.collections.List&lt;DeviceResDto&gt; projectManagementControllerGetDevicesByProject(projectId)

Get all devices using component of the projectId

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val projectId : java.math.BigDecimal = 8.14 // java.math.BigDecimal | 
try {
    val result : kotlin.collections.List<DeviceResDto> = apiInstance.projectManagementControllerGetDevicesByProject(projectId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerGetDevicesByProject")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerGetDevicesByProject")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectId** | **java.math.BigDecimal**|  |

### Return type

[**kotlin.collections.List&lt;DeviceResDto&gt;**](DeviceResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerGetProjectConfigOption"></a>
# **projectManagementControllerGetProjectConfigOption**
> ProjectConfigResDto projectManagementControllerGetProjectConfigOption()

Get project&#39;s config option

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
try {
    val result : ProjectConfigResDto = apiInstance.projectManagementControllerGetProjectConfigOption()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerGetProjectConfigOption")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerGetProjectConfigOption")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ProjectConfigResDto**](ProjectConfigResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerGetProjectReleases"></a>
# **projectManagementControllerGetProjectReleases**
> kotlin.collections.List&lt;ProjectReleasesDto&gt; projectManagementControllerGetProjectReleases(projectId)

Get project release

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val projectId : kotlin.Any =  // kotlin.Any | 
try {
    val result : kotlin.collections.List<ProjectReleasesDto> = apiInstance.projectManagementControllerGetProjectReleases(projectId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerGetProjectReleases")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerGetProjectReleases")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectId** | [**kotlin.Any**](.md)|  |

### Return type

[**kotlin.collections.List&lt;ProjectReleasesDto&gt;**](ProjectReleasesDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerGetUserProjects"></a>
# **projectManagementControllerGetUserProjects**
> MemberProjectsResDto projectManagementControllerGetUserProjects()

Get all User&#39;s projects

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
try {
    val result : MemberProjectsResDto = apiInstance.projectManagementControllerGetUserProjects()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerGetUserProjects")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerGetUserProjects")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**MemberProjectsResDto**](MemberProjectsResDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="projectManagementControllerRemoveMemberFromProject"></a>
# **projectManagementControllerRemoveMemberFromProject**
> projectManagementControllerRemoveMemberFromProject(memberId, projectId)

Remove member from Project

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val memberId : kotlin.Any =  // kotlin.Any | 
val projectId : kotlin.Any =  // kotlin.Any | 
try {
    apiInstance.projectManagementControllerRemoveMemberFromProject(memberId, projectId)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerRemoveMemberFromProject")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerRemoveMemberFromProject")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **memberId** | [**kotlin.Any**](.md)|  |
 **projectId** | [**kotlin.Any**](.md)|  |

### Return type

null (empty response body)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="projectManagementControllerSetProjectConfigOption"></a>
# **projectManagementControllerSetProjectConfigOption**
> ProjectConfigDto projectManagementControllerSetProjectConfigOption(projectConfigDto)

Set project&#39;s config option

### Example
```kotlin
// Import classes:
//import GetApp.Client.infrastructure.*
//import GetApp.Client.models.*

val apiInstance = ProjectManagementApi()
val projectConfigDto : ProjectConfigDto =  // ProjectConfigDto | 
try {
    val result : ProjectConfigDto = apiInstance.projectManagementControllerSetProjectConfigOption(projectConfigDto)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ProjectManagementApi#projectManagementControllerSetProjectConfigOption")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ProjectManagementApi#projectManagementControllerSetProjectConfigOption")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **projectConfigDto** | [**ProjectConfigDto**](ProjectConfigDto.md)|  |

### Return type

[**ProjectConfigDto**](ProjectConfigDto.md)

### Authorization


Configure bearer:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

