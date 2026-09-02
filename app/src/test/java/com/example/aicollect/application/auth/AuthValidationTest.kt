// Test unitario de AuthValidation: reglas de complejidad de contraseña y de formato de correo (RF-02).
package com.example.aicollect.application.auth

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `password shorter than 8 characters is rejected`() {
        assertNotNull(AuthValidation.passwordError("Ab1!ab"))
    }

    @Test
    fun `password without an uppercase letter is rejected`() {
        assertNotNull(AuthValidation.passwordError("secret123!"))
    }

    @Test
    fun `password without a lowercase letter is rejected`() {
        assertNotNull(AuthValidation.passwordError("SECRET123!"))
    }

    @Test
    fun `password without a digit is rejected`() {
        assertNotNull(AuthValidation.passwordError("Secretpass!"))
    }

    @Test
    fun `password without a symbol is rejected`() {
        assertNotNull(AuthValidation.passwordError("Secret1234"))
    }

    @Test
    fun `password with all required character classes is accepted`() {
        assertNull(AuthValidation.passwordError("Secret123!"))
    }

    @Test
    fun `blank email is rejected`() {
        assertNotNull(AuthValidation.emailError(""))
    }

    @Test
    fun `email without an at sign is rejected`() {
        assertNotNull(AuthValidation.emailError("usergmail.com"))
    }

    @Test
    fun `email with a well-formed institutional or corporate domain is accepted`() {
        assertNull(AuthValidation.emailError("user@uvigo.es"))
    }

    @Test
    fun `email with a well-known generic domain is accepted`() {
        assertNull(AuthValidation.emailError("user@gmail.com"))
    }

    @Test
    fun `email format check is case-insensitive on the domain`() {
        assertNull(AuthValidation.emailError("user@GMAIL.COM"))
    }

    @Test
    fun `email with a malformed domain is rejected`() {
        assertNotNull(AuthValidation.emailError("user@notadomain"))
    }
}
