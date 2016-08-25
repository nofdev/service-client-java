package org.nofdev.http.oauth2
import groovy.json.JsonBuilder
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.nofdev.http.DefaultProxyStrategyImpl
import org.nofdev.http.PoolingConnectionManagerFactory
import org.nofdev.servicefacade.UnhandledException
import spock.lang.Ignore
import spock.lang.Specification
/**
 * Created by Liutengfei on 2016/4/25.
 */
class OauthJsonProxySpec extends Specification {
    private ClientAndServer resourceServer
    private def resourceUrl
    private def secureResourceUrl

    private ClientAndServer tokenServer
    private def tokenServerUrl
    private def secureTokenServerUrl

    def setup() {
        println "---------------setup---------------------"
        TokenContext.instance.stopTime = 0 //保证每次测试重新获取 token TODO 要从内存中销毁 TokenContext 才合适

        //resource server
        resourceServer = ClientAndServer.startClientAndServer(2016,8444)
        resourceUrl = "http://localhost:2016"
        secureResourceUrl = "https://localhost:8444"

        //token server
        tokenServer = ClientAndServer.startClientAndServer(9527,8443)
        tokenServerUrl = "http://localhost:9527/oauth/token"
        secureTokenServerUrl = "https://localhost:8443/oauth/token"
    }

    def cleanup() {
        println "---------------cleanup---------------------"
        TokenContext.instance.stopTime = 0 //保证每次测试重新获取 token TODO 要从内存中销毁 TokenContext 才合适

        tokenServer.stop()
        resourceServer.stop()
    }

    @Ignore
    def "测试入参和返回值"() {
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
        OAuthConfig oAuthConfig = new OAuthConfig();
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "${tokenServerUrl}"

        def proxy = new OAuthJsonProxy(
                DemoFacade.class,
                oAuthConfig,
                resourceUrl
        )
        def testFacadeService = proxy.getObject()
        def returnResult = testFacadeService."${method}"(*args);
        expect:
        returnResult == exp
        where:
        method              | args                                | token       | expires_in | tokenExp    | val                                 | exp
        "method1"           | []                                  | '111111111' | 3600       | '111111111' | "hello world"                       | "hello world"
        "sayHello"          | []                                  | '222222222' | 3600       | '222222222' | null                                | null
        "getAllAttendUsers" | [new UserDTO(name: "tom", age: 10)] | '333333333' | 3600       | '222222222' | [new UserDTO(name: "tom", age: 10)] | [new UserDTO(name: "tom", age: 10)]
    }

    @Ignore
    def "bugfix: 测试代理 https 请求, 对于不受信证书的 ssl 访问, 请使用复杂构造函数"() {
        setup:
        tokenServer.when(
                HttpRequest.request()
                        .withURL("${secureTokenServerUrl}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([access_token: token, token_type: "bearer", expires_in: expires_in]).toString())
        )
        resourceServer.when(
                HttpRequest.request().withURL("${secureResourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/${method}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
        )
        OAuthConfig oAuthConfig = new OAuthConfig();
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "${secureTokenServerUrl}"

        def proxy = new OAuthJsonProxy(
                DemoFacade.class,
                oAuthConfig,
                new DefaultProxyStrategyImpl(secureResourceUrl),
                new PoolingConnectionManagerFactory(true),
                null
        )
        def testFacadeService = proxy.getObject()
        def returnResult = testFacadeService."${method}"(*args);
        expect:
        returnResult == exp
        where:
        method              | args                                | token       | expires_in | tokenExp    | val                                 | exp
        "method1"           | []                                  | '111111111' | 3600       | '111111111' | "hello world"                       | "hello world"
    }

    @Ignore
    def "token过期时自动获取新token"() {
        setup:
        tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([access_token: '111111111', token_type: "bearer", expires_in: 1]).toString())
        )

        resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/method1")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
        )
        OAuthConfig oAuthConfig = new OAuthConfig()
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "${tokenServerUrl}"

        def proxy = new OAuthJsonProxy(
                DemoFacade.class,
                oAuthConfig,
                resourceUrl
        )
        def testFacadeService = proxy.getObject()
        testFacadeService.method1();
        def tokenResult1 = TokenContext.instance.getAccess_token()
        sleep(3000)
        tokenServer.reset()
        tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([access_token: '2222222222', token_type: "bearer", expires_in: 3600]).toString())
        )
        testFacadeService.method1();
        def tokenResult2 = TokenContext.instance.getAccess_token()
        expect:
        tokenResult1 == '111111111'
        tokenResult2 == '2222222222'
    }

    @Ignore
    def "token不过期的时候还是使用之前的token"() {
        setup:
        tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([access_token: '111111111', token_type: "bearer", expires_in: 3600]).toString())
        )
        resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/method1")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
        )
        OAuthConfig oAuthConfig = new OAuthConfig();
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "${tokenServerUrl}"

        def proxy = new OAuthJsonProxy(
                DemoFacade.class,
                oAuthConfig,
                resourceUrl
        )
        def testFacadeService = proxy.getObject()
        testFacadeService.method1();
        def tokenResult1 = TokenContext.instance.getAccess_token()
        sleep(1)
        tokenServer.reset()
        tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([access_token: '22222222', token_type: "bearer", expires_in: 3600]).toString())
        )
        testFacadeService.method1();
        def tokenResult2 = TokenContext.instance.getAccess_token()
        expect:
        tokenResult1 == '111111111'
        tokenResult2 == '111111111'

    }

    @Ignore
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
        OAuthConfig oAuthConfig = new OAuthConfig();
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "http://localhost:9527/oauth/token"

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

    @Ignore
    def "当token服务器宕机的时候"() {
        setup:
        resourceServer.when(HttpRequest.request().withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/sayHello")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
        )
        OAuthConfig oAuthConfig = new OAuthConfig();
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "http://localhost:1234/oauth/token"

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

    @Ignore
    def "未完成测试代码_当访问资源时发现token已经过期就重新获取token"() {
        setup:
        int markToken=0
        tokenServer.when(HttpRequest.request().withURL("${tokenServerUrl}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([access_token: ++markToken, token_type: "bearer", expires_in: 3600]).toString())
        )
        resourceServer.when(
                HttpRequest.request()
                        .withHeader(Header.header("Authorization", "Bearer 1"))
                        .withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/method1")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(401)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
        )
        resourceServer.when(
                HttpRequest.request()
                        .withHeader(Header.header("Authorization", "Bearer 2"))
                        .withURL("${resourceUrl}/facade/json/org.nofdev.http.oauth2/Demo/method1")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: 'hello world', err: null]).toString())
        )
        OAuthConfig oAuthConfig = new OAuthConfig()
        oAuthConfig.clientId = "test"
        oAuthConfig.clientSecret = "test"
        oAuthConfig.grantType = "client_credentials"
        oAuthConfig.authenticationServerUrl = "${tokenServerUrl}"

        def proxy = new OAuthJsonProxy(
                DemoFacade.class,
                oAuthConfig,
                resourceUrl
        )
        def testFacadeService = proxy.getObject()
        testFacadeService.method1();
        def tokenResult1 = TokenContext.instance.getAccess_token()

        expect:
        tokenResult1 == '2'
    }
}

