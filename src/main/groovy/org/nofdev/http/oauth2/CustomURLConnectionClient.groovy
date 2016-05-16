package org.nofdev.http.oauth2

import org.apache.oltu.oauth2.client.HttpClient
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthClientResponse
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.exception.OAuthSystemException
import org.nofdev.http.DefaultRequestConfig
import org.nofdev.http.HttpClientUtil
import org.nofdev.http.HttpMessageWithHeader
import org.nofdev.http.PoolingConnectionManagerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException

/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
class CustomURLConnectionClient implements HttpClient {
    private static Logger logger = LoggerFactory.getLogger(CustomURLConnectionClient.class);

    private PoolingConnectionManagerFactory connectionManagerFactory;
    private DefaultRequestConfig defaultRequestConfig;

    public CustomURLConnectionClient(PoolingConnectionManagerFactory connectionManagerFactory, DefaultRequestConfig defaultRequestConfig) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.connectionManagerFactory = connectionManagerFactory;
        this.defaultRequestConfig = defaultRequestConfig;
    }

    public CustomURLConnectionClient(PoolingConnectionManagerFactory connectionManagerFactory) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this(connectionManagerFactory, new DefaultRequestConfig());
    }


    @Override
    def <T extends OAuthClientResponse> T execute(OAuthClientRequest request, Map<String, String> headers, String requestMethod, Class<T> responseClass) throws OAuthSystemException, OAuthProblemException {
        HttpMessageWithHeader httpMessageWithHeader = new HttpClientUtil(connectionManagerFactory, defaultRequestConfig).postWithHeader(request.locationUri, queryStringToParams(request.body), request.headers)
        if (responseClass.isAssignableFrom(OAuthJSONAccessTokenResponse.class)) {
            return OAuthClientResponseFactory.createJSONTokenResponse(httpMessageWithHeader.body, httpMessageWithHeader.contentType, httpMessageWithHeader.statusCode)
        }

        return new CustomOAuthResourceResponse(headers: httpMessageWithHeader.headers, body: httpMessageWithHeader.body, contentType: httpMessageWithHeader.contentType, responseCode: httpMessageWithHeader.statusCode)
    }

    @Override
    void shutdown() {

    }

    private Map<String, String> queryStringToParams(String queryString) {
        def params = [:]
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf("=")
            if (eq < 0) {
                // key with no value
                params.put(URLDecoder.decode(pair, "UTF-8"), "")
            } else {
                // key=value
                String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                String value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                params.put(key, value)
            }
        }
        params
    }
}
