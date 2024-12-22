package com.github.antonkonyshev.tryst

import com.github.antonkonyshev.tryst.data.UserMapper
import com.github.antonkonyshev.tryst.domain.User
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals

class UserMapperTestCase {
    @Test
    fun testDomainToDocument() = runBlocking {
        val now = Date().time
        val document = UserMapper.mapDomainToDocument(
            User(
                "testing-test", "Testing Test", 10.1, 20.2,
                now, "test"
            )
        )
        assertEquals(null, document.get("uid"))
        assertEquals("Testing Test", document.get("name"))
        assertEquals(10.1, document.get("latitude"))
        assertEquals(20.2, document.get("longitude"))
        assertEquals(now, document.get("timestamp"))
        assertEquals("test", document.get("group"))
    }

    @Test
    fun testDomainToData() = runBlocking {
        val now = Date().time
        val document = UserMapper.mapDomainToData(
            User(
                "testing-test", "Testing Test", 10.1, 20.2,
                now, "test"
            )
        )
        assertEquals("testing-test", document.get("uid"))
        assertEquals("Testing Test", document.get("name"))
        assertEquals(10.1, document.get("latitude"))
        assertEquals(20.2, document.get("longitude"))
        assertEquals(now, document.get("timestamp"))
        assertEquals("test", document.get("group"))
    }
}