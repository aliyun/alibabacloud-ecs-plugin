package com.alibabacloud.jenkins.ecs.exception;

/**
 * Created by kunlun.ykl on 2020/9/25.
 */
public class AlibabaEcsException extends Exception {
    public AlibabaEcsException() {
    }

    public AlibabaEcsException(String message) {
        super(message);
    }

    public AlibabaEcsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlibabaEcsException(Throwable cause) {
        super(cause);
    }

    public AlibabaEcsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
