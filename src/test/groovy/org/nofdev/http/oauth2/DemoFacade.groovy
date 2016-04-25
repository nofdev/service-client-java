package org.nofdev.http.oauth2

import org.joda.time.DateTime

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