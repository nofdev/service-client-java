package org.nofdev.client.filter

import groovy.util.logging.Slf4j
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter
import org.nofdev.extension.SpiMeta
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder

/**
 * Created by Liutengfei on 2017/10/26
 */
@SpiMeta(order = -100)
@Slf4j
class RpcContextFilter implements RpcFilter {
    @Override
    Object invoke(Caller caller, Request request) throws Throwable {
        ServiceContext serviceContext = ServiceContextHolder.getServiceContext()
        serviceContext.generateCallIdIfAbsent()

        return caller.call(request)
    }
}
