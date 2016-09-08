package org.nofdev.http.batch

/**
 * Created by Liutengfei on 2016/8/29 0029.
 */
class BatchResult<T> {
    Map<String, T> val;
    Map<String, Throwable> err;
}
