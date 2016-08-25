package org.nofdev.http.batch

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import groovy.transform.CompileStatic
import org.nofdev.http.*
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.function.Consumer

/**
 * Created by LiuTengfei on 20/8/16.
 */
@CompileStatic
public class BatchJsonProxy {
    private static final Logger logger = LoggerFactory.getLogger(BatchJsonProxy.class);

    private Class<?> inter;

    private DefaultRequestConfig defaultRequestConfig;
    private PoolingConnectionManagerFactory connectionManagerFactory;

    private String serverUrl;
    private Object proxy_target;

    private Multimap<Method, Object[]> multiMap = ArrayListMultimap.create();

    public BatchJsonProxy(Class<?> inter, String url, PoolingConnectionManagerFactory connectionManagerFactory, DefaultRequestConfig defaultRequestConfig) throws Exception {
        this.inter = inter;
        this.serverUrl = url;
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

        Class<?>[] interfaces = [this.inter];
        proxy_target = Proxy.newProxyInstance(inter.getClassLoader(), interfaces, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("hashCode".equals(method.getName())) {
                    return null;
                }
                if ("toString".equals(method.getName())) {
                    return null;
                }
                multiMap.put(method, args);
                return null;
            }
        });
        println "+++++++++" + proxy_target
    }

    public BatchJsonProxy(Class<?> inter, String url) throws Exception {
        this(inter, url, null, null);
    }


    void batchExec(Consumer consumer) throws Throwable {
        consumer.accept(proxy_target);

        Date start = new Date();
        final String lineSeparator = System.getProperty("line.separator");
        ServiceContext serviceContext = ServiceContextHolder.getServiceContext();
        if (serviceContext != null && serviceContext.getCallId() != null) {
            MDC.put(ServiceContext.CALLID.toString(), ObjectMapperFactory.createObjectMapper().writeValueAsString(serviceContext.getCallId()));
        }

        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

        HttpClientUtil httpClientUtil = new HttpClientUtil(this.connectionManagerFactory, this.defaultRequestConfig);

        BatchProxyStrategyImpl proxyStrategy = new BatchProxyStrategyImpl(serverUrl);
        for (Method method : multiMap.keySet()) {

            String url = proxyStrategy.getRemoteURL(this.inter, method);

            List<NameValuePairX> pairList = new ArrayList<>();
            for (Object[] args : multiMap.get(method)) {
                String paramsStr = objectMapper.writeValueAsString(args);
                NameValuePairX nameValuePairX = new NameValuePairX(name: "params", value: paramsStr);
                pairList.add(nameValuePairX);
            }

            Map<String, String> headers = serviceContextToMap(serviceContext);
            HttpMessageWithHeader response = httpClientUtil.postWithHeader(url, pairList, headers);

            Object result = proxyStrategy.getResult(method, response);

            Date end = new Date();
            long millis = end.getTime() - start.getTime();
            String slow = "";
            if (millis > 500) {
                slow = "${lineSeparator}SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!${lineSeparator}";
                logger.warn("${inter}.${method?.getName()} result($slow$millis ms$slow): ${lineSeparator}${ObjectMapperFactory.createObjectMapper().writeValueAsString(result)}");
            } else {
                logger.debug("${inter}.${method?.getName()} result($slow$millis ms$slow): ${lineSeparator}${ObjectMapperFactory.createObjectMapper().writeValueAsString(result)}");
            }

            System.out.println(result);
        }
    }

    private Map<String, String> serviceContextToMap(ServiceContext serviceContext) throws Exception {
        Map<String, String> context = new HashMap<>();
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
        if (serviceContext != null && serviceContext.getCallId() != null) {
            context.put(ServiceContext.CALLID.toString(), objectMapper.writeValueAsString(serviceContext.getCallId()));
        }
        return context;
    }

}