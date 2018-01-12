package org.nofdev.client.http.oauth2
/**
 * Created by Liutengfei on 2016/4/21 0021.
 */
@Singleton
class TokenContext {
    String access_token
    //单位：秒
    long expires_in
    long startTime
    long stopTime

    /**
     * 是否过期
     * @return true 过期，false 没有过期
     */
    public boolean isExpire() {
        return TokenContext.instance.getStopTime() < new Date().getTime()
    }

}
