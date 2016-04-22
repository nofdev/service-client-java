package org.nofdev.http.oauth2
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.nofdev.http.*
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
/**
 * Created by HouDongQiang on 2016/3/10.
 * for OAuth proxy handler. 用于OAuth认证的代理器
 */
@CompileStatic
class OAuthJsonProxy implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(OAuthJsonProxy.class);

    private Class<?> inter
    private OAuthProxyStrategyImpl oAuthProxyStrategy
    private PoolingConnectionManagerFactory poolingConnectionManagerFactory
    private DefaultRequestConfig defaultRequestConfig

    public OAuthJsonProxy(Class<?> inter, OAuthProxyStrategyImpl oAuthProxyStrategy, PoolingConnectionManagerFactory poolingConnectionManagerFactory, DefaultRequestConfig defaultRequestConfig) {
        this.inter = inter
        this.oAuthProxyStrategy = oAuthProxyStrategy

        if (poolingConnectionManagerFactory == null) {
            try {
                this.poolingConnectionManagerFactory = new PoolingConnectionManagerFactory();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.poolingConnectionManagerFactory = poolingConnectionManagerFactory;
        }
        if (defaultRequestConfig == null) {
            this.defaultRequestConfig = new DefaultRequestConfig();
        } else {
            this.defaultRequestConfig = defaultRequestConfig;
        }
    }

    public OAuthJsonProxy(Class<?> inter, OAuthProxyStrategyImpl oAuthProxyStrategy) {
        this(inter, oAuthProxyStrategy, null, null)
    }

    @Override
    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        def serviceContext = ServiceContextHolder.getServiceContext()

        if ("hashCode".equals(method.getName())) {
            return inter.hashCode();
        }
        if("toString".equals(method.getName())){
            return inter.toString();
        }

        String remoteURL = oAuthProxyStrategy.getRemoteURL(inter, method);
        OAuthURLConnectionClient httpClientUtil = new OAuthURLConnectionClient(poolingConnectionManagerFactory, defaultRequestConfig)
        Map<String, String> params = oAuthProxyStrategy.getParams(args)
        Map<String, String> context = serviceContextToMap(serviceContext)

        logger.info("RPC call: ${remoteURL} ${ObjectMapperFactory.createObjectMapper().writeValueAsString(params)}");

        HttpMessageWithHeader response = oAuthProxyStrategy.resource(httpClientUtil,remoteURL, params, context);
        return oAuthProxyStrategy.getResult(method, response)
    }


    private Map<String, String> serviceContextToMap(ServiceContext serviceContext) {
        Map<String, String> context = new HashMap<>();
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
        if (serviceContext != null && serviceContext.getCallId() != null) {
            context.put(ServiceContext.CALLID.toString(), objectMapper.writeValueAsString(serviceContext.getCallId()));
        }
        context
    }

    public Object getObject() {
        Class<?>[] interfaces = [inter];
        return Proxy.newProxyInstance(inter.getClassLoader(), interfaces, this);
    }

}
