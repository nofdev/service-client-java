package org.nofdev.http;

import groovy.transform.CompileStatic;
import org.nofdev.servicefacade.AbstractBusinessException;
import org.nofdev.servicefacade.ExceptionMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by liuyang on 2014/6/11.
 */
@CompileStatic
public class ExceptionUtil {

    public static Boolean isExistClass(String name) {
        Boolean flag = false;
        try {
            Class<?> cl = Class.forName(name);
            flag = true;
        } catch (Exception e) {
        }
        return flag;
    }

    public static Throwable getThrowableInstance(String name, String msg) {
        try {
            Class<?> cl = Class.forName(name);
            Class[] params = {String.class};
            Constructor constructor = cl.getConstructor(params);
            return (Throwable) constructor.newInstance(new Object[]{msg});
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    public static Throwable getThrowableInstance(ExceptionMessage exceptionMessage) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> cl = Class.forName(exceptionMessage.getName());
        Class[] params = {String.class};
        Constructor constructor = cl.getConstructor(params);
        Throwable throwable = (Throwable) constructor.newInstance(new Object[]{exceptionMessage.getMsg()});
        if (throwable instanceof AbstractBusinessException) {
            ((AbstractBusinessException) throwable).setDatail(exceptionMessage.getDatail());
        }
        return throwable;
    }
}
