package org.nofdev.core

import java.lang.reflect.Method

/**
 * Created by Liutengfei on 2017/10/26
 */
class Request {
    Class<?> clazz
    Method method
    Object[] args
}
