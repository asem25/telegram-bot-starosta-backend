package ru.semavin.telegrambot.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniAppGroupRequestValidationTest {
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

    @ParameterizedTest
    @ValueSource(strings = {
            "М3О-503С-22",
            "М11О-503БВ-22",
            "М3О-503Б-22"
    })
    void acceptsValidGroupNames(String groupName) {
        assertTrue(validator.validate(new MiniAppGroupRequest(groupName)).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "M3O-503C-22",
            "М3О-503C-22",
            "М3О-503АБВ-22",
            "М3О-50С-22",
            "М3О-503С-2",
            "М111О-503С-22",
            " М3О-503С-22",
            "М3О-503С-22 "
    })
    void rejectsInvalidGroupNames(String groupName) {
        assertFalse(validator.validate(new MiniAppGroupRequest(groupName)).isEmpty());
    }
}
