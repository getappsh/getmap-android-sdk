package com.ngsoft.getapp.sdk

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ngsoft.getapp.sdk.test", appContext.packageName)
    }

    @Test
    fun login_isOk() {
        val configuration = Configuration()
        configuration.baseUrl = "http://getapp-dev.getapp.sh:3000"
        configuration.user = "rony@example.com"
        configuration.password = "rony123"

        val loginApi = LoginApi(configuration.baseUrl ?: "localhost")
        val cred = UserLoginDto(
            configuration.user ?: "user",
            configuration.password ?: "password"
        )

        val tokens: TokensDto = loginApi.loginControllerGetToken(cred)

        assert(!tokens.accessToken.isNullOrEmpty())
        println("\naccessToken:\n${tokens.accessToken}\n")

    }

}