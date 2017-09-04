package org.nofdev.http

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.nofdev.servicefacade.HttpJsonResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

/**
 * Created by Qiang on 8/14/14.
 */
class DefaultProxyStrategyImpl implements ProxyStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyStrategyImpl.class)

    private String baseURL

    DefaultProxyStrategyImpl(String baseURL) {
        this.baseURL = baseURL
    }

    @Override
    String getRemoteURL(Class<?> inter, Method method) {
        logger.debug("The baseUrl is {}", baseURL)
        String serviceLayer
        String serviceName = inter.getSimpleName()
        if (inter.getName().endsWith("Facade")) {
            serviceLayer = "facade"
            serviceName = serviceName.substring(0, serviceName.length() - 6)
        } else if (inter.getName().endsWith("Service")) {
            serviceLayer = "service"
            serviceName = serviceName.substring(0, serviceName.length() - 7)
        } else {
            serviceLayer = "micro"
        }
        StringBuilder remoteURLBuffer = new StringBuilder()
        remoteURLBuffer.append(baseURL)
        remoteURLBuffer.append("/")
        remoteURLBuffer.append(serviceLayer)
        remoteURLBuffer.append("/json/")
        remoteURLBuffer.append(inter.getPackage().getName())
        remoteURLBuffer.append("/")
        remoteURLBuffer.append(serviceName)
        remoteURLBuffer.append("/")
        remoteURLBuffer.append(method.getName())
        String remoteURL = remoteURLBuffer.toString()
        logger.debug("The remoteUrl is {}", remoteURL)
        return remoteURL
    }

    @Override
    Map<String, String> getParams(Object[] args) throws JsonProcessingException {
        Map<String, String> params = new HashMap<>()
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()

        String paramsStr = objectMapper.writeValueAsString(args)
        logger.debug("The params string is {}", paramsStr)
        params.put("params", paramsStr)
        return params
    }

    @Override
    Object getResult(Method method, HttpMessageSimple httpMessageSimple) throws Throwable {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()

        String result = httpMessageSimple.getBody()
        logger.debug("The request return " + result)
        logger.debug("The method return type is {}", method.getGenericReturnType())
        JavaType javaType
        if (method.getGenericReturnType() != void.class) {
            logger.debug("The method return type is not void")
            javaType = objectMapper.getTypeFactory().constructType(method.getGenericReturnType())
        } else {
            logger.debug("The method return type is void")
            javaType = objectMapper.getTypeFactory().constructType(Object.class)
        }
        javaType = objectMapper.getTypeFactory().constructParametrizedType(HttpJsonResponse.class,HttpJsonResponse.class, javaType)
        HttpJsonResponse httpJsonResponse = objectMapper.readValue(result, javaType)
        if (httpJsonResponse.getErr() == null) {
            return httpJsonResponse.getVal()
        } else {
            throw ExceptionUtil.getThrowableInstance(httpJsonResponse.getErr())
        }
    }
}
