package org.nofdev.http.oauth2
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.oltu.oauth2.client.HttpClient
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthClientResponse
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.exception.OAuthSystemException
import org.apache.oltu.oauth2.common.utils.OAuthUtils
import org.nofdev.http.DefaultRequestConfig
import org.nofdev.http.PoolingConnectionManagerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.Charset
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
class OAuthURLConnectionClient  implements HttpClient {
    private static Logger logger = LoggerFactory.getLogger(OAuthURLConnectionClient.class);

    private PoolingConnectionManagerFactory connectionManagerFactory;
    private DefaultRequestConfig defaultRequestConfig;
    private org.apache.http.client.HttpClient httpClient;
    Map<String, String> params

    public OAuthURLConnectionClient(PoolingConnectionManagerFactory connectionManagerFactory, DefaultRequestConfig defaultRequestConfig) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.connectionManagerFactory = connectionManagerFactory;
        this.defaultRequestConfig = defaultRequestConfig;
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager((PoolingHttpClientConnectionManager) connectionManagerFactory.getObject());
        this.httpClient = httpClientBuilder.build();
    }

    public OAuthURLConnectionClient(PoolingConnectionManagerFactory connectionManagerFactory) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this(connectionManagerFactory, new DefaultRequestConfig());
    }


    @Override
    def <T extends OAuthClientResponse> T execute(OAuthClientRequest request, Map<String, String> headers, String requestMethod, Class<T> responseClass) throws OAuthSystemException, OAuthProblemException {
        HttpPost post = new HttpPost(request.getLocationUri());
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(defaultRequestConfig.getDefaultConnectionRequestTimeout())
                .setConnectTimeout(defaultRequestConfig.getDefaultConnectionTimeout())
                .setSocketTimeout(defaultRequestConfig.getDefaultSoTimeout())
                .setExpectContinueEnabled(false)
                .build();
        post.setConfig(requestConfig);
        List<NameValuePair> pairList = new ArrayList<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                NameValuePair nameValuePair = new BasicNameValuePair(entry.getKey(), entry.getValue());
                pairList.add(nameValuePair);
            }
        } else {
            logger.trace("Request params do not exit");
        }
        post.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
        headers=request.getHeaders();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            post.addHeader(entry.getKey(), entry.getValue());
        }

        HttpResponse httpResponse = httpClient.execute(post);
        logger.debug("The http response status code is {}", httpResponse.getStatusLine().getStatusCode());
        HttpEntity httpEntity = httpResponse.getEntity();
        String body = EntityUtils.toString(httpEntity);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String contentType;
        if (httpEntity.getContentType() == null) {
            contentType = null;
        } else {
            contentType = httpEntity.getContentType().getValue();
        }
        logger.debug("response entity is " + body);

        Map<String,String> responseHeaders = new HashMap<>();
        for(Header header:httpResponse.getAllHeaders()){
            responseHeaders.put(header.getName(),header.getValue());
        }

        post.releaseConnection();

        OAuthClientResponse resp = (OAuthResourceResponse2)OAuthUtils.instantiateClassWithParameters(responseClass, null, null);
        resp.init(body, contentType, statusCode);

        OAuthResourceResponse2 response2=(OAuthResourceResponse2)resp;
        response2.headers=responseHeaders
        return response2;
    }

    @Override
    void shutdown() {
        // Nothing to do here
    }
}
