package org.nofdev.http.oauth2

import groovy.transform.CompileStatic
import org.apache.oltu.oauth2.client.OAuthClient
import org.apache.oltu.oauth2.client.URLConnectionClient
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse
import org.apache.oltu.oauth2.common.OAuth
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.message.types.GrantType
import org.nofdev.http.DefaultProxyStrategyImpl
import org.nofdev.http.HttpMessageWithHeader
import org.nofdev.http.ObjectMapperFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by HouDongQiang on 2016/3/16.
 * A StrategyImpl class for OAuth2 oauth2. OAuth2client的 增强实现
 */
@CompileStatic
class OAuthProxyStrategyImpl extends DefaultProxyStrategyImpl {

    private static final Logger logger = LoggerFactory.getLogger(OAuthProxyStrategyImpl.class)

    private String baseURL
    private OAuthDTO oAuthDTO

    public OAuthProxyStrategyImpl(String baseURL, OAuthDTO oAuthDTO) {
        super(baseURL)
        this.baseURL = baseURL
        this.oAuthDTO = oAuthDTO
    }

    /**
     * get accessToken. 获取一个accessToken
     * @return accessToken
     * @throws OAuthProblemException
     */

    /*  ---------------------
        client_credentials
        ---------------------
        post body
        client_id       d712fa37-e965-4d37-9714-a87af8e6991b
        client_secret   52f0a3bb-6411-4312-97fa-e2cf692e2c09
        grant_type      client_credentials
        url:      http://10.32.150.121:3000/oauth/token
    */

    public TokenContext getAccessToken() throws OAuthProblemException {
        if (TokenContext.instance.isExpire()) {
            //todo 默认为client_credentials方式 待改造成4种都支持的
            if (GrantType.CLIENT_CREDENTIALS.toString().equals(oAuthDTO.grantType)) {
                OAuthClientRequest request = OAuthClientRequest
                        .tokenLocation(oAuthDTO.authenticationServerUrl)
                        .setGrantType(GrantType.CLIENT_CREDENTIALS)
                        .setClientId(oAuthDTO.getClientId())
                        .setClientSecret(oAuthDTO.getClientSecret())
                        .buildBodyMessage();

                long timeNow = new Date().getTime();
                OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient())
                OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, OAuthJSONAccessTokenResponse.class)
                TokenContext.instance.access_token = oAuthResponse.getAccessToken()
                TokenContext.instance.expires_in = oAuthResponse.getExpiresIn()
                TokenContext.instance.startTime = timeNow
                TokenContext.instance.stopTime = timeNow + oAuthResponse.getExpiresIn() * 1000
            } else {
                logger.error("现在支持" + GrantType.CLIENT_CREDENTIALS.toString() + "方式")
                throw new AuthorizationException("grant_type not support!")
            }
        }
        return TokenContext.instance
    }
    /*resource OAuthURLConnection* @return
     */

    public HttpMessageWithHeader resource(OAuthURLConnectionClient httpClient, String url, Map<String, String> params, Map<String, String> headers) {
        println("获取到的token是:" + ObjectMapperFactory.createObjectMapper().writeValueAsString(getAccessToken()))
        //init headers
        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(url).setAccessToken(getAccessToken().access_token).buildBodyMessage();
        // add headers
        for (Map.Entry<String, String> entry : headers?.entrySet()) {
            bearerClientRequest.addHeader(entry.key,entry.value);
        }
        httpClient.params = params;
        OAuthClient oAuthClient = new OAuthClient(httpClient);
        OAuthResourceResponse2 resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.POST, OAuthResourceResponse2.class );
        if (resourceResponse.getResponseCode() == 400 || resourceResponse.getResponseCode() == 401) {
            throw new AuthorizationException("未授权");
        }
        return new HttpMessageWithHeader(resourceResponse.getResponseCode(),resourceResponse.getContentType(),resourceResponse.getBody(),resourceResponse.getHeaders());
    }

    /**
     * AcessToen isexpire  是否过期
     * TokenContext.instance.isExpire()
     */
    boolean isExpire() {
        return TokenContext.instance.isExpire();
    }

    /**
     *  refresh accessToken 刷新token
     * @return
     */
    public TokenContext refreshAccessToken() {
        //todo 暂时重新获取token
        getAccessToken()
    }


}