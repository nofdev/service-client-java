package org.nofdev.http.batch

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.nofdev.http.HttpMessageSimple
import org.nofdev.http.ObjectMapperFactory
import org.nofdev.http.ProxyStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

/**
 * Created by LiuTengfei on 20/8/16.
 */
public class BatchProxyStrategyImpl implements ProxyStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BatchProxyStrategyImpl.class);

    private String baseURL;

    public BatchProxyStrategyImpl(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public String getRemoteURL(Class<?> inter, Method method) {
        logger.debug("The baseUrl is {}", baseURL);
        String serviceName = inter.getSimpleName();
        StringBuilder remoteURLBuffer = new StringBuilder();
        remoteURLBuffer.append(baseURL);
        remoteURLBuffer.append("/");
        remoteURLBuffer.append("batch");
        remoteURLBuffer.append("/json/");
        remoteURLBuffer.append(inter.getPackage().getName());
        remoteURLBuffer.append("/");
        remoteURLBuffer.append(serviceName);
        remoteURLBuffer.append("/");
        remoteURLBuffer.append(method.getName());
        String remoteURL = remoteURLBuffer.toString();
        logger.debug("The remoteUrl is {}", remoteURL);
        return remoteURL;
    }

    @Override
    public Map<String, String> getParams(Object[] args) throws JsonProcessingException {
        return null
    }

    @Override
    public Object getResult(Method method, HttpMessageSimple httpMessageSimple) throws Throwable {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
        String result = httpMessageSimple.getBody();
        logger.debug("The request return " + result);
        logger.debug("The method return type is {}", method.getGenericReturnType());
        JavaType javaType;
        if (method.getGenericReturnType() != void.class) {
            logger.debug("The method return type is not void");
            javaType = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
        } else {
            logger.debug("The method return type is void");
            javaType = objectMapper.getTypeFactory().constructType(Object.class);
        }
        javaType = objectMapper.getTypeFactory().constructParametrizedType(BatchHttpJsonResponse.class, BatchHttpJsonResponse.class, javaType);
        BatchHttpJsonResponse httpJsonResponse = objectMapper.readValue(result, javaType);
        return httpJsonResponse
    }
}
