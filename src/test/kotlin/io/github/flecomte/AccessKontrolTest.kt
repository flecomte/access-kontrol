package io.github.flecomte

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

data class User(
    val name: String
)
data class MyObject(
    val title: String
)

class AccessControlSample : AccessKontrol() {
    fun canView(myObject: MyObject, user: User?): AccessResponse {
        return if (myObject.title == "granted" && user != null) {
            granted("ok")
        } else if (myObject.title == "wrong2") {
            denied("KO2", "ko2")
        } else {
            denied("KO", "ko")
        }
    }

    fun canView(myObjects: List<MyObject>, user: User?): AccessResponses {
        return canAll(myObjects) { canView(it, user) }
    }
}

class AccessKontrolTest {
    @Test
    fun `test granted`() {
        AccessControlSample().run {
            assertTrue(canView(MyObject("granted"), User("")).toBoolean())
        }
    }

    @Test
    fun `test denied`() {
        AccessControlSample().run {
            assertFalse(canView(MyObject("wrong"), User("")).toBoolean())
        }
    }

    @Test
    fun `test canAllGranted`() {
        AccessControlSample().run {
            assertTrue(
                canView(
                    listOf(
                        MyObject("granted"),
                        MyObject("granted")
                    ),
                    User("")
                ).first().toBoolean()
            )
        }
    }

    @Test
    fun `test CanAllDenied`() {
        AccessControlSample().run {
            assertFalse(
                canView(
                    listOf(
                        MyObject("granted"),
                        MyObject("wrong")
                    ),
                    User("")
                ).toBoolean()
            )
        }
    }

    @Test
    fun `test Assert on fail`() {
        assertThrows(AccessDeniedException::class.java) {
            AccessControlSample().canView(MyObject("denied"), User("")).assert()
        }
    }

    @Test
    fun `test Assert on success`() {
        AccessControlSample().assert { canView(MyObject("granted"), User("")) }
    }

    @Test
    fun `Exception tests`() {
        assertThrows(AccessDeniedException::class.java) {
            AccessControlSample().canView(listOf(MyObject("wrong"), MyObject("granted"), MyObject("wrong2")), User("")).assert()
        }.run {
            assertEquals("ko", first().code)
            assertTrue(hasErrorCode("ko"))
            assertFalse(hasErrorCode("notExists"))
            assertEquals("ko", getErrorCode("ko")?.code)
            assertEquals(null, getErrorCode("notExists")?.code)
            assertEquals("KO2", getMessages().last())
            assertEquals("KO", getFirstMessage())
        }
    }

    @Test
    fun `Assert success`() {
        AccessControlSample()
            .canView(listOf(MyObject("granted"), MyObject("granted")), User(""))
            .assert()
    }

    @Test
    fun `test getFirstDecisionResponse`() {
        AccessControlSample()
            .canView(listOf(MyObject("granted"), MyObject("granted")), User(""))
            .getFirstDecisionResponse()
            .run {
                assertTrue(decision.toBoolean())
            }
    }

    @Test
    fun `test getFirstDecisionResponse on denied`() {
        AccessControlSample()
            .canView(listOf(MyObject("granted"), MyObject("denied")), User(""))
            .getFirstDecisionResponse()
            .run {
                assertFalse(decision.toBoolean())
            }
    }

    @Test
    fun `GrantedResponse instantiation test`() {
        assertEquals(AccessDecision.GRANTED, GrantedResponse(AccessControlSample()).decision)
    }

    @Test
    fun `DeniedResponses must be throw exception if have no denied responses`() {
        assertThrows(Exception::class.java) {
            DeniedResponses(
                listOf(GrantedResponse(AccessControlSample())),
            )
        }
    }
}
