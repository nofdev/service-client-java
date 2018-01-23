package org.nofdev.client.http.oauth2

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.oltu.oauth2.client.OAuthClient
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse
import org.apache.oltu.oauth2.common.OAuth
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.message.types.GrantType
import org.nofdev.client.http.DefaultProxyStrategyImpl
import org.nofdev.client.http.ObjectMapperFactory
import org.nofdev.client.http.ProxyStrategy
import org.nofdev.exception.AuthenticationException
import org.nofdev.exception.AuthorizationException
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.http.*
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.nofdev.servicefacade.UnhandledException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Liutengfei on 2017/10/31
 */
class OAuthHttpCaller implements Caller {
    private static final Logger log = LoggerFactory.getLogger(OAuthHttpCaller.class)

    private ProxyStrategy proxyStrategy
    private PoolingConnectionManagerFactory poolingConnectionManagerFactory
    private DefaultRequestConfig defaultRequestConfig
    private OAuthConfig oAuthConfig


    OAuthHttpCaller(OAuthConfig oAuthConfig, ProxyStrategy proxyStrategy, DefaultRequestConfig defaultRequestConfig, PoolingConnectionManagerFactory poolingConnectionManagerFactory) {
        this.proxyStrategy = proxyStrategy
        this.oAuthConfig = oAuthConfig

        if (poolingConnectionManagerFactory == null) {
            try {
                this.poolingConnectionManagerFactory = new PoolingConnectionManagerFactory()
            } catch (Exception e) {
                e.printStackTrace()
            }
        } else {
            this.poolingConnectionManagerFactory = poolingConnectionManagerFactory
        }
        if (defaultRequestConfig == null) {
            this.defaultRequestConfig = new DefaultRequestConfig()
        } else {
            this.defaultRequestConfig = defaultRequestConfig
        }
    }

    /**
     * 不支持不被信任证书的 ssl 访问, 请使用支持传入new PoolingConnectionManagerFactory(true)的构造函数
     * @param inter
     * @param oAuthConfig
     * @param proxyStrategy
     */
    OAuthHttpCaller(OAuthConfig oAuthConfig, ProxyStrategy proxyStrategy) {
        this(oAuthConfig, proxyStrategy, null, null)
    }

    /**
     * 不支持不被信任证书的 ssl 访问, 请使用支持传入new PoolingConnectionManagerFactory(true)的构造函数
     * @param inter
     * @param oAuthConfig
     * @param url
     */
    OAuthHttpCaller(OAuthConfig oAuthConfig, String url) {
        this(oAuthConfig, new DefaultProxyStrategyImpl(url))
    }

    TokenContext getAccessToken() {
        log.info("Getting access token.")
        if (TokenContext.instance.isExpire()) {
            return getNewAccessToken()
        } else {
            // 什么也不做
        }
        log.debug("The access token is ${TokenContext.instance.access_token}, and expires in ${TokenContext.instance.expires_in}")
        return TokenContext.instance
    }

    private TokenContext getNewAccessToken() throws Exception {
        //todo 默认为client_credentials方式 待改造成4种都支持的
        if (GrantType.CLIENT_CREDENTIALS.toString().equals(oAuthConfig.grantType)) {
            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation(oAuthConfig.authenticationServerUrl)
                    .setGrantType(GrantType.CLIENT_CREDENTIALS)
                    .setClientId(oAuthConfig.getClientId())
                    .setClientSecret(oAuthConfig.getClientSecret())
                    .buildBodyMessage()

            long timeNow = new Date().getTime()
            //TODO 这里每次都 new 一个不合适
            OAuthClient oAuthClient = new OAuthClient(new CustomURLConnectionClient(poolingConnectionManagerFactory, defaultRequestConfig))
            try {
                OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, OAuthJSONAccessTokenResponse.class)
                TokenContext.instance.access_token = oAuthResponse.getAccessToken()
                TokenContext.instance.expires_in = oAuthResponse.getExpiresIn()
                TokenContext.instance.startTime = timeNow
                TokenContext.instance.stopTime = timeNow + oAuthResponse.getExpiresIn() * 1000
            } catch (OAuthProblemException e) {
                log.error(e.message, e)
                //发送请求成功了但是token验证错误
                throw new AuthenticationException("token认证失败", e)
            } catch (Exception e) {
                log.error(e.message, e)
                //请求没有发送成功（400和401以外的异常）
                throw new UnhandledException("token认证服务器系统异常", e)
            }
        } else {
            log.error("现在只支持 ${GrantType.CLIENT_CREDENTIALS.toString()} 方式")
            throw new AuthenticationException("grant_type not support!")
        }
        return TokenContext.instance
    }

    private CustomOAuthResourceResponse resource(String token, String url, Map<String, String> params, Map<String, String> headers) {
        //init headers
        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(url).setAccessToken(token).buildHeaderMessage()
        // add headers
        headers.each {
            bearerClientRequest.addHeader(it.key, it.value)
        }
        // add body
//        bearerClientRequest.setBody(new ObjectMapper().writeValueAsString(params))
        bearerClientRequest.setBody(this.paramsToQueryString(params))

        // post request
        CustomURLConnectionClient customURLConnectionClient = new CustomURLConnectionClient(poolingConnectionManagerFactory, defaultRequestConfig)
        OAuthClient oAuthClient = new OAuthClient(customURLConnectionClient)
        return oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.POST, CustomOAuthResourceResponse.class)
    }


    private Map<String, String> serviceContextToMap(ServiceContext serviceContext) {
        Map<String, String> context = new HashMap<>()
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

    private String paramsToQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder()
        for (def entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&')
            }
            sb.append(URLEncoder.encode(entry.key, 'UTF-8'))
            if (entry.value) {
                sb.append('=')
                sb.append(URLEncoder.encode(entry.value, 'UTF-8'))
            }
        }
        return sb.toString()
    }

    @Override
    Object call(Request request) {
        if ("hashCode" == request.method.getName()) {return request.clazz.hashCode()}
        if ("toString" == request.method.getName()) {return request.clazz.toString()}

        String remoteURL = proxyStrategy.getRemoteURL(request.clazz, request.method)
        Map<String, String> params = proxyStrategy.getParams(request.args)
        ServiceContext serviceContext = ServiceContextHolder.getServiceContext()
        Map<String, String> context = serviceContextToMap(serviceContext)

        log.debug("RPC call: ${remoteURL} ${ObjectMapperFactory.createObjectMapper().writeValueAsString(params)}")

        CustomOAuthResourceResponse resourceResponse = this.resource(getAccessToken().access_token, remoteURL, params, context)
        if (resourceResponse.getResponseCode() == 400 || resourceResponse.getResponseCode() == 401) {
            if (resourceResponse.getResponseCode() == 401) {
                log.debug("对${remoteURL}的访问未取得授权,有可能token已经过期，尝试重新获取token")
                //重试一次
                resourceResponse = this.resource(getNewAccessToken().access_token, remoteURL, params, context)
                if (resourceResponse.getResponseCode() == 400 || resourceResponse.getResponseCode() == 401) {
                    log.error("对${remoteURL}的访问未取得授权")
                    throw new AuthorizationException("对${remoteURL}的访问未取得授权")
                }
            } else {
                log.error("对${remoteURL}的访问未取得授权")
                throw new AuthorizationException("对${remoteURL}的访问未取得授权")
            }
        }

        HttpMessageWithHeader response = new HttpMessageWithHeader(resourceResponse.getResponseCode(), resourceResponse.getContentType(), resourceResponse.getBody(), resourceResponse.getHeaders())
        def result = proxyStrategy.getResult(request.method, response)
        result
    }
}
