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

import GetApp.Client.models.BugReportDto
import GetApp.Client.models.NewBugReportDto
import GetApp.Client.models.NewBugReportResDto

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

class DeviceBugReportApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) : ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "http://localhost")
        }
    }

    /**
     * Get Bug Report
     * This endpoint allows a user to fetch the details of a bug report using its unique identifier.
     * @param bugId 
     * @return BugReportDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun bugReportControllerGetBugReport(bugId: kotlin.String) : BugReportDto {
        val localVarResponse = bugReportControllerGetBugReportWithHttpInfo(bugId = bugId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as BugReportDto
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
     * Get Bug Report
     * This endpoint allows a user to fetch the details of a bug report using its unique identifier.
     * @param bugId 
     * @return ApiResponse<BugReportDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun bugReportControllerGetBugReportWithHttpInfo(bugId: kotlin.String) : ApiResponse<BugReportDto?> {
        val localVariableConfig = bugReportControllerGetBugReportRequestConfig(bugId = bugId)

        return request<Unit, BugReportDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation bugReportControllerGetBugReport
     *
     * @param bugId 
     * @return RequestConfig
     */
    fun bugReportControllerGetBugReportRequestConfig(bugId: kotlin.String) : RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/api/v1/bug-report/{bugId}".replace("{"+"bugId"+"}", encodeURIComponent(bugId.toString())),
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    /**
     * Report New Bug
     * This endpoint allows a user to report a new bug associated with a specific device.
     * @param newBugReportDto 
     * @return NewBugReportResDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun bugReportControllerReportNewBug(newBugReportDto: NewBugReportDto) : NewBugReportResDto {
        val localVarResponse = bugReportControllerReportNewBugWithHttpInfo(newBugReportDto = newBugReportDto)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as NewBugReportResDto
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
     * Report New Bug
     * This endpoint allows a user to report a new bug associated with a specific device.
     * @param newBugReportDto 
     * @return ApiResponse<NewBugReportResDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun bugReportControllerReportNewBugWithHttpInfo(newBugReportDto: NewBugReportDto) : ApiResponse<NewBugReportResDto?> {
        val localVariableConfig = bugReportControllerReportNewBugRequestConfig(newBugReportDto = newBugReportDto)

        return request<NewBugReportDto, NewBugReportResDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation bugReportControllerReportNewBug
     *
     * @param newBugReportDto 
     * @return RequestConfig
     */
    fun bugReportControllerReportNewBugRequestConfig(newBugReportDto: NewBugReportDto) : RequestConfig<NewBugReportDto> {
        val localVariableBody = newBugReportDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/v1/bug-report",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }


    private fun encodeURIComponent(uriComponent: kotlin.String): kotlin.String =
        HttpUrl.Builder().scheme("http").host("localhost").addPathSegment(uriComponent).build().encodedPathSegments[0]
}
