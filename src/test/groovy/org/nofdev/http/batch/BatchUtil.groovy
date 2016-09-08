package org.nofdev.http.batch

import org.nofdev.http.DefaultRequestConfig
import org.nofdev.http.PoolingConnectionManagerFactory

/**
 * Created by Liutengfei on 2016/9/8 0008.
 */
class BatchUtil {
    static BatchJsonProxy load(Class clazz, String serverUrl) {
        return new BatchJsonProxy(clazz, serverUrl);
    }

    static BatchJsonProxy load(Class<?> inter, String url, PoolingConnectionManagerFactory connectionManagerFactory, DefaultRequestConfig defaultRequestConfig) {
        return new BatchJsonProxy(inter, url, connectionManagerFactory, defaultRequestConfig);
    }
}
