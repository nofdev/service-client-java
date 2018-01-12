package org.nofdev.client.filter

import groovy.util.logging.Slf4j
import org.nofdev.core.Caller
import org.nofdev.core.RpcFilter
import org.nofdev.core.Request
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder

/**
 * Created by Liutengfei on 2017/10/26
 */
@Singleton
@Slf4j
class RpcContextFilter extends RpcFilter {
    @Override
    Object invoke(Caller caller, Request request) throws Throwable {
        ServiceContext serviceContext = ServiceContextHolder.getServiceContext()
        serviceContext.generateCallIdIfAbsent()

        Object result = null
        log.info("1 RpcContextFilter")
        result = caller.call(request)
        log.info("2 RpcContextFilter")
        return result
    }
}
