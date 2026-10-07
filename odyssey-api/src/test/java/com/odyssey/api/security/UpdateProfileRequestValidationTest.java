package com.odyssey.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UpdateProfileRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validRequestHasNoViolations() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            "Martin",
            "+33600000000",
            "+33600000001",
            "fr"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void whatsappNumberIsOptional() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            "Martin",
            "+33600000000",
            null,
            "fr"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void blankFirstNameIsRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            " ",
            "Martin",
            "+33600000000",
            null,
            "fr"
        );

        assertViolationOn(request, "firstName");
    }

    @Test
    void blankLastNameIsRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            " ",
            "+33600000000",
            null,
            "fr"
        );

        assertViolationOn(request, "lastName");
    }

    @Test
    void blankPhoneNumberIsRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            "Martin",
            " ",
            null,
            "fr"
        );

        assertViolationOn(request, "phoneNumber");
    }

    @Test
    void blankPreferredLanguageIsRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            "Martin",
            "+33600000000",
            null,
            " "
        );

        assertViolationOn(request, "preferredLanguage");
    }

    private void assertViolationOn(
        UpdateProfileRequest request,
        String propertyName
    ) {
        Set<ConstraintViolation<UpdateProfileRequest>> violations =
            validator.validate(request);

        assertEquals(1, violations.size());
        assertTrue(
            violations.stream()
                .anyMatch(violation -> violation.getPropertyPath()
                    .toString()
                    .equals(propertyName)),
            () -> "Expected a violation on '" + propertyName + "' but got: " + violations
        );
    }
}