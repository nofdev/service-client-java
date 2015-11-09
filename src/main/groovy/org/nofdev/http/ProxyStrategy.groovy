package org.nofdev.http
import com.fasterxml.jackson.core.JsonProcessingException

import java.lang.reflect.Method
/**
 * Created by Qiang on 8/14/14.
 */
public interface ProxyStrategy {

    /**
     * 获取远程请求地址
     *
     * @param inter  代理的接口类
     * @param method 要调用的方法
     * @return
     */
    public String getRemoteURL(Class<?> inter, Method method);

    /**
     * 获取远程请求参数
     *
     * @param args 接口方法的请求参数
     * @return
     */
    public Map<String, String> getParams(Object[] args) throws JsonProcessingException;

    /**
     * 获取返回结果类
     *
     * @param method            要调用的方法
     * @param httpMessageSimple 远程请求返回结果
     * @return
     */
    public Object getResult(Method method, HttpMessageSimple httpMessageSimple) throws Throwable;
}
