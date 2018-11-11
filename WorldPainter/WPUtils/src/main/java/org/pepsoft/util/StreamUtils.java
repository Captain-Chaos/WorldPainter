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
     * Copy all bytes from the input stream to the output stream and close both
     * streams.
     *
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @param progressReceiver An optional progress receiver to report copy
     *                         progress to.
     * @param byteCount The number of bytes in the input stream. This parameter
     *                  is only consulted if a progress receiver was provided.
     * @throws IOException If an I/O error occurs while copying the bytes or
     *                     closing the streams.
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