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
        protected lateinit var service: GetMapService

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val cfg = Configuration(
                "http://getapp-dev.getapp.sh:3000",
                "rony@example.com",
                "rony123",
                "todo: storage path"
            )

            service = GetMapServiceFactory.createService(cfg)
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