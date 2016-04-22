package org.nofdev.http.oauth2

import org.apache.oltu.oauth2.client.response.OAuthResourceResponse
/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
class CustomOAuthResourceResponse extends OAuthResourceResponse{
    Map<String,String> headers
}
