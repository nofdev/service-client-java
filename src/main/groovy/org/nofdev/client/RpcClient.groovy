package org.nofdev.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.nofdev.client.filter.DurationTimeFilter
import org.nofdev.client.filter.RpcContextFilter
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter

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
        return build(inter, caller, null)
    }
    static  <T> T build(Class<T> inter, Caller caller, List<RpcFilter> _filters) {
        return build(inter,caller,_filters,false)
    }
    static  <T> T build(Class<T> inter, Caller caller, List<RpcFilter> _filters,boolean forceReplaceFilter) {
        List<RpcFilter> filters = new ArrayList<>()
        if(!forceReplaceFilter){
            filters.add(RpcContextFilter.instance)
            filters.add(DurationTimeFilter.instance)
        }
        if (_filters) filters.addAll(_filters)


        Caller lastCaller=decorateWithFilter(filters, caller)
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


    static Caller decorateWithFilter(List<RpcFilter> filters, Caller caller) {
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
