package net.solooo.wechat.service;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * Description:
 * Author:Eric
 * Date:2017/10/25
 */
@Service
public class WxService {

    private Logger logger = LoggerFactory.getLogger(WxService.class);

    @Autowired
    private WxConfig wxConfig;

    private String rootPath = "/opt/images";

    /**
     * 获取accessToken, 缓存并刷新token
     * @return string
     */
    public String getAccessToken() {
        return WxApi.getAccessToken(wxConfig.getAppId(), wxConfig.getSecret(), wxConfig.getRefreshTokenTime());
    }

    /**
     * JS-SDK接入参数
     * @param url
     * @return
     */
    public Map<String, Object> jsSDK(String url) {
        String ticket = WxApi.getJsapiTicket(getAccessToken());
        String nonceStr = "xxxx"; // 随机生成字符串
        Long timestamp = new Date().getTime() / 1000;
        String signature = WxApi.getSignature(nonceStr, ticket, timestamp, url);
        Map<String, Object> map = new HashMap<>();
        map.put("appId", wxConfig.getAppId());
        map.put("timestamp", timestamp);
        map.put("nonceStr", nonceStr);
        map.put("signature", signature);
        System.out.println("------------------------------------");
        System.out.println("nonceStr: " + nonceStr);
        System.out.println("ticket: " + ticket);
        System.out.println("timestamp: " + timestamp);
        System.out.println("url: " + url);
        System.out.println("------------------------------------");
        return map;
    }

    /**
     * 获取用户微信openID
     * @param code
     * @return
     */
    public Map getOpenId(String code) {
        Map userToken = WxApi.getOpenId(wxConfig.getAppId(), wxConfig.getSecret(), code);
        String access_token = (String) userToken.get("access_token");
        String openid = (String) userToken.get("openid");
        String userInfo = WxApi.getUserInfo(access_token, openid);
        return JSON.parseObject(userInfo, Map.class);
    }

    /**
     * 下载微信图片
     * @param dir
     * @param mediaIds
     * @return
     */
    public List<Integer> getImageIds(String dir, List<String> mediaIds) {
        return this.getImageIds(dir, mediaIds.toArray(new String[] {}));
    }


    /**
     * 下载微信图片
     * @param dir
     * @param mediaIds
     * @return
     */
    public List<Integer> getImageIds(String dir, String... mediaIds) {
        List<Integer> imageIds = new ArrayList<>();
        if (StringUtils.isBlank(dir)) {
            dir = "default";
        }
        for (String id : mediaIds) {
            String fileName = new Date().getTime() + ".jpg";
            String filePath = dir + File.separator + fileName;
            boolean result = WxApi.download(this.getAccessToken(), id, rootPath + filePath);
            if (result) {
                // 图片下载后逻辑处理
            }
        }
        return imageIds;
    }
}
