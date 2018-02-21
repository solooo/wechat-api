package net.solooo.wechat.service;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * Author:Eric
 * Date:2017/10/25
 */
public class WxApi {

    private static Logger logger = LoggerFactory.getLogger(WxApi.class);

    /**
     *
     */
    private static final int LENGTH = 2;

    /**
     * byte 转 int换算
     */
    private static final int HEX = 0xFF;

    // get access_token
    private static final String ACCESS_TOKEN = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={0}&secret={1}";

    // get user info
    private static final String USER_INFO = "https://api.weixin.qq.com/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";

    // get menu
    private static final String GET_MENU = "https://api.weixin.qq.com/cgi-bin/menu/get?access_token={0}";

    // create menu
    private static final String CREATE_MENU = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token={0}";

    // delete menu
    private static final String DELETE_MENU = "https://api.weixin.qq.com/cgi-bin/menu/delete?access_token={0}";

    // 网页授取获取 token 和 openID
    private static final String OPEN_ID = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";

    // 获取jsapi_ticket
    private static final String JSAPI_TICKET = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token={0}&type=jsapi";

    // 下载图片
    private static final String MEDIA_DOWNLOAD = "https://api.weixin.qq.com/cgi-bin/media/get?access_token={0}&media_id={1}";

    // 发送模板消息
    private static final String SENT_TEMPLATE = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token={0}";
    /**
     * cache access_token
     */
    private static String accessToken;

    /**
     * cache jsapi_ticket
     */
    private static String jsapiTicket;


    /**
     * 接入验证
     *
     * @param token     token
     * @param timestamp timestamp
     * @param nonce     nonce
     * @param signature signature
     * @return blooean
     * @author PeiJian
     */
    public static boolean checkSign(String token, String timestamp, String nonce, String signature) {
        String[] ttn = new String[] { token, timestamp, nonce };
        Arrays.sort(ttn);
        String temp = StringUtils.join(ttn, "");
        String s = sha1Encryption(temp);
        return StringUtils.isNotBlank(s)
                && StringUtils.isNotBlank(signature)
                && s.equals(signature);
    }

