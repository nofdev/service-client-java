package org.nofdev.servicefacade;

/**
 * 未知异常
 * <p/>
 * 由对外暴露服务的最后一层捕获（如Controller）而不是Service中捕获的系统异常都要转化为未知异常，未知异常也是一种业务异常
 * <p/>
 * 这只是一个建议，每个应用都可以实现自己的UnhandledException
 * <p/>
 * 如果抛出这种异常，建议记录error级别的日志
 */
class UnhandledException extends AbstractBusinessException {
    static String DEFAULT_EXCEPTION_MESSAGE = "系统未知异常";

    UnhandledException() {
        super(DEFAULT_EXCEPTION_MESSAGE)
    }

    UnhandledException(String message) {
        super(message)
    }

    UnhandledException(String message, Throwable cause) {
        super(message, cause)
    }

    UnhandledException(Throwable cause) {
        super(cause)
    }
}
