package org.pepsoft.worldpainter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class StartupMessages {
    public static void addError(String error) {
        ERRORS.add(error);
    }

    public static void addWarning(String warning) {
        WARNINGS.add(warning);
    }

    public static void addMessage(String message) {
        MESSAGES.add(message);
    }

    public static List<String> getErrors() {
        return unmodifiableList(ERRORS);
    }

    public static List<String> getWarnings() {
        return unmodifiableList(WARNINGS);
    }

    public static List<String> getMessages() {
        return unmodifiableList(MESSAGES);
    }

    private static final List<String> ERRORS = new ArrayList<>(), WARNINGS = new ArrayList<>(), MESSAGES = new ArrayList<>();
}