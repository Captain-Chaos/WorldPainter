package org.pepsoft.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

/**
 * Utility class for reading and writing RFC 4180 CSV files. This class does not
 * support line breaks in fields, and it only supports files with headers.
 */
public class CSVDataSource {
    /**
     * Open a CSV formatted character stream for reading. A
     * {@code CSVDataSource} that has been used for reading once cannot be
     * reused.
     *
     * @param in The character stream to read.
     * @throws IOException If an I/O error occurs reading the headers or the
     * first row of data.
     */
    public void openForReading(Reader in) throws IOException {
        if ((this.in != null) || (out != null)) {
            throw new IllegalStateException("Already open");
        }
        if (in instanceof BufferedReader) {
            this.in = (BufferedReader) in;
        } else {
            this.in = new BufferedReader(in);
        }
        readHeaders();
        readValues();
    }

    /**
     * Open a CSV formatted character stream for writing. A
     * {@code CSVDataSource} that has been used for writing once cannot be
     * reused.
     *
     * @param out The character stream to write to.
     * @param columnNames The names of all the columns that will be written.
     * @throws IOException If an I/O error occurs writing the headers.
     */
    public void openForWriting(Writer out, String... columnNames) throws IOException {
        if ((in != null) || (this.out != null)) {
            throw new IllegalStateException("Already open");
        }
        this.out = out;
        columnsByIndex = asList(columnNames);
        columnsByName = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            columnsByName.put(columnNames[i], i);
        }
        writeHeaders();
        currentRow = asList(new String[columnNames.length]);
    }

    /**
     * Indicates whether the end of the file has been reached. Only applicable
     * when reading.
     *
     * @return {@code true} if the end of the file has been reached.
     */
    public boolean isEndOfFile() {
        return currentRow == null;
    }

    /**
     * Advance to the next row, if any. When writing this will write a row of
     * values to the stream. When reading, {@link #isEndOfFile()} should be
     * invoked afterwards, and before trying to get data, to determine whether
     * the end of the file had been reached.
     *
     * @throws IOException If an I/O error occurs reading from or writing to the
     * stream.
     */
    public void next() throws IOException {
        if (in != null) {
            readValues();
        } else {
            writeValues();
        }
    }

    /**
     * Get a string-typed value by column name.
     *
     * @param columnName The name of the column.
     * @return The value of the specified column in the current row.
     */
    public String getString(String columnName) {
        return getString(columnsByName.get(columnName));
    }

    /**
     * Get a string-typed value by column index.
     *
     * @param columnIndex The index of the column.
     * @return The value of the specified column in the current row.
     */
    public String getString(int columnIndex) {
        return currentRow.get(columnIndex);
    }

    /**
     * Set a string-typed value by column name. {@code null} values are
     * supported but are converted into empty strings.
     *
     * @param columnName The name of the column.
     * @param value The value to store in the column.
     */
    public void setString(String columnName, String value) {
        setString(columnsByName.get(columnName), value);
    }

    /**
     * Set a string-typed value by column index. {@code null} values are
     * supported but are converted into empty strings.
     *
     * @param columnIndex The index of the column.
     * @param value The value to store in the column.
     */
    public void setString(int columnIndex, String value) {
        currentRow.set(columnIndex, value);
    }

    public int getInt(String columnName) {
        return Integer.parseInt(getString(columnName));
    }

    public void setInt(String columnName, int value) {
        setString(columnName, Integer.toString(value));
    }

    public boolean getBoolean(String columnName) {
        return Boolean.parseBoolean(getString(columnName));
    }

    public void setBoolean(String columnName, boolean value) {
        setString(columnName, Boolean.toString(value));
    }

    private void readHeaders() throws IOException {
        List<String> headers = readLine();
        columnsByName = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            columnsByName.put(headers.get(i), i);
        }
    }

    private void writeHeaders() throws IOException {
        out.write(columnsByIndex.stream().map(this::quoteIfNecessary).collect(joining(",")));
    }

    private void readValues() throws IOException {
        currentRow = readLine();
    }

    private void writeValues() throws IOException {
        out.write("\r\n");
        out.write(currentRow.stream()
                .map(str -> (str != null) ? str : "")
                .map(this::quoteIfNecessary)
                .collect(joining(",")));
        currentRow.replaceAll(str -> null);
    }

    private String quoteIfNecessary(String text) {
        return (text.contains(",") || text.contains("\""))
                ? ('"' + text.replace("\"", "\"\"") + '"')
                : text;
    }

    private List<String> readLine() throws IOException {
        String line = in.readLine();
        if (line == null) {
            return null;
        }

        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        final int IDLE = 0;
        final int READING_QUOTED_VALUE = 1;
        final int READING_UNQUOTED_VALUE = 2;
        final int QUOTE_ENCOUNTERED_IN_QUOTED_VALUE = 3;
        int state = IDLE;
        for (char c: line.toCharArray()) {
            switch (state) {
                case IDLE:
                    if (c == '"') {
                        state = READING_QUOTED_VALUE;
                    } else if (c == ',') {
                        values.add(currentValue.toString());
                        currentValue.setLength(0);
                    } else {
                        currentValue.append(c);
                        state = READING_UNQUOTED_VALUE;
                    }
                    break;
                case READING_QUOTED_VALUE:
                    if (c == '"') {
                        state = QUOTE_ENCOUNTERED_IN_QUOTED_VALUE;
                    } else {
                        currentValue.append(c);
                    }
                    break;
                case READING_UNQUOTED_VALUE:
                    if (c == ',') {
                        values.add(currentValue.toString());
                        currentValue.setLength(0);
                        state = IDLE;
                    } else {
                        currentValue.append(c);
                    }
                    break;
                case QUOTE_ENCOUNTERED_IN_QUOTED_VALUE:
                    if (c == '"') {
                        currentValue.append('"');
                        state = READING_QUOTED_VALUE;
                    } else if (c == ',') {
                        values.add(currentValue.toString());
                        currentValue.setLength(0);
                        state = IDLE;
                    } else {
                        throw new IOException("Single double quote encountered in quoted field (line: \"" + line + "\")");
                    }
                    break;
            }
        }
        switch (state) {
            case IDLE:
            case READING_UNQUOTED_VALUE:
            case QUOTE_ENCOUNTERED_IN_QUOTED_VALUE:
                values.add(currentValue.toString());
                break;
            case READING_QUOTED_VALUE:
                throw new IOException("Quoted field not closed (line: \"" + line + "\")");
        }

        return values;
    }

    private BufferedReader in;
    private Writer out;
    private Map<String, Integer> columnsByName;
    private List<String> columnsByIndex;
    private List<String> currentRow;
}