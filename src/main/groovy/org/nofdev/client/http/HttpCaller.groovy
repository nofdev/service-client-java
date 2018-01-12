package org.nofdev.client.http

import com.fasterxml.jackson.databind.ObjectMapper
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.http.DefaultRequestConfig
import org.nofdev.http.HttpClientUtil
import org.nofdev.http.HttpMessageWithHeader
import org.nofdev.http.PoolingConnectionManagerFactory
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.nofdev.servicefacade.ServiceNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Liutengfei on 2017/10/26
 */
class HttpCaller implements Caller {
    private static final Logger logger = LoggerFactory.getLogger(HttpCaller.class)

    ProxyStrategy proxyStrategy
    DefaultRequestConfig defaultRequestConfig
    PoolingConnectionManagerFactory connectionManagerFactory


    HttpCaller(ProxyStrategy proxyStrategy, DefaultRequestConfig defaultRequestConfig, PoolingConnectionManagerFactory connectionManagerFactory) {
        this.proxyStrategy = proxyStrategy

        if (connectionManagerFactory == null) {
            try {
                this.connectionManagerFactory = new PoolingConnectionManagerFactory()
            } catch (Exception e) {
                logger.error("new PoolingConnectionManagerFactory Fail", e)
            }
        } else {
            this.connectionManagerFactory = connectionManagerFactory
        }

        if (defaultRequestConfig == null) {
            this.defaultRequestConfig = new DefaultRequestConfig()
        } else {
            this.defaultRequestConfig = defaultRequestConfig
        }
    }

    /**
     * 不支持不被信任证书的 ssl 访问, 请使用支持传入new PoolingConnectionManagerFactory(true)的构造函数
     * @param proxyStrategy
     */
    HttpCaller(ProxyStrategy proxyStrategy) {
        this(proxyStrategy, null, null)
    }

    /**
     * 不支持不被信任证书的 ssl 访问, 请使用支持传入new PoolingConnectionManagerFactory(true)的构造函数
     * @param inter
     * @param url
     */
    HttpCaller(String url) {
        this(new DefaultProxyStrategyImpl(url))
    }

    private Map<String, String> serviceContextToMap(ServiceContext serviceContext) {
        Map<String, String> context = new HashMap<>();
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
        serviceContext?.forEach({ String k, Object v ->
            if (v instanceof String) {
                context.put(k, v)
            } else {
                context.put(k, objectMapper.writeValueAsString(v))
            }
        })
        context
    }

    @Override
    Object call(Request request) {
        String remoteURL = proxyStrategy.getRemoteURL(request.clazz, request.method)
        HttpClientUtil httpClientUtil = new HttpClientUtil(connectionManagerFactory, defaultRequestConfig)
        logger.debug("Default connection pool idle connection time is " + connectionManagerFactory.getIdleConnTimeout())
        Map<String, String> params = proxyStrategy.getParams(request.args)

        ServiceContext serviceContext = ServiceContextHolder.getServiceContext()
        Map<String, String> context = serviceContextToMap(serviceContext)

        logger.debug("RPC call: ${remoteURL} ${ObjectMapperFactory.createObjectMapper().writeValueAsString(params)}")
        HttpMessageWithHeader httpMessageWithHeader = httpClientUtil.postWithHeader(remoteURL, params, context)
        if (httpMessageWithHeader.statusCode == 404) {
            throw new ServiceNotFoundException()
        }
        def result = proxyStrategy.getResult(request.method, httpMessageWithHeader)
        return result
    }


}
