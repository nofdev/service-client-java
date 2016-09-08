package org.nofdev.http.batch

import groovy.json.JsonBuilder
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.nofdev.http.ObjectMapperFactory
import org.nofdev.servicefacade.ExceptionMessage
import spock.lang.Specification

/**
 * Created by LiuTengfei on 20/8/16.
 */
class BatchJsonProxySpec extends Specification {

    private ClientAndServer mockServer
    private String serverUrl
    private String secureUrl

    def setupSpec() {
    }

    def setup() {
        mockServer = ClientAndServer.startClientAndServer(9999, 8443)
        serverUrl = "http://localhost:9999"
        secureUrl = "https://localhost:8443"
    }

    def cleanup() {
        mockServer.stop()
    }

    def "有参数的方法"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${serverUrl}/batch/json/org.nofdev.http.batch/BusinessFacade/getAllAttendUsers")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(
                        new JsonBuilder(
                                [
                                        callId: UUID.randomUUID().toString(),
                                        val   : ["0": new UserDTO(name: "tom", age: 18), "3": new UserDTO(name: "jerry", age: 16)],
                                        err   : [
                                                name    : "org.nofdev.exception.BatchException",
                                                msg     : "批量接口执行异常",
                                                cause   : null,
                                                stack   : null,
                                                children:
                                                        [
                                                                "1": new ExceptionMessage(name: "java.lang.IllegalArgumentException", msg: "参数错误"),
                                                                "2": new ExceptionMessage(name: "java.lang.IllegalArgumentException", msg: "参数错误")
                                                        ]
                                        ]
                                ]
                        ).toString())
        )


        BatchResult batchResult = BatchUtil.load(BusinessFacade, serverUrl).batchExec({ BusinessFacade business ->
            business.getAllAttendUsers(new UserDTO(name: "tom"))
            business.getAllAttendUsers(new UserDTO(name: "yintai"))
            business.getAllAttendUsers(new UserDTO(name: "alibaba"))
            business.getAllAttendUsers(new UserDTO(name: "jerry"))
        })


        batchResult.val.each {
            println it.key + "------" + ObjectMapperFactory.createObjectMapper().writeValueAsString(it.value)
        }
        batchResult.err.each {
            println it.key + "------" + it.value.getMessage()
        }
    }

    def "返回值为Void的方法"() {
        setup:
        mockServer.when(
                HttpRequest.request()
                        .withURL("${serverUrl}/batch/json/org.nofdev.http.batch/BusinessFacade/sayHello")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(
                        new JsonBuilder(
                                [
                                        callId: UUID.randomUUID().toString(),
                                        val   : null,
                                        err   : [
                                                name    : "org.nofdev.exception.BatchException",
                                                msg     : "批量接口执行异常",
                                                cause   : null,
                                                stack   : null,
                                                children:
                                                        [
                                                                "1": new ExceptionMessage(name: "java.lang.NullPointerException", msg: "xx不能为空"),
                                                                "2": new ExceptionMessage(name: "java.lang.NullPointerException", msg: "yy不能为空")
                                                        ]
                                        ]
                                ]
                        ).toString())
        )


        BatchResult batchResult = BatchUtil.load(BusinessFacade, serverUrl).batchExec({ BusinessFacade business ->
            business.sayHello()
            business.sayHello()
            business.sayHello()
            business.sayHello()
        })


        batchResult.val.each {
            println it.key + "------" + ObjectMapperFactory.createObjectMapper().writeValueAsString(it.value)
        }
        batchResult.err.each {
            println it.key + "------" + it.value.getMessage()
        }
    }
}

class UserDTO implements Serializable {
    String name;
    Integer age;
}

interface BusinessFacade {
    String method1();

    void sayHello();

    List<UserDTO> getAllAttendUsers(UserDTO userDTO);
}

class TestException extends RuntimeException {
    TestException(String msg) {
        super(msg);
    }
}


