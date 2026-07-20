package com.example.clinic.user;

import java.util.regex.Pattern;

public final class PasswordPolicy {
    private static final Pattern ALNUM = Pattern.compile("^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{8,}$");

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && ALNUM.matcher(password).matches();
    }
}
