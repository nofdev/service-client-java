package org.nofdev.http.oauth2

import com.yintai.demo.sample.facade.HelloFacade
import spock.lang.Specification
/**
 * Created by Qiang on 4/22/16.
 */
class OauthJsonProxySpec extends Specification {
    def setupSpec() {

    }

    def setup() {

    }

    def cleanup() {

    }

    def "基本测试"(){
        setup:

        String baseUrl = "http://demo-test-bk.yintai.com:1234";

        OAuthConfig oAuthDTO=new OAuthConfig();
        oAuthDTO.clientId="test"
        oAuthDTO.clientSecret="test"
        oAuthDTO.grantType="client_credentials"
        oAuthDTO.authenticationServerUrl="http://gw-test-bk.yintai.com:3000/oauth/token"

        HelloFacade helloFacade=new OAuthJsonProxy(
                HelloFacade.class,
                oAuthDTO,
                baseUrl
        ).getObject() as HelloFacade;

        def result = helloFacade.hello("123")

        expect:
        result == "Hello 123"
    }
}
