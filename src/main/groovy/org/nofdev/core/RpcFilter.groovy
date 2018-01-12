package org.nofdev.core
/**
 * Created by Liutengfei on 2017/10/26
 */
abstract class RpcFilter {
    abstract Object invoke(Caller caller, Request request) throws Throwable
}