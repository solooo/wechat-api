package net.solooo.wechat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Description:
 * Author:Eric
 * Date:2017/10/27
 */
@Component
public class WxConfig {
    @Value("${wx.appid}")
    private String appId;

    @Value("${wx.secret}")
    private String secret;

    @Value("${wx.refreshTokenTime}")
    private Long refreshTokenTime;

    public String getAppId() {
        return appId;
    }

    public String getSecret() {
        return secret;
    }

    public Long getRefreshTokenTime() {
        return refreshTokenTime;
    }
}
