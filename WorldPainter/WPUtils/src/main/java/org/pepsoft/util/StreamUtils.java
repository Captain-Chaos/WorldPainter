package org.pepsoft.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods for working with {@link InputStream}s and {@link OutputStream}s.
 */
public class StreamUtils {
    private StreamUtils() {
        // Prevent instantiation
    }

    /**
     * Load all bytes from an input stream and close it. Note that there may
     * not be more than {@link Integer#MAX_VALUE} bytes left in the stream.
     *
     * @param in The input stream to load.
     * @return All bytes from the input stream.
     * @throws IOException If an I/O error occurs while loading the bytes or
     * closing the stream.
     */
    public static byte[] load(InputStream in) throws IOException {
        try {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = BUFFERS.get();
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                in.close();
                return out.toByteArray();
            }
        } finally {
            // Don't obscure an existing exception with a secondary exception
            try {
                in.close();
            } catch (IOException | Error | RuntimeException e) {
                System.err.println("StreamUtils.load(InputStream): secondary I/O error while closing input stream");
                e.printStackTrace();
            }
        }
    }

    /**
     * Copy all bytes from the input stream to the output stream and close both
     * streams.
     *
     * @param in  The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If an I/O error occurs while copying the bytes or
     *                     closing the streams.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            copy(in, out, null, -1);
        } catch (ProgressReceiver.OperationCancelled e) {
            // This can't happen since we didn't provide a progress receiver.
            throw new InternalError(e);
        }
    }

    /**
     * Copy all bytes (up to a specified maximum) from the input stream to the
     * output stream and close both streams. This version allows the
     * specification of a maximum number of bytes to copy. If the input stream
     * contains more bytes, the method throws an {@link IOException}.
     *
     * @param in  The input stream to copy from.
     * @param out The output stream to copy to.
     * @param maximumBytes The maximum number of bytes to allow in the input
     *                     stream.
     * @throws IOException If an I/O error occurs while copying the bytes or
     *                     closing the streams, or if the input stream contained
     *                     more than the specified number of bytes.
     */
    public static void copy(InputStream in, OutputStream out, int maximumBytes) throws IOException {
        try {
            copy(in, out, null, maximumBytes);
        } catch (ProgressReceiver.OperationCancelled e) {
            // This can't happen since we didn't provide a progress receiver.
            throw new InternalError(e);
        }
    }

    /**
     * Copy all bytes from the input stream to the output stream and close both
     * streams.
     *
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @param progressReceiver An optional progress receiver to report copy
     *                         progress to.
     * @param byteCount The number of bytes in the input stream. If the input
     *                  stream contains <em>more</em> bytes, an
     *                  {@link IOException} is thrown. Specify {@code -1} to
     *                  disable a byte limit. If a progress receiver is
     *                  specified then this parameter is mandatory.
     * @throws IOException If an I/O error occurs while copying the bytes or
     *                     closing the streams, or if there were more.
     * @throws ProgressReceiver.OperationCancelled If a progress receiver was
     * provided and it threw an {@code OperationCancelled} exception.
     */
    public static void copy(InputStream in, OutputStream out, ProgressReceiver progressReceiver, long byteCount) throws IOException, ProgressReceiver.OperationCancelled {
        try {
            try {
                byte[] buffer = BUFFERS.get();
                int bytesRead;
                long bytesReadTotal = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    if ((byteCount > 0) && (bytesReadTotal + bytesRead > byteCount)) {
                        throw new IOException("Too many bytes in input stream (limit: " + byteCount + "; bytes read so far: " + (bytesReadTotal + bytesRead) + ")");
                    }
                    out.write(buffer, 0, bytesRead);
                    if (progressReceiver != null) {
                        bytesReadTotal += bytesRead;
                        progressReceiver.setProgress((float) byteCount / bytesReadTotal);
                    }
                }
                in.close();
                out.close();
            } finally {
                // Don't obscure an existing exception with a secondary exception
                try {
                    in.close();
                } catch (IOException | Error | RuntimeException e) {
                    System.err.println("StreamUtils.copy(InputStream, OutputStream, ProgressReceiver, long): secondary I/O error while closing input stream");
                    e.printStackTrace();
                }
            }
        } finally {
            // Don't obscure an existing exception with a secondary exception
            try {
                out.close();
            } catch (IOException | Error | RuntimeException e) {
                System.err.println("StreamUtils.copy(InputStream, OutputStream, ProgressReceiver, long): secondary I/O error while closing output stream");
                e.printStackTrace();
            }
        }
    }

    private static final int BUFFER_SIZE = 32768;
    private static final ThreadLocal<byte[]> BUFFERS = ThreadLocal.withInitial(() -> new byte[BUFFER_SIZE]);
}