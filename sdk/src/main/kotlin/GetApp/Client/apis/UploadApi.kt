/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package GetApp.Client.apis

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.HttpUrl

import GetApp.Client.models.ComponentDto
import GetApp.Client.models.UpdateUploadStatusDto
import GetApp.Client.models.UploadArtifactDto

import com.squareup.moshi.Json

import GetApp.Client.infrastructure.ApiClient
import GetApp.Client.infrastructure.ApiResponse
import GetApp.Client.infrastructure.ClientException
import GetApp.Client.infrastructure.ClientError
import GetApp.Client.infrastructure.ServerException
import GetApp.Client.infrastructure.ServerError
import GetApp.Client.infrastructure.MultiValueMap
import GetApp.Client.infrastructure.PartConfig
import GetApp.Client.infrastructure.RequestConfig
import GetApp.Client.infrastructure.RequestMethod
import GetApp.Client.infrastructure.ResponseType
import GetApp.Client.infrastructure.Success
import GetApp.Client.infrastructure.toMultiValue

class UploadApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) : ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "http://localhost")
        }
    }

    /**
     * Get Last Version
     * This service message allows retrieval of the last version by project ID.
     * @param projectId 
     * @return ComponentDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun uploadControllerGetLastVersion(projectId: java.math.BigDecimal) : ComponentDto {
        val localVarResponse = uploadControllerGetLastVersionWithHttpInfo(projectId = projectId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as ComponentDto
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()} ${localVarError.body}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Get Last Version
     * This service message allows retrieval of the last version by project ID.
     * @param projectId 
     * @return ApiResponse<ComponentDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun uploadControllerGetLastVersionWithHttpInfo(projectId: java.math.BigDecimal) : ApiResponse<ComponentDto?> {
        val localVariableConfig = uploadControllerGetLastVersionRequestConfig(projectId = projectId)

        return request<Unit, ComponentDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation uploadControllerGetLastVersion
     *
     * @param projectId 
     * @return RequestConfig
     */
    fun uploadControllerGetLastVersionRequestConfig(projectId: java.math.BigDecimal) : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/upload/lastVersion/{projectId}".replace("{"+"projectId"+"}", encodeURIComponent(projectId.toString())),
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * Update Upload Status
     * This service message allows updating the upload status.
     * @param updateUploadStatusDto 
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun uploadControllerUpdateUploadStatus(updateUploadStatusDto: UpdateUploadStatusDto) : Unit {
        val localVarResponse = uploadControllerUpdateUploadStatusWithHttpInfo(updateUploadStatusDto = updateUploadStatusDto)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> Unit
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()} ${localVarError.body}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Update Upload Status
     * This service message allows updating the upload status.
     * @param updateUploadStatusDto 
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun uploadControllerUpdateUploadStatusWithHttpInfo(updateUploadStatusDto: UpdateUploadStatusDto) : ApiResponse<Unit?> {
        val localVariableConfig = uploadControllerUpdateUploadStatusRequestConfig(updateUploadStatusDto = updateUploadStatusDto)

        return request<UpdateUploadStatusDto, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation uploadControllerUpdateUploadStatus
     *
     * @param updateUploadStatusDto 
     * @return RequestConfig
     */
    fun uploadControllerUpdateUploadStatusRequestConfig(updateUploadStatusDto: UpdateUploadStatusDto) : RequestConfig<UpdateUploadStatusDto> {
        val localVariableBody = updateUploadStatusDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        
        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/upload/updateUploadStatus",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = false,
            body = localVariableBody
        )
    }

    /**
     * Upload Artifact
     * This service message allows uploading an artifact.
     * @param uploadArtifactDto 
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun uploadControllerUploadArtifact(uploadArtifactDto: UploadArtifactDto) : Unit {
        val localVarResponse = uploadControllerUploadArtifactWithHttpInfo(uploadArtifactDto = uploadArtifactDto)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> Unit
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()} ${localVarError.body}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Upload Artifact
     * This service message allows uploading an artifact.
     * @param uploadArtifactDto 
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun uploadControllerUploadArtifactWithHttpInfo(uploadArtifactDto: UploadArtifactDto) : ApiResponse<Unit?> {
        val localVariableConfig = uploadControllerUploadArtifactRequestConfig(uploadArtifactDto = uploadArtifactDto)

        return request<UploadArtifactDto, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation uploadControllerUploadArtifact
     *
     * @param uploadArtifactDto 
     * @return RequestConfig
     */
    fun uploadControllerUploadArtifactRequestConfig(uploadArtifactDto: UploadArtifactDto) : RequestConfig<UploadArtifactDto> {
        val localVariableBody = uploadArtifactDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        
        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/upload/artifact",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = false,
            body = localVariableBody
        )
    }

    /**
     * Upload Manifest
     * This service message allows uploading a manifest file and an upload token.
     * @param file 
     * @param uploadToken  (optional)
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun uploadControllerUploadManifest(file: java.io.File, uploadToken: kotlin.String? = null) : Unit {
        val localVarResponse = uploadControllerUploadManifestWithHttpInfo(file = file, uploadToken = uploadToken)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> Unit
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()} ${localVarError.body}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Upload Manifest
     * This service message allows uploading a manifest file and an upload token.
     * @param file 
     * @param uploadToken  (optional)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun uploadControllerUploadManifestWithHttpInfo(file: java.io.File, uploadToken: kotlin.String?) : ApiResponse<Unit?> {
        val localVariableConfig = uploadControllerUploadManifestRequestConfig(file = file, uploadToken = uploadToken)

        return request<Map<String, PartConfig<*>>, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation uploadControllerUploadManifest
     *
     * @param file 
     * @param uploadToken  (optional)
     * @return RequestConfig
     */
    fun uploadControllerUploadManifestRequestConfig(file: java.io.File, uploadToken: kotlin.String?) : RequestConfig<Map<String, PartConfig<*>>> {
        val localVariableBody = mapOf(
            "file" to PartConfig(body = file, headers = mutableMapOf()),
            "uploadToken" to PartConfig(body = uploadToken, headers = mutableMapOf()),)
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf("Content-Type" to "multipart/form-data")
        
        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/upload/manifest",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = false,
            body = localVariableBody
        )
    }


    private fun encodeURIComponent(uriComponent: kotlin.String): kotlin.String =
        HttpUrl.Builder().scheme("http").host("localhost").addPathSegment(uriComponent).build().encodedPathSegments[0]
}
