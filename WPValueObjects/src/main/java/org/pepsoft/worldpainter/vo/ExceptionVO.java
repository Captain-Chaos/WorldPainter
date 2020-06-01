package org.pepsoft.worldpainter.vo;

import java.io.Serializable;

import static java.util.Arrays.stream;

/**
 * A canonical representation of an exception, including the stack trace and
 * chain of causes. While {@code Throwable} is {@code Serializable}, that only
 * works if the receiving system has the (exact same) exception class on its
 * classpath.
 */
public class ExceptionVO implements Serializable {
    public ExceptionVO(Throwable exception) {
        type = exception.getClass().getName();
        message = exception.getMessage();
        stackTrace = stream(exception.getStackTrace()).map(StackFrameVO::new).toArray(StackFrameVO[]::new);
        cause = (exception.getCause() != null) ? new ExceptionVO(exception.getCause()) : null;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public StackFrameVO[] getStackTrace() {
        return stackTrace;
    }

    public ExceptionVO getCause() {
        return cause;
    }

    private final String type, message;
    private final StackFrameVO[] stackTrace;
    private final ExceptionVO cause;

    private static final long serialVersionUID = 1L;

    public static class StackFrameVO implements Serializable {
        public StackFrameVO(StackTraceElement stackTraceElement) {
            class_ = stackTraceElement.getClassName();
            method = stackTraceElement.getMethodName();
            sourceFile = stackTraceElement.getFileName();
            lineNo = stackTraceElement.getLineNumber();
        }

        public String getClass_() {
            return class_;
        }

        public String getMethod() {
            return method;
        }

        public String getSourceFile() {
            return sourceFile;
        }

        public int getLineNo() {
            return lineNo;
        }

        private final String class_, method, sourceFile;
        private final int lineNo;

        private static final long serialVersionUID = 1L;
    }
}