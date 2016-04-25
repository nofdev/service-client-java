package org.nofdev.http.oauth2
/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
class AuthenticationException extends RuntimeException {

    AuthenticationException() {
    }

    AuthenticationException(String message) {
        super(message)
    }

    AuthenticationException(String message, Throwable cause) {
        super(message, cause)
    }

    AuthenticationException(Throwable cause) {
        super(cause)
    }

    protected AuthenticationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace)
    }
}
