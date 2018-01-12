package org.nofdev.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.nofdev.client.http.HttpCaller
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter
import org.nofdev.extension.ActivationComparator
import org.nofdev.extension.ExtensionLoader

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Created by liutengfei on 12/01/18.
 */
@CompileStatic
@Slf4j
class RpcClient {

    static <T> T build(Class<T> inter, Caller caller) {
        return new RpcProxy<T>(inter, caller).getObject()
    }

    static <T> T build(Class<T> inter, Caller caller, List<RpcFilter> _filters) {
        return new RpcProxy<T>(inter, caller, _filters).getObject()
    }
}
