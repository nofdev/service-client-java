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

            OAuthConfig oAuthConfig=new OAuthConfig();
            oAuthConfig.clientId="test"
            oAuthConfig.clientSecret="test"
            oAuthConfig.grantType="client_credentials"
            oAuthConfig.authenticationServerUrl="http://gw-test-bk.yintai.com:3000/oauth/token"

            HelloFacade helloFacade=new OAuthJsonProxy(
                    HelloFacade.class,
                    oAuthConfig,
                    baseUrl
            ).getObject() as HelloFacade;

            def result = helloFacade.hello("world")

        expect:
            result == "Hello world"
    }
}
