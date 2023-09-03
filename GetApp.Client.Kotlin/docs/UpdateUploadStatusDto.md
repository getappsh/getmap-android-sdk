
# UpdateUploadStatusDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**status** | [**inline**](#Status) |  | 
**catalogId** | **kotlin.String** |  |  [optional]
**uploadToken** | **kotlin.String** |  |  [optional]


<a id="Status"></a>
## Enum: status
Name | Value
---- | -----
status | started, downloading-from-url, fail-to-download, uploading-to-s3, fail-to-upload, in-progress, ready, error



