package com.tfg.movies.server.health

import com.tfg.movies.server.runWithApp
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRoutesTest {

    /**
     * /health should report HTTP 200 with database=up when the DB
     * is reachable. The test only asserts on the contract, not on
     * exact JSON formatting.
     */
    @Test
    fun `health returns 200 and reports database up`() = runWithApp { client ->
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"ok\""), "Expected status=ok in: $body")
        assertTrue(body.contains("\"database\":\"up\""), "Expected database=up in: $body")
    }
}