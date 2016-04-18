package org.nofdev.http.oauth2

/**
 * Created by HouDongQiang on 2016/3/17.
 */
class OAuthDTO {


    private String clientId

    private String clientSecret
    private String grantType
    /**
     * protected resource url
     */
    private String resourceServerUrl
    /**
     * OAuth server provider
     */
    private String authenticationServerUrl
    /**
     * option 可选
     */
    private String scope

    String getClientId() {
        return clientId
    }

    void setClientId(String clientId) {
        this.clientId = clientId
    }

    String getClientSecret() {
        return clientSecret
    }

    void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret
    }

    String getGrantType() {
        return grantType
    }

    void setGrantType(String grantType) {
        this.grantType = grantType
    }

    String getResourceServerUrl() {
        return resourceServerUrl
    }

    void setResourceServerUrl(String resourceServerUrl) {
        this.resourceServerUrl = resourceServerUrl
    }

    String getAuthenticationServerUrl() {
        return authenticationServerUrl
    }

    void setAuthenticationServerUrl(String authenticationServerUrl) {
        this.authenticationServerUrl = authenticationServerUrl
    }

    String getScope() {
        return scope
    }

    void setScope(String scope) {
        this.scope = scope
    }
}
