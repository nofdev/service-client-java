package org.nofdev.http.oauth2

import org.apache.oltu.oauth2.client.response.OAuthClientResponse
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
class OAuthResourceResponse2 extends OAuthClientResponse{
    Map<String,String> headers;


    public String getBody() {
        return body;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getContentType(){
        return contentType;
    }

    @Override
    protected void setBody(String body) throws OAuthProblemException {
        this.body = body;
    }

    @Override
    protected void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    protected void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

}
