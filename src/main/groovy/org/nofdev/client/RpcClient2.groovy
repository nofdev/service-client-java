package org.nofdev.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
class RpcClient2 {

    static <T> T build(Class<T> inter, Caller caller) {
        return build(inter, caller, null)
    }

    static  <T> T build(Class<T> inter, Caller caller, List<RpcFilter> _filters) {

        Caller lastCaller=decorateWithFilter(_filters, caller)

        return (T) Proxy.newProxyInstance(inter.getClassLoader(), [inter] as Class<?>[], new InvocationHandler() {
            @Override
            Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("hashCode" == method.getName()) {return inter.hashCode()}
                if ("toString" == method.getName()) {return inter.toString()}

                Request request = new Request()
                request.clazz = inter
                request.method = method
                request.args = args

                return lastCaller.call(request)
            }
        })
    }

    private static Caller decorateWithFilter(List<RpcFilter> _filters, Caller caller) {
        List<RpcFilter> filters = new ArrayList<>()
        //获取所有的 filter
        List<RpcFilter> extFilters = ExtensionLoader.getExtensionLoader(RpcFilter).getExtensions("")
        if (extFilters) filters.addAll(extFilters)
        if (_filters) filters.addAll(_filters)
        Collections.sort(filters, new ActivationComparator<RpcFilter>())

        //组装成链
        List<RpcFilter> filtersReverseList = filters.reverse()
        Caller lastCaller = caller
        filtersReverseList.each { RpcFilter f ->
            final Caller lp = lastCaller
            lastCaller = new Caller() {
                @Override
                Object call(Request request) {
                    return f.invoke(lp, request)
                }
            }
        }
        return lastCaller
    }
}
