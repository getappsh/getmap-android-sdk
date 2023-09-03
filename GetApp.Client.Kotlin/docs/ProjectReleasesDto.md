
# ProjectReleasesDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**uploadStatus** | [**inline**](#UploadStatus) |  | 
**catalogId** | **kotlin.String** |  |  [optional]
**name** | **kotlin.String** |  |  [optional]
**platform** | **kotlin.String** |  |  [optional]
**formation** | **kotlin.String** |  |  [optional]
**version** | **kotlin.String** |  |  [optional]
**releaseNotes** | **kotlin.String** |  |  [optional]
**virtualSize** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional]
**category** | **kotlin.String** |  |  [optional]
**deploymentStatus** | **kotlin.String** |  |  [optional]
**securityStatus** | **kotlin.String** |  |  [optional]
**policyStatus** | **kotlin.String** |  |  [optional]


<a id="UploadStatus"></a>
## Enum: uploadStatus
Name | Value
---- | -----
uploadStatus | started, downloading-from-url, fail-to-download, uploading-to-s3, fail-to-upload, in-progress, ready, error



