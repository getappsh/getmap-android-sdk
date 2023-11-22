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

import GetApp.Client.models.CreateImportDto
import GetApp.Client.models.CreateImportResDto
import GetApp.Client.models.ImportStatusDto
import GetApp.Client.models.ImportStatusResDto
import GetApp.Client.models.MapDto
import GetApp.Client.models.OfferingMapResDto

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

class GetMapApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) : ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "http://localhost")
        }
    }

    /**
     * 
     * This service message allows the consumer to cancel import of a map stamp
     * @param importRequestId 
     * @return CreateImportResDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerCancelImportCreate(importRequestId: kotlin.String) : CreateImportResDto {
        val localVarResponse = getMapControllerCancelImportCreateWithHttpInfo(importRequestId = importRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as CreateImportResDto
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
     * 
     * This service message allows the consumer to cancel import of a map stamp
     * @param importRequestId 
     * @return ApiResponse<CreateImportResDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerCancelImportCreateWithHttpInfo(importRequestId: kotlin.String) : ApiResponse<CreateImportResDto?> {
        val localVariableConfig = getMapControllerCancelImportCreateRequestConfig(importRequestId = importRequestId)

        return request<Unit, CreateImportResDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerCancelImportCreate
     *
     * @param importRequestId 
     * @return RequestConfig
     */
    fun getMapControllerCancelImportCreateRequestConfig(importRequestId: kotlin.String) : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/map/import/cancel/{importRequestId}".replace("{"+"importRequestId"+"}", encodeURIComponent(importRequestId.toString())),
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * This service message allows the consumer to request to start export of a map stamp and tracking of the packaging process.
     * @param createImportDto 
     * @return CreateImportResDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerCreateImport(createImportDto: CreateImportDto) : CreateImportResDto {
        val localVarResponse = getMapControllerCreateImportWithHttpInfo(createImportDto = createImportDto)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as CreateImportResDto
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
     * 
     * This service message allows the consumer to request to start export of a map stamp and tracking of the packaging process.
     * @param createImportDto 
     * @return ApiResponse<CreateImportResDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerCreateImportWithHttpInfo(createImportDto: CreateImportDto) : ApiResponse<CreateImportResDto?> {
        val localVariableConfig = getMapControllerCreateImportRequestConfig(createImportDto = createImportDto)

        return request<CreateImportDto, CreateImportResDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerCreateImport
     *
     * @param createImportDto 
     * @return RequestConfig
     */
    fun getMapControllerCreateImportRequestConfig(createImportDto: CreateImportDto) : RequestConfig<CreateImportDto> {
        val localVariableBody = createImportDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/map/import/create",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * This service message allows the to get all requested map
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerGetAllMaps() : Unit {
        val localVarResponse = getMapControllerGetAllMapsWithHttpInfo()

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
     * 
     * This service message allows the to get all requested map
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerGetAllMapsWithHttpInfo() : ApiResponse<Unit?> {
        val localVariableConfig = getMapControllerGetAllMapsRequestConfig()

        return request<Unit, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerGetAllMaps
     *
     * @return RequestConfig
     */
    fun getMapControllerGetAllMapsRequestConfig() : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        
        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/map/maps",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * This service message allows the consumer to get status information and tracking of the packaging process.  
     * @param importRequestId 
     * @return ImportStatusResDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerGetImportStatus(importRequestId: kotlin.String) : ImportStatusResDto {
        val localVarResponse = getMapControllerGetImportStatusWithHttpInfo(importRequestId = importRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as ImportStatusResDto
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
     * 
     * This service message allows the consumer to get status information and tracking of the packaging process.  
     * @param importRequestId 
     * @return ApiResponse<ImportStatusResDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerGetImportStatusWithHttpInfo(importRequestId: kotlin.String) : ApiResponse<ImportStatusResDto?> {
        val localVariableConfig = getMapControllerGetImportStatusRequestConfig(importRequestId = importRequestId)

        return request<Unit, ImportStatusResDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerGetImportStatus
     *
     * @param importRequestId 
     * @return RequestConfig
     */
    fun getMapControllerGetImportStatusRequestConfig(importRequestId: kotlin.String) : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/map/import/status/{importRequestId}".replace("{"+"importRequestId"+"}", encodeURIComponent(importRequestId.toString())),
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * This service message allows the to get map by catalog id with its all devices
     * @param catalogId 
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerGetMap(catalogId: kotlin.String) : Unit {
        val localVarResponse = getMapControllerGetMapWithHttpInfo(catalogId = catalogId)

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
     * 
     * This service message allows the to get map by catalog id with its all devices
     * @param catalogId 
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerGetMapWithHttpInfo(catalogId: kotlin.String) : ApiResponse<Unit?> {
        val localVariableConfig = getMapControllerGetMapRequestConfig(catalogId = catalogId)

        return request<Unit, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerGetMap
     *
     * @param catalogId 
     * @return RequestConfig
     */
    fun getMapControllerGetMapRequestConfig(catalogId: kotlin.String) : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        
        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/map/map/{catalogId}".replace("{"+"catalogId"+"}", encodeURIComponent(catalogId.toString())),
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * This service message allows the to get requested map information by is importRequestId.
     * @param importRequestId 
     * @return MapDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerGetMapProperties(importRequestId: kotlin.String) : MapDto {
        val localVarResponse = getMapControllerGetMapPropertiesWithHttpInfo(importRequestId = importRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as MapDto
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
     * 
     * This service message allows the to get requested map information by is importRequestId.
     * @param importRequestId 
     * @return ApiResponse<MapDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerGetMapPropertiesWithHttpInfo(importRequestId: kotlin.String) : ApiResponse<MapDto?> {
        val localVariableConfig = getMapControllerGetMapPropertiesRequestConfig(importRequestId = importRequestId)

        return request<Unit, MapDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerGetMapProperties
     *
     * @param importRequestId 
     * @return RequestConfig
     */
    fun getMapControllerGetMapPropertiesRequestConfig(importRequestId: kotlin.String) : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/map/properties/{importRequestId}".replace("{"+"importRequestId"+"}", encodeURIComponent(importRequestId.toString())),
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * This service message allows the to get all offerings of maps
     * @return kotlin.collections.List<OfferingMapResDto>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerGetOffering() : kotlin.collections.List<OfferingMapResDto> {
        val localVarResponse = getMapControllerGetOfferingWithHttpInfo()

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as kotlin.collections.List<OfferingMapResDto>
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
     * 
     * This service message allows the to get all offerings of maps
     * @return ApiResponse<kotlin.collections.List<OfferingMapResDto>?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerGetOfferingWithHttpInfo() : ApiResponse<kotlin.collections.List<OfferingMapResDto>?> {
        val localVariableConfig = getMapControllerGetOfferingRequestConfig()

        return request<Unit, kotlin.collections.List<OfferingMapResDto>>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerGetOffering
     *
     * @return RequestConfig
     */
    fun getMapControllerGetOfferingRequestConfig() : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/map/offering",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * 
     * Set Import Status
     * @param importStatusDto 
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMapControllerPostImportStatus(importStatusDto: ImportStatusDto) : Unit {
        val localVarResponse = getMapControllerPostImportStatusWithHttpInfo(importStatusDto = importStatusDto)

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
     * 
     * Set Import Status
     * @param importStatusDto 
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun getMapControllerPostImportStatusWithHttpInfo(importStatusDto: ImportStatusDto) : ApiResponse<Unit?> {
        val localVariableConfig = getMapControllerPostImportStatusRequestConfig(importStatusDto = importStatusDto)

        return request<ImportStatusDto, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMapControllerPostImportStatus
     *
     * @param importStatusDto 
     * @return RequestConfig
     */
    fun getMapControllerPostImportStatusRequestConfig(importStatusDto: ImportStatusDto) : RequestConfig<ImportStatusDto> {
        val localVariableBody = importStatusDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        
        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/map/import/status",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }


    private fun encodeURIComponent(uriComponent: kotlin.String): kotlin.String =
        HttpUrl.Builder().scheme("http").host("localhost").addPathSegment(uriComponent).build().encodedPathSegments[0]
}
