package org.nofdev.http.batch
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.mockserver.integration.ClientAndServer
import spock.lang.Specification

import java.util.function.Consumer
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
//        mockServer = ClientAndServer.startClientAndServer(9999, 8443)
        serverUrl = "http://localhost:9999"
        secureUrl = "https://localhost:8443"
    }

    def cleanup() {
        mockServer.stop()
    }

    def "测试代理有参数的方法"() {
        setup:
        new BatchJsonProxy(DemoFacade, serverUrl).batchExec(new Consumer<DemoFacade>() {
            @Override
            void accept(DemoFacade demoFacade) {
                println "-----------------"+demoFacade
                demoFacade.getAllAttendUsers(new UserDTO(name: "tom", age: 18))
                demoFacade.getAllAttendUsers(new UserDTO(name: "jerry", age: 16))
                demoFacade.getAllAttendUsers(new UserDTO(name: "bill", age: 15))
            }
        })
    }
}

class UserDTO implements Serializable {
    String name;
    Integer age;

    @Override
    String toString() {
        return ReflectionToStringBuilder.toString(this)
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


