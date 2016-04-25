package org.nofdev.http.oauth2
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.apache.oltu.oauth2.client.OAuthClient
import org.apache.oltu.oauth2.client.URLConnectionClient
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse
import org.apache.oltu.oauth2.common.OAuth
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.message.types.GrantType
import org.nofdev.http.*
import org.nofdev.servicefacade.ServiceContext
import org.nofdev.servicefacade.ServiceContextHolder
import org.nofdev.servicefacade.UnhandledException
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
    private ProxyStrategy proxyStrategy
    private PoolingConnectionManagerFactory poolingConnectionManagerFactory
    private DefaultRequestConfig defaultRequestConfig
    private OAuthConfig oAuthConfig

    public OAuthJsonProxy(Class<?> inter, OAuthConfig oAuthConfig, ProxyStrategy proxyStrategy, PoolingConnectionManagerFactory poolingConnectionManagerFactory, DefaultRequestConfig defaultRequestConfig) {
        this.inter = inter
        this.proxyStrategy = proxyStrategy
        this.oAuthConfig = oAuthConfig

        if (poolingConnectionManagerFactory == null) {
            try {
                this.poolingConnectionManagerFactory = new PoolingConnectionManagerFactory()
            } catch (Exception e) {
                e.printStackTrace()
            }
        } else {
            this.poolingConnectionManagerFactory = poolingConnectionManagerFactory;
        }
        if (defaultRequestConfig == null) {
            this.defaultRequestConfig = new DefaultRequestConfig()
        } else {
            this.defaultRequestConfig = defaultRequestConfig
        }
    }

    public OAuthJsonProxy(Class<?> inter, OAuthConfig oAuthConfig, ProxyStrategy proxyStrategy) {
        this(inter, oAuthConfig, proxyStrategy, null, null)
    }

    public OAuthJsonProxy(Class<?> inter, OAuthConfig oAuthConfig, String url) {
        this(inter, oAuthConfig, new DefaultProxyStrategyImpl(url))
    }

    public TokenContext getAccessToken() throws Exception {
        if (TokenContext.instance.isExpire()) {
            //todo 默认为client_credentials方式 待改造成4种都支持的
            if (GrantType.CLIENT_CREDENTIALS.toString().equals(oAuthConfig.grantType)) {
                OAuthClientRequest request = OAuthClientRequest
                        .tokenLocation(oAuthConfig.authenticationServerUrl)
                        .setGrantType(GrantType.CLIENT_CREDENTIALS)
                        .setClientId(oAuthConfig.getClientId())
                        .setClientSecret(oAuthConfig.getClientSecret())
                        .buildBodyMessage();

                long timeNow = new Date().getTime();
                OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient())
                try {
                    OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, OAuthJSONAccessTokenResponse.class)
                    TokenContext.instance.access_token = oAuthResponse.getAccessToken()
                    TokenContext.instance.expires_in = oAuthResponse.getExpiresIn()
                    TokenContext.instance.startTime = timeNow
                    TokenContext.instance.stopTime = timeNow + oAuthResponse.getExpiresIn() * 1000
                } catch (Exception e) {
                    if (e instanceof OAuthProblemException) {//发送请求成功了但是token验证错误
                        throw new AuthenticationException("token认证失败", e);
                    } else {//请求没有发送成功（400和401以外的异常）
                        throw new UnhandledException("token认证服务器系统异常", e);
                    }
                }
            } else {
                logger.error("现在只支持" + GrantType.CLIENT_CREDENTIALS.toString() + "方式")
                throw new AuthenticationException("grant_type not support!")
            }
        }
        return TokenContext.instance
    }

    private HttpMessageWithHeader resource(String url, Map<String, String> params, Map<String, String> headers) throws Exception {
        //init headers
        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(url).setAccessToken(getAccessToken().access_token).buildHeaderMessage();
        // add headers
        headers.each {
            bearerClientRequest.addHeader(it.key, it.value)
        }
        // add body
        bearerClientRequest.setBody(new ObjectMapper().writeValueAsString(params))
        // post request
        CustomURLConnectionClient customURLConnectionClient = new CustomURLConnectionClient(poolingConnectionManagerFactory, defaultRequestConfig)
        OAuthClient oAuthClient = new OAuthClient(customURLConnectionClient);
        CustomOAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.POST, CustomOAuthResourceResponse.class);
        if (resourceResponse.getResponseCode() == 400 || resourceResponse.getResponseCode() == 401) {
            throw new AuthorizationException("未授权");
        }
        return new HttpMessageWithHeader(resourceResponse.getResponseCode(), resourceResponse.getContentType(), resourceResponse.getBody(), resourceResponse.getHeaders());
    }

    @Override
    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        def serviceContext = ServiceContextHolder.getServiceContext()

        if ("hashCode".equals(method.getName())) {
            return inter.hashCode();
        }
        if ("toString".equals(method.getName())) {
            return inter.toString();
        }

        String remoteURL = proxyStrategy.getRemoteURL(inter, method);
        Map<String, String> params = proxyStrategy.getParams(args)
        Map<String, String> context = serviceContextToMap(serviceContext)

        logger.info("RPC call: ${remoteURL} ${ObjectMapperFactory.createObjectMapper().writeValueAsString(params)}");

        HttpMessageWithHeader response = this.resource(remoteURL, params, context)

        return proxyStrategy.getResult(method, response)
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
