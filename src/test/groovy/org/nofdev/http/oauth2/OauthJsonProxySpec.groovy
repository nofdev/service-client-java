package org.nofdev.http.oauth2
import groovy.json.JsonBuilder
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.nofdev.servicefacade.UnhandledException
import spock.lang.Specification
/**
 * Created by Liutengfei on 2016/4/25.
 */
class OauthJsonProxySpec extends Specification {
    private ClientAndServer resourceServer
    private def resourceUrl

    private ClientAndServer tokenServer
    private def tokenServerUrl

    def setupSpec() {

    }

    def setup() {
        //resource server
        resourceServer = ClientAndServer.startClientAndServer(2016)
        resourceUrl = "http://localhost:2016"

        //token server
        tokenServer=ClientAndServer.startClientAndServer(9527)
        tokenServerUrl="http://localhost:9527/oauth/token"
    }

    def cleanup() {
        tokenServer.stop()
        resourceServer.stop()
    }

    def "token不过期的时候还是使用之前的token"() {
        setup:
            tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")
            ).respond(
                    HttpResponse.response()
                            .withStatusCode(200)
                            .withBody(new JsonBuilder([access_token: token, token_type: "bearer", expires_in: expires_in]).toString())
            )

            resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/${method}")
            ).respond(
                    HttpResponse.response()
                            .withStatusCode(200)
                            .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
            )
            OAuthConfig oAuthConfig=new OAuthConfig();
            oAuthConfig.clientId="test"
            oAuthConfig.clientSecret="test"
            oAuthConfig.grantType="client_credentials"
            oAuthConfig.authenticationServerUrl="http://localhost:9527/oauth/token"

            def proxy = new OAuthJsonProxy(
                    DemoFacade.class,
                    oAuthConfig,
                    resourceUrl
            )
            def testFacadeService = proxy.getObject()
            def returnResult = testFacadeService."${method}"(*args);
            def tokenResult= TokenContext.instance.getAccess_token()
        expect:
            returnResult == exp
            tokenResult==tokenExp
        where:
            method              | args                                    | token        |expires_in |  tokenExp  | val                                      | exp
            "method1"           | []                                      | '111111111'  |3600       | '111111111'| "hello world"                            | "hello world"
            "getAllAttendUsers" | [new UserDTO(name: "zhangsan", age: 10)]| '222222222'  |3600       | '111111111'| [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)]
    }

    def "token过期时自动获取新token"() {
        setup:
            tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")
            ).respond(
                    HttpResponse.response()
                            .withStatusCode(200)
                            .withBody(new JsonBuilder([access_token: token, token_type: "bearer", expires_in: expires_in]).toString())
            )

            resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/${method}")
            ).respond(
                    HttpResponse.response()
                            .withStatusCode(200)
                            .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
            )
            OAuthConfig oAuthConfig=new OAuthConfig();
            oAuthConfig.clientId="test"
            oAuthConfig.clientSecret="test"
            oAuthConfig.grantType="client_credentials"
            oAuthConfig.authenticationServerUrl="http://localhost:9527/oauth/token"

            def proxy = new OAuthJsonProxy(
                    DemoFacade.class,
                    oAuthConfig,
                    resourceUrl
            )
            def testFacadeService = proxy.getObject()
            def returnResult = testFacadeService."${method}"(*args);
            Thread.sleep(3000)
            def tokenResult= TokenContext.instance.getAccess_token()
        expect:
            returnResult == exp
            tokenResult==tokenExp
        where:
            method              | args                                    | token        |expires_in |  tokenExp  | val                                      | exp
            "method1"           | []                                      | '111111111'  |1          | '111111111'| "hello world"                            | "hello world"
            "getAllAttendUsers" | [new UserDTO(name: "zhangsan", age: 10)]| '222222222'  |3600       | '222222222'| [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)]
    }
    def "当client_id或client_secret验证错误时"() {
        setup:
        tokenServer.when(
                HttpRequest.request()
                        .withURL("${tokenServerUrl}")
                        .withBody("grant_type=client_credentials&client_secret=test&client_id=test")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(400)
                        .withBody("{'code':400,'error':'invalid_client','error_description':'Client credentials are invalid'}")
        )

        resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/sayHello")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
        )
        OAuthConfig oAuthConfig=new OAuthConfig();
        oAuthConfig.clientId="test"
        oAuthConfig.clientSecret="test"
        oAuthConfig.grantType="client_credentials"
        oAuthConfig.authenticationServerUrl="http://localhost:9527/oauth/token"

        def proxy = new OAuthJsonProxy(
                DemoFacade.class,
                oAuthConfig,
                resourceUrl
        )
        def testFacadeService = proxy.getObject()
        when:
            testFacadeService.sayHello()
        then:
            thrown(AuthenticationException)
    }
    def "当token服务器宕机的时候"() {
        setup:
            resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/sayHello")
            ).respond(
                    HttpResponse.response()
                            .withStatusCode(200)
                            .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
            )
            OAuthConfig oAuthConfig=new OAuthConfig();
            oAuthConfig.clientId="test"
            oAuthConfig.clientSecret="test"
            oAuthConfig.grantType="client_credentials"
            oAuthConfig.authenticationServerUrl="http://localhost:9527/oauth/token"

            def proxy = new OAuthJsonProxy(
                    DemoFacade.class,
                    oAuthConfig,
                    resourceUrl
            )
            def testFacadeService = proxy.getObject()
        when:
            testFacadeService.sayHello()
        then:
            thrown(UnhandledException)
    }
}

