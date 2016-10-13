package org.nofdev.http

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

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

    /**
     * 不支持不被信任证书的 ssl 访问, 请使用支持传入new PoolingConnectionManagerFactory(true)的构造函数
     * @param inter
     * @param proxyStrategy
     */
    public HttpJsonProxy(Class<?> inter, ProxyStrategy proxyStrategy) {
        this(inter, proxyStrategy, null, null);
    }

    /**
     * 不支持不被信任证书的 ssl 访问, 请使用支持传入new PoolingConnectionManagerFactory(true)的构造函数
     * @param inter
     * @param url
     */
    public HttpJsonProxy(Class<?> inter, String url) {
        this(inter, new DefaultProxyStrategyImpl(url));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, Throwable {
        def start = new Date()
        final endl = System.properties.'line.separator'

        def serviceContext = ServiceContextHolder.getServiceContext()

        if(serviceContext?.getCallId()){
            MDC.put(ServiceContext.CALLID.toString(), ObjectMapperFactory.createObjectMapper().writeValueAsString(serviceContext?.getCallId()))
        }

        if ("hashCode".equals(method.getName())) {
            return inter.hashCode();
        }
        if("toString".equals(method.getName())){
            return inter.toString();
        }

        String remoteURL = proxyStrategy.getRemoteURL(inter, method);
        HttpClientUtil httpClientUtil = new HttpClientUtil(connectionManagerFactory, defaultRequestConfig);
        logger.debug("Default connection pool idle connection time is " + connectionManagerFactory.getIdleConnTimeout());
        Map<String, String> params = proxyStrategy.getParams(args)
        Map<String, String> context = serviceContextToMap(serviceContext)

        logger.debug("RPC call: ${remoteURL} ${ObjectMapperFactory.createObjectMapper().writeValueAsString(params)}");
        HttpMessageWithHeader response = httpClientUtil.postWithHeader(remoteURL, params, context);

        def result = proxyStrategy.getResult(method, response)

        def end = new Date()
        long millis = end.time - start.time
        def slow = ''
        if(millis > 500) {
            slow = "${endl}SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!${endl}"
            logger.warn("${inter}.${method?.getName()} result($slow$millis ms$slow): ${endl}${ObjectMapperFactory.createObjectMapper().writeValueAsString(result)}")
        }else {
            logger.debug("${inter}.${method?.getName()} result($slow$millis ms$slow): ${endl}${ObjectMapperFactory.createObjectMapper().writeValueAsString(result)}")
        }

        result
    }

    private Map<String, String> serviceContextToMap(ServiceContext serviceContext) {
        Map<String, String> context = new HashMap<>();
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
        serviceContext?.forEach({String k,Object v->
            context.put(k, objectMapper.writeValueAsString(v));
        })
        context
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
