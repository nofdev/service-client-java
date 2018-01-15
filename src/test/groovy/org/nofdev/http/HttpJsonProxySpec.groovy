package org.nofdev.http

import groovy.json.JsonBuilder
import jdk.nashorn.internal.ir.annotations.Ignore
import org.joda.time.DateTime
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.nofdev.client.http.DefaultProxyStrategyImpl
import org.nofdev.client.http.HttpCaller
import org.nofdev.servicefacade.ExceptionMessage
import org.nofdev.servicefacade.ServiceNotFoundException
import spock.lang.Specification

/**
 * Created by Qiang on 7/10/14.
 */
class HttpJsonProxySpec extends Specification {

    private ClientAndServer mockServer
    private String url
    private String secureUrl

    def setupSpec() {
    }

    def setup() {
        mockServer = ClientAndServer.startClientAndServer(9999, 8443)
        url = "http://localhost:9999"
        secureUrl = "https://localhost:8443"
    }

    def cleanup() {
        mockServer.stop()
    }

    def "测试ClientFilter"() {
        setup:
        def list=[new UserDTO(name: "tom", age: 18), new UserDTO(name: "jerry", age: 18)]

        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/Demo/getAllAttendUsers")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: [new UserDTO(name: "tom", age: 18), new UserDTO(name: "jerry", age: 18)], err: null]).toString())
        )
        DemoFacade testFacadeService = RpcBuilder.httpJson(DemoFacade, url)
        def result = testFacadeService.getAllAttendUsers(new UserDTO(age: 18))
        expect:
        result == list
    }

    def "测试RpcClient2"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/Demo/sayHello")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: null, err: null]).toString())
        )
//        DemoFacade testFacadeService=new RpcClient<DemoFacade>(DemoFacade,new HttpCaller(url)).getObject()
        DemoFacade testFacadeService = RpcBuilder.httpJson(DemoFacade, url)
        def result = testFacadeService.sayHello()

        expect:
        result == null
    }


    def "测试能否正常的代理一个远程接口"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/Demo/${method}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
        )

        def testFacadeService = RpcBuilder.httpJson(DemoFacade, new HttpCaller(url))
        def result = testFacadeService."${method}"(*args)

        expect:
        result == exp

        where:
        method              | args                                     | val                                      | exp
        "method1"           | []                                       | "hello world"                            | "hello world"
        "getAllAttendUsers" | [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)]
    }

    def "bugfix: 测试代理 https 请求, 对于不受信证书的 ssl 访问, 请使用复杂构造函数"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${secureUrl}/facade/json/org.nofdev.http/Demo/${method}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
        )

        def testFacadeService = RpcBuilder.httpJson(DemoFacade, new HttpCaller(new DefaultProxyStrategyImpl(secureUrl), null, new PoolingConnectionManagerFactory(true)))
        def result = testFacadeService."${method}"(*args)

        expect:
        result == exp

        where:
        method    | args | val           | exp
        "method1" | []   | "hello world" | "hello world"
    }

    def "测试能否正常的代理一个远程接口抛出的异常"() {
        setup:
        def exceptionMessage = new ExceptionMessage(name: "org.nofdev.http.TestException", msg: "Test")
        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/Demo/method1")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(500)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: null, err: exceptionMessage]).toString())
        )

        DemoFacade testFacadeService = RpcBuilder.httpJson(DemoFacade, new HttpCaller(url))

        when:
        testFacadeService.method1()

        then:
        thrown(TestException)
    }


    def "测试代理策略接口"() {
        setup:
        def baseUrl = "http://localhost:9999"
        url = "${baseUrl}/facade/json/org.nofdev.http/Demo"
        mockServer.when(
                HttpRequest.request().withURL("${url}/${method}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
        )
        def testFacadeService = RpcBuilder.httpJson(DemoFacade, new HttpCaller(new DefaultProxyStrategyImpl(baseUrl)))
        def result = testFacadeService."${method}"(*args)
        expect:
        result == exp

        where:
        method              | args                                     | val                                      | exp
        "method1"           | []                                       | "hello world"                            | "hello world"
        "getAllAttendUsers" | [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)]
        "sayHello"          | []                                       | null                                     | null
    }

    def "Bugfix，如果接口方法返回是void的话会报错"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/Demo/sayHello")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: null, err: null]).toString())
        )
        DemoFacade testFacadeService = RpcBuilder.httpJson(DemoFacade, new HttpCaller(url))
        def result = testFacadeService.sayHello()

        expect:
        result == null
    }

    def "测试远程服务器宕机的情况"() {
        setup:
        DemoFacade testFacadeService = RpcBuilder.httpJson(DemoFacade, new HttpCaller(url))
        testFacadeService.sayHello()
        when:
        testFacadeService.sayHello()
        then:
        thrown(ServiceNotFoundException)
    }

    @Ignore
    def "TODO 测试异常类不能被反序列化的情况"() {
//        setup:
//        when:
//        then:
//        thrown(ErrorDeserializedException)
    }
}

class UserDTO implements Serializable {
    /**
     * 姓名
     */
    private String name
    /**
     * 年龄
     */
    private Integer age

    private DateTime birthday

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    Integer getAge() {
        return age
    }

    void setAge(Integer age) {
        this.age = age
    }

    DateTime getBirthday() {
        return birthday
    }

    void setBirthday(DateTime birthday) {
        this.birthday = birthday
    }

    @Override
    boolean equals(Object obj) {
        return (this.name == obj.name && this.age == obj.age && this.birthday.equals(this.birthday))
    }

}

interface DemoFacade {
    String method1()

    void sayHello()

    List<UserDTO> getAllAttendUsers(UserDTO userDTO)
}

class TestException extends RuntimeException {
    TestException(String msg) {
        super(msg)
    }
}


