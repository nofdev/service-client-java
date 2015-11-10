package org.nofdev.http

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.nofdev.servicefacade.CallId
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException

/**
 * Created by Qiang on 6/3/14.
 */
@CompileStatic
public class HttpJsonProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpJsonProxy.class);

    private Class<?> inter;

    private ProxyStrategy proxyStrategy;
    private DefaultRequestConfig defaultRequestConfig;
    private PoolingConnectionManagerFactory connectionManagerFactory;

    public HttpJsonProxy(Class<?> inter, ProxyStrategy proxyStrategy, PoolingConnectionManagerFactory connectionManagerFactory, DefaultRequestConfig defaultRequestConfig) {
        this.inter = inter;
        this.proxyStrategy = proxyStrategy;
        if (connectionManagerFactory == null) {
            try {
                this.connectionManagerFactory = new PoolingConnectionManagerFactory();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.connectionManagerFactory = connectionManagerFactory;
        }
        if (defaultRequestConfig == null) {
            this.defaultRequestConfig = new DefaultRequestConfig();
        } else {
            this.defaultRequestConfig = defaultRequestConfig;
        }
    }

    public HttpJsonProxy(Class<?> inter, ProxyStrategy proxyStrategy) {
        this(inter, proxyStrategy, null, null);
    }

    public HttpJsonProxy(Class<?> inter, String url) {
        this(inter, new DefaultProxyStrategyImpl(url));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, Throwable {

        if ("hashCode".equals(method.getName())) {
            return inter.hashCode();
        }

        String remoteURL = proxyStrategy.getRemoteURL(inter, method);
        HttpClientUtil httpClientUtil = new HttpClientUtil(connectionManagerFactory, defaultRequestConfig);
        logger.debug("Default connection pool idle connection time is " + connectionManagerFactory.getIdleConnTimeout());
        Map<String, String> params = proxyStrategy.getParams(args);

        ServiceContext serviceContext = ServiceContextHolder.getServiceContext();
        Map<String, String> context = serviceContextToMap(serviceContext)
        HttpMessageWithHeader response = httpClientUtil.postWithHeader(remoteURL, params, context);
        serviceContext = mapToServiceContext(response.headers)
        ServiceContextHolder.setServiceContext(serviceContext)

        return proxyStrategy.getResult(method, response);

    }

    private Map<String, String> serviceContextToMap(ServiceContext serviceContext) {
        Map<String, String> context = new HashMap<>();
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
        if (serviceContext != null && serviceContext.getCallId() != null) {
            context.put(ServiceContext.CALLID.toString(), objectMapper.writeValueAsString(serviceContext.getCallId()));
        }
        context
    }

    private ServiceContext mapToServiceContext(Map<String, String> header) {

        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
        def serviceContext = ServiceContextHolder.getServiceContext()
        header?.each { k, v ->
            if (k == ServiceContext.CALLID) {
                def callId = objectMapper.readValue(v, CallId)
                serviceContext.getCallId()?.parent = callId?.id
            } else if (k.startsWith(ServiceContext.PREFIX.toString())) {
                serviceContext.put(k, v)
            } else {
                //什么都不干
            }
        }
        serviceContext
    }

    public Object getObject() {
        Class<?>[] interfaces = [inter];
        return Proxy.newProxyInstance(inter.getClassLoader(), interfaces, this);
    }

    public void setInter(Class<?> inter) {
        this.inter = inter;
    }

    public void setDefaultRequestConfig(DefaultRequestConfig defaultRequestConfig) {
        this.defaultRequestConfig = defaultRequestConfig;
    }

    public void setConnectionManagerFactory(PoolingConnectionManagerFactory connectionManagerFactory) {
        this.connectionManagerFactory = connectionManagerFactory;
    }

    public void setProxyStrategy(ProxyStrategy proxyStrategy) {
        this.proxyStrategy = proxyStrategy;
    }

}
