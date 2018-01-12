package org.nofdev.client.filter

import groovy.util.logging.Slf4j
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter
import org.nofdev.extension.Activation

/**
 * Created by Liutengfei on 2017/10/27
 */
@Activation(sequence = -80)
@Slf4j
class DurationTimeFilter implements RpcFilter {

    static final lineSeparator = System.properties.'line.separator'

    @Override
    Object invoke(Caller caller, Request request) throws Throwable {
        Object result = null
        def start = System.currentTimeMillis()
        try {
            log.info("1 DurationTimeFilter")
            result = caller.call(request)
            log.info("2 DurationTimeFilter")
        } finally {
            long millis = System.currentTimeMillis() - start
            def slow = ''
            if (millis > 500) {
                slow = "${lineSeparator}SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!${lineSeparator}"
                log.warn("${request.clazz}.${request.method?.getName()} result($slow$millis ms$slow)")
            } else {
                log.debug("${request.clazz}.${request.method?.getName()} result($slow$millis ms$slow)")
            }
        }
        return result
    }
}
