package com.Vishleshan.Api.util;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email must not be empty"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}