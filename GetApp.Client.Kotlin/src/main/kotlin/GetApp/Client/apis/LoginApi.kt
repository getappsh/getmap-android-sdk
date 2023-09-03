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

import GetApp.Client.models.RefreshTokenDto
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto

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

class LoginApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) : ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "http://localhost")
        }
    }

    /**
     * 
     * 
     * @param refreshTokenDto 
     * @return TokensDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun loginControllerGetRefreshToken(refreshTokenDto: RefreshTokenDto) : TokensDto {
        val localVarResponse = loginControllerGetRefreshTokenWithHttpInfo(refreshTokenDto = refreshTokenDto)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as TokensDto
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * 
     * 
     * @param refreshTokenDto 
     * @return ApiResponse<TokensDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun loginControllerGetRefreshTokenWithHttpInfo(refreshTokenDto: RefreshTokenDto) : ApiResponse<TokensDto?> {
        val localVariableConfig = loginControllerGetRefreshTokenRequestConfig(refreshTokenDto = refreshTokenDto)

        return request<RefreshTokenDto, TokensDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation loginControllerGetRefreshToken
     *
     * @param refreshTokenDto 
     * @return RequestConfig
     */
    fun loginControllerGetRefreshTokenRequestConfig(refreshTokenDto: RefreshTokenDto) : RequestConfig<RefreshTokenDto> {
        val localVariableBody = refreshTokenDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/login/refresh",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = false,
            body = localVariableBody
        )
    }

    /**
     * 
     * 
     * @param userLoginDto 
     * @return TokensDto
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun loginControllerGetToken(userLoginDto: UserLoginDto) : TokensDto {
        val localVarResponse = loginControllerGetTokenWithHttpInfo(userLoginDto = userLoginDto)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as TokensDto
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * 
     * 
     * @param userLoginDto 
     * @return ApiResponse<TokensDto?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun loginControllerGetTokenWithHttpInfo(userLoginDto: UserLoginDto) : ApiResponse<TokensDto?> {
        val localVariableConfig = loginControllerGetTokenRequestConfig(userLoginDto = userLoginDto)

        return request<UserLoginDto, TokensDto>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation loginControllerGetToken
     *
     * @param userLoginDto 
     * @return RequestConfig
     */
    fun loginControllerGetTokenRequestConfig(userLoginDto: UserLoginDto) : RequestConfig<UserLoginDto> {
        val localVariableBody = userLoginDto
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/api/login",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = false,
            body = localVariableBody
        )
    }


    private fun encodeURIComponent(uriComponent: kotlin.String): kotlin.String =
        HttpUrl.Builder().scheme("http").host("localhost").addPathSegment(uriComponent).build().encodedPathSegments[0]
}
