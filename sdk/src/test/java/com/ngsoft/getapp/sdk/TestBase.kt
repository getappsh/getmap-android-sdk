package com.ngsoft.getapp.sdk

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass

open class TestBase {
    companion object {
        init {
            println("init...")
        }

        @JvmStatic
        protected lateinit var api: GetMapService

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val cfg = Configuration()
            cfg.baseUrl = "http://getapp-dev.getapp.sh:3000"
            cfg.user = "rony@example.com"
            cfg.password = "rony123"

            api = GetMapServiceImpl(cfg)
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            println("Test teardown...")
        }
    }

    @Before
    fun prepareTest() {
        println("Test prepare...")
    }

    @After
    fun cleanupTest() {
        println("Test cleanup...")
    }
}