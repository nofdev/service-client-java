package org.nofdev.http
/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
class Test {
    private static Boolean isSSL(String baseUrl) {
        if (baseUrl?.startsWith('https://')) {
            true
        } else {
            false
        }
    }

//    public static void main(String[] args) {
//        PoolingConnectionManagerFactory httpPoolingConnectionManagerFactory = new PoolingConnectionManagerFactory();
//        PoolingConnectionManagerFactory httpsPoolingConnectionManagerFactory = new PoolingConnectionManagerFactory();
//        DefaultRequestConfig defaultRequestConfig = new DefaultRequestConfig();
//        String baseUrl = "http://demo-test-bk.yintai.com:1234";
////        String baseUrl = "http://demo-test.yintai.com";
//
//        OAuthDTO oAuthDTO=new OAuthDTO();
//        oAuthDTO.clientId="test"
//        oAuthDTO.clientSecret="test"
//        oAuthDTO.grantType="client_credentials"
//        oAuthDTO.authenticationServerUrl="http://gw-test-bk.yintai.com:3000/oauth/token"
////        oAuthDTO.authenticationServerUrl="http://gw-test.yintai.com/oauth/token"
//
//
//        HelloFacade hello=new OAuthJsonProxy(HelloFacade.class,
//                new OAuthProxyStrategyImpl(baseUrl,oAuthDTO),
//                isSSL(baseUrl) ? httpsPoolingConnectionManagerFactory : httpPoolingConnectionManagerFactory,
//                defaultRequestConfig).getObject() as HelloFacade;
//
//        hello.hello("123")
//    }
}

