package org.nofdev.http

import groovy.json.JsonBuilder
import org.joda.time.DateTime
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.nofdev.servicefacade.ExceptionMessage
import spock.lang.Specification

/**
 * Created by Qiang on 7/10/14.
 */
class HttpJsonProxySpec extends Specification {

    private ClientAndServer mockServer
    private def url

    def setupSpec() {
    }

    def setup() {
        mockServer = ClientAndServer.startClientAndServer(9999)
        url = "http://localhost:9999"
    }

    def cleanup() {
        mockServer.stop()
    }

    def "测试能否正常的代理一个远程接口"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/DemoFacade/${method}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
        )
        def proxy = new HttpJsonProxy(DemoFacade, url)
        def testFacadeService = proxy.getObject()
        def result = testFacadeService."${method}"(*args);

        expect:
        result == exp

        where:
        method              | args                                     | val                                      | exp
        "method1"           | []                                       | "hello world"                            | "hello world"
        "getAllAttendUsers" | [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)] | [new UserDTO(name: "zhangsan", age: 10)]
    }

    def "测试能否正常的代理一个远程接口抛出的异常"() {
        setup:
        def exceptionMessage = new ExceptionMessage(name: "org.nofdev.http.TestException", msg: "Test")
        mockServer.when(
                HttpRequest.request()
                        .withURL("${url}/facade/json/org.nofdev.http/DemoFacade/method1")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(500)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: null, err: exceptionMessage]).toString())
        )
        def proxy = new HttpJsonProxy(DemoFacade, url)
        def testFacadeService = proxy.getObject()

        when:
        testFacadeService.method1()

        then:
        thrown(TestException)
    }

//    def "测试JodaTime的序列化与反序列化"(){
//        setup:
//        mockServer.when(
//                HttpRequest.request()
//                        .withURL("${url}/method1")
//        ).respond(
//                HttpResponse.response()
//                        .withStatusCode(200)
//                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
//        )
//        def proxy = new HttpJsonProxy(DemoFacade, url)
//        def testFacadeService = proxy.getObject()
//	}

    def "测试代理策略接口"() {
        setup:
        url = "http://localhost:9999/facade/json/org.nofdev.http/DemoFacade"
        mockServer.when(
                HttpRequest.request().withURL("${url}/${method}")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: val, err: null]).toString())
        )
        def baseUrl = "http://localhost:9999"
        def proxy = new HttpJsonProxy(DemoFacade, new DefaultProxyStrategyImpl(baseUrl))
        def testFacadeService = proxy.getObject()
        def result = testFacadeService."${method}"(*args);
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
                        .withURL("${url}/facade/json/org.nofdev.http/DemoFacade/sayHello")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(new JsonBuilder([callId: UUID.randomUUID().toString(), val: null, err: null]).toString())
        )
        def proxy = new HttpJsonProxy(DemoFacade, url)
        def testFacadeService = proxy.getObject()
        def result = testFacadeService.sayHello()

        expect:
        result == null
    }
}

class UserDTO implements Serializable {
    /**
     * 姓名
     */
    private String name;
    /**
     * 年龄
     */
    private Integer age;

    private DateTime birthday;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    DateTime getBirthday() {
        return birthday
    }

    void setBirthday(DateTime birthday) {
        this.birthday = birthday
    }

    @Override
    public boolean equals(Object obj) {
        return (this.name == obj.name && this.age == obj.age && this.birthday.equals(this.birthday));
    }

}

interface DemoFacade {
    String method1();

    void sayHello();

    List<UserDTO> getAllAttendUsers(UserDTO userDTO);
}

class TestException extends RuntimeException {
    TestException(String msg) {
        super(msg);
    }
}


