package org.nofdev.servicefacade

/**
 * Created by Qiang on 11/2/15.
 */
class ServiceNotFoundException extends AbstractBusinessException{
    static String DEFAULT_EXCEPTION_MESSAGE = "该服务不存在";

    ServiceNotFoundException() {
        super(DEFAULT_EXCEPTION_MESSAGE)
    }

    ServiceNotFoundException(String message) {
        super(message)
    }

    ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause)
    }

    ServiceNotFoundException(Throwable cause) {
        super(cause)
    }
}
