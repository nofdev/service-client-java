package org.nofdev.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter
import org.nofdev.client.filter.DurationTimeFilter
import org.nofdev.client.filter.RpcContextFilter

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException

/**
 * Created by liutengfei on 12/01/18.
 */
@CompileStatic
@Slf4j
class RpcProxy implements InvocationHandler {

    private List<RpcFilter> filters = new ArrayList<>()
    private Class<?> inter
    private Caller caller


    RpcProxy(Class<?> inter, Caller _caller) {
        this(inter, _caller,null)
    }
    RpcProxy(Class<?> inter, Caller _caller, LinkedList<RpcFilter> _filters) {
        this(inter, _caller,_filters,false)
    }

    RpcProxy(Class<?> inter, Caller _caller, LinkedList<RpcFilter> _filters,boolean forceReplaceFilter) {
        this.inter = inter
        if(!forceReplaceFilter){
            this.filters.add(RpcContextFilter.instance)
            this.filters.add(DurationTimeFilter.instance)
        }
        if(_filters) this.filters.addAll(_filters)
        this.caller = decorateWithFilter(_caller)
    }



    @Override
    Object invoke(Object proxy, Method method, Object[] args) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, Throwable {
        if ("hashCode" == method.getName()) {return inter.hashCode()}
        if ("toString" == method.getName()) {return inter.toString()}

        Request request = new Request()
        request.clazz = inter
        request.method = method
        request.args = args

        return caller.call(request)
    }

    Caller decorateWithFilter(Caller caller) {
        List<RpcFilter> filtersReverseList=filters.reverse()
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

    Object getObject() {
        Class<?>[] interfaces = [inter]
        return Proxy.newProxyInstance(inter.getClassLoader(), interfaces, this)
    }

    void setInter(Class<?> inter) {
        this.inter = inter
    }
}
