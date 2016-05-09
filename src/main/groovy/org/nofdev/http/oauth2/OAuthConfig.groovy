package org.nofdev.http.oauth2

/**
 * Created by HouDongQiang on 2016/3/17.
 */
class OAuthConfig {
    String clientId
    String clientSecret
    String grantType
    /**
     * OAuth server provider
     */
    String authenticationServerUrl
    /**
     * option 可选
     */
    String scope
}