    /**
     * sha1加密
     *
     * @param str str
     * @return string
     * @author PeiJian
     */
    private static String sha1Encryption(String str) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = digest.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String shaHex = Integer.toHexString(aMessageDigest & HEX);
                if (shaHex.length() < LENGTH) {
                    sb.append(0);
                }
                sb.append(shaHex);
            }
            String s = sb.toString();
            if (StringUtils.isNotBlank(s)) {
                return s;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取access_token
     *
     * @param appId     appId
     * @param appsecret appsecret
     * @return string
     * @author PeiJian
     */
    public static String getAccessToken(String appId, String appsecret, Long refreshTokenTime) {
        if (StringUtils.isBlank(accessToken)) {
            accessToken = refreshToken(appId, appsecret);
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleWithFixedDelay(() -> accessToken = refreshToken(appId, appsecret),
                    refreshTokenTime,
                    refreshTokenTime, TimeUnit.SECONDS);
        }
        return accessToken;
    }

    /**
     * 刷新access_token
     *
     * @param appId
     * @param secret
     * @return
     */
    private static String refreshToken(String appId, String secret) {
        logger.info("刷新access_token...");
        String url = MessageFormat.format(ACCESS_TOKEN, appId, secret);
        String accessResult = sendGet(url);
        Map accessMap = JSON.parseObject(accessResult, Map.class);
        if (accessMap.get("access_token") != null) {
            return accessMap.get("access_token").toString();
        }
        logger.error("access_token获取失败：" + accessMap);
        return "";
    }

    /**
     * 网页授权获取用户openID
     * @param appId
     * @param secret
     * @param code
     * @return
     */
    public static Map getOpenId(String appId, String secret, String code) {
        String url = MessageFormat.format(OPEN_ID, appId, secret, code);
        String result = sendGet(url);
        return JSON.parseObject(result, Map.class);
    }

    /**
     * 获取jspapi_ticket
     * @param accessToken
     * @return
     */
    public static String getJsapiTicket(String accessToken) {
        if (StringUtils.isBlank(jsapiTicket)) {
            jsapiTicket = refreshJsapiTicket(accessToken);
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleWithFixedDelay(() -> jsapiTicket = refreshJsapiTicket(accessToken),
                    7200, 7200, TimeUnit.SECONDS);
        }
        return jsapiTicket;
    }

    /**
     * 定时刷新jsapi_ticket
     * @param accessToken
     * @return
     */
    private static String refreshJsapiTicket(String accessToken) {
        String url = MessageFormat.format(JSAPI_TICKET, accessToken);
        String result = sendGet(url);
        Map map = JSON.parseObject(result, Map.class);
        return (String) map.get("ticket");
    }

    /**
     * js_sdk签名
     * @param nonceStr
     * @param ticket
     * @param timestamp
     * @param url
     * @return
     */
    public static String getSignature(String nonceStr, String ticket, Long timestamp, String url) {
        String str = "jsapi_ticket={0}&noncestr={1}&timestamp={2,number,#}&url={3}";
        String paramStr = MessageFormat.format(str, ticket, nonceStr, timestamp, url);
        return sha1Encryption(paramStr);
    }

    /**
     * 照片下载
     * @param accessToken
     * @param mediaId
     * @param target
     * @return
     */
    public static boolean download(String accessToken, String mediaId, String target) {
        String urlStr = MessageFormat.format(MEDIA_DOWNLOAD, accessToken, mediaId);
        try {
            File file = new File(target);
            file.getParentFile().mkdirs();
            URL url = new URL(urlStr);
            try (InputStream in = url.openStream()) {
                Files.copy(in, Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 发送模板消息
     * @param accessToken
     * @param msg
     * @return
     */
    public static String sentTempMsg(String accessToken, String msg) {
        String url = MessageFormat.format(SENT_TEMPLATE, accessToken);
        return sendPost(url, msg);
    }

    /**
     * 自定义菜单查询
     *
     * @param accessToken
     * @return
     */
    public static String getMenu(String accessToken) {
        String url = MessageFormat.format(GET_MENU, accessToken);
        return sendGet(url);
    }

    /**
     * create menu
     * @param accessToken
     * @param menuJson
     * @return
     */
    public static String createMenu(String accessToken, String menuJson) {
        String url = MessageFormat.format(CREATE_MENU, accessToken);
        return sendPost(url, menuJson);
    }

    /**
     * delete menu
     * @param accessToken
     * @return
     */
    public static String deleteMenu(String accessToken) {
        String url = MessageFormat.format(DELETE_MENU, accessToken);
        return sendGet(url);
    }

    /**
     * get user info
     * @param accessToken
     * @param openId
     * @return
     */
    public static String getUserInfo(String accessToken, String openId) {
        String format = MessageFormat.format(USER_INFO, accessToken, openId);
        return sendGet(format);
    }

    /**
     * get request
     *
     * @param url
     * @return
     */
    public static String sendGet(String url) {
        return send("GET", url, null);
    }

    /**
     * post request
     *
     * @param url
     * @param param
     * @return
     */
    public static String sendPost(String url, String param) {
        return send("POST", url, param);
    }

    /**
     * 发送请求
     *
     * @param urlpath urlpath
     * @param type    type
     * @param param   param
     * @return string
     * @author Administrator
     */
    private static String send(String type, String urlpath, String param) {
        String resStr = "";
        try {
            // 创建连接
            URL url = new URL(urlpath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod(type);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;encoding=utf-8");
            connection.connect();
            // post请求发送参数
            if ("POST".equalsIgnoreCase(type)) {
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.write(param.getBytes("UTF-8"));
                out.flush();
                out.close();
            }

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "utf-8"));
            String lines;
            StringBuilder sb = new StringBuilder("");
            while ((lines = reader.readLine()) != null) {
                lines = new String(lines.getBytes());
                sb.append(lines);
            }
            reader.close();
            connection.disconnect();
            resStr = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return resStr;
    }

}
