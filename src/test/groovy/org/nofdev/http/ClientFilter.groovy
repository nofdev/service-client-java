package org.nofdev.http

import groovy.json.JsonBuilder
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter
import org.nofdev.extension.SpiMeta

@SpiMeta(order = 10)
class ClientFilter implements RpcFilter {

    @Override
    Object invoke(Caller caller, Request request) throws Throwable {
        Object result = null

        println "\n\n-----------------------------------------\n\n"
        println " 接口：${request.clazz}"
        println " 方法：${request.getMethod().getName()}"
        println " 参数类型：${request.getMethod().getGenericParameterTypes()}"
        println " 参数：${new JsonBuilder(request.args).toString()}"

        result = caller.call(request)

        println "\n\n-----------------------------------------\n\n"
        println " 执行结果：${new JsonBuilder(result).toString()}"
        println "\n\n-----------------------------------------\n\n"


        return result
    }
}