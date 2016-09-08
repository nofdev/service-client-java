package org.nofdev.http.batch

import org.nofdev.servicefacade.ExceptionMessage

/**
 * Created by Liutengfei on 2016/8/29 0029.
 */
class BatchHttpJsonResponse<T> {
    HashMap<String,T> val;
    String callId;
    ExceptionMessage err;
}
