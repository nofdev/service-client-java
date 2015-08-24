package org.nofdev.servicefacade
/**
 * Created by Qiang on 7/30/14.
 */
//@JsonTypeInfo(use=JsonTypeInfo.Id.NONE, include=JsonTypeInfo.As.WRAPPER_OBJECT)
public class HttpJsonResponse<T> implements Serializable {
    private static final long serialVersionUID = 5393610697317077173L;

    T val;
    String callId;
    ExceptionMessage err;

}
