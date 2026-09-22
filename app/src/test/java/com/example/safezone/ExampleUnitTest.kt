package com.example.safezone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    // ==============================
    // FULL NAME TESTS
    // ==============================

    @Test
    fun validFullName_isAccepted() {
        assertTrue(
            ValidationUtils.isValidFullName("Thabelo Mahada")
        )
    }

    @Test
    fun invalidFullName_isRejected() {
        assertFalse(
            ValidationUtils.isValidFullName("A")
        )
    }

    @Test
    fun emptyFullName_isRejected() {
        assertFalse(
            ValidationUtils.isValidFullName("")
        )
    }

    @Test
    fun whitespaceFullName_isRejected() {
        assertFalse(
            ValidationUtils.isValidFullName("   ")
        )
    }


    // ==============================
    // EMAIL TESTS
    // ==============================

    @Test
    fun validEmail_isAccepted() {
        assertTrue(
            ValidationUtils.isValidEmail("student@example.com")
        )
    }

    @Test
    fun invalidEmail_isRejected() {
        assertFalse(
            ValidationUtils.isValidEmail("studentexample.com")
        )
    }

    @Test
    fun emptyEmail_isRejected() {
        assertFalse(
            ValidationUtils.isValidEmail("")
        )
    }

    @Test
    fun emailWithoutDomain_isRejected() {
        assertFalse(
            ValidationUtils.isValidEmail("student@")
        )
    }


    // ==============================
    // PASSWORD TESTS
    // ==============================

    @Test
    fun validPassword_isAccepted() {
        assertTrue(
            ValidationUtils.isValidPassword("SafeZone1")
        )
    }

    @Test
    fun weakPassword_isRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("password")
        )
    }

    @Test
    fun shortPassword_isRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("Sa1")
        )
    }

    @Test
    fun passwordWithoutUppercase_isRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("safezone1")
        )
    }

    @Test
    fun passwordWithoutLowercase_isRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("SAFEZONE1")
        )
    }

    @Test
    fun passwordWithoutNumber_isRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("SafeZone")
        )
    }


    // ==============================
    // PASSWORD MATCHING TESTS
    // ==============================

    @Test
    fun matchingPasswords_areAccepted() {
        assertTrue(
            ValidationUtils.passwordsMatch(
                "SafeZone1",
                "SafeZone1"
            )
        )
    }

    @Test
    fun differentPasswords_areRejected() {
        assertFalse(
            ValidationUtils.passwordsMatch(
                "SafeZone1",
                "SafeZone2"
            )
        )
    }

    @Test
    fun emptyPasswords_areAcceptedWhenBothEmpty() {
        assertTrue(
            ValidationUtils.passwordsMatch(
                "",
                ""
            )
        )
    }

    @Test
    fun passwordAndEmptyConfirmation_areRejected() {
        assertFalse(
            ValidationUtils.passwordsMatch(
                "SafeZone1",
                ""
            )
        )
    }
}