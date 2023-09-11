package com.ngsoft.getappclient

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import com.ngsoft.sharedtest.LoginTest
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LoginTest {
    @Test
    fun login_isOk() {
        LoginTest().login2GetApp()
    }
}