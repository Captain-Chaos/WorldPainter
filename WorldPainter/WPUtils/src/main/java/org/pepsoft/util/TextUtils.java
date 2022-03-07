package org.pepsoft.util;

public class TextUtils {
    /**
     * Breaks a text into lines of a specified maximum length and returns one the resulting lines. Existing line endings
     * and other whitespace in the text is ignored. This method does not break words, so if there are words in the text
     * that are longer than the maximum line length it will result in lines that are longer than the specified maximum
     * length.
     *
     * @param text          The text to break up.
     * @param maxLineLength The maximum length of the lines.
     * @param line          The line to return.
     * @return The specified line from the text broken up in lines. If the requested line lies beyond the end of the
     * text, the empty string is returned.
     */
    public static String getLine(String text, int maxLineLength, int line) {
        final String[] tokens = text.split("\\s+");
        final StringBuilder currentLine = new StringBuilder();
        int lineNo = 0;
        for (String token: tokens) {
            if (currentLine.length() + token.length() + ((currentLine.length() == 0) ? 0 : 1) <= maxLineLength) {
                // The token fits on the current line
                if (currentLine.length() > 0) {
                    currentLine.append(' ');
                }
                currentLine.append(token);
            } else if (currentLine.length() == 0) {
                // The token does not fit on the current line, but the current line is empty so we have no choice but to
                // consume the token anyway and continue with the next line, or return it
                if (lineNo == line) {
                    return token;
                } else {
                    lineNo++;
                }
            } else if (lineNo < line) {
                // The token does not fit on the current line and we have not yet arrived at the requested line; start a
                // new line
                currentLine.setLength(0);
                currentLine.append(token);
                lineNo++;
            } else {
                // The token does not fit on the current line and we are at the requested line, so return it
                return currentLine.toString();
            }
        }
        if (lineNo == line) {
            return currentLine.toString();
        } else {
            return "";
        }
    }
}