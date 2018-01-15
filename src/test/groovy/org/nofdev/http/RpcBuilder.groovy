package org.nofdev.http

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.nofdev.client.RpcClient
import org.nofdev.client.http.HttpCaller
import org.nofdev.client.http.oauth2.OAuthConfig
import org.nofdev.client.http.oauth2.OAuthHttpCaller
import org.nofdev.core.Caller
import org.nofdev.core.RpcFilter

/**
 * Created by liutengfei on 12/01/18.
 */
@CompileStatic
@Slf4j
class RpcBuilder {


    static <T> T httpJson(Class<T> inter, String url) {
        return build(inter, new HttpCaller(url), null)
    }

    static <T> T httpJson(Class<T> inter, HttpCaller caller) {
        return build(inter, caller, null)
    }

    static <T> T httpJsonOAuth(Class<T> inter, String url, OAuthConfig oAuthConfig) {
        return build(inter, new OAuthHttpCaller(oAuthConfig, url), null)
    }

    static <T> T build(Class<T> inter, Caller caller) {
        return build(inter, caller, null)
    }

    static <T> T build(Class<T> inter, Caller caller, List<RpcFilter> _filters) {
        return new RpcClient<T>(inter, caller, _filters).getObject()
    }
}
