package com.wood.app.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.wood.app.api.ApiResult;
import com.wood.app.util.DingCallbackCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.wood.app.config.Constant.*;

/**
 * @Description: Hello服务测试
 * @Author wood
 * @Date 2020-12-09
 */

@RestController
public class DingController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping(value = "/dd/callback", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> ddCallback(@RequestParam String msg_signature,
                            @RequestParam String timestamp,
                            @RequestParam String nonce,
                            @RequestBody JSONObject json) throws DingCallbackCrypto.DingTalkEncryptException {
        // 1. 从http请求中获取加解密参数

        // 2. 使用加解密类型
        // Constant.OWNER_KEY 说明：
        // 1、开发者后台配置的订阅事件为应用级事件推送，此时OWNER_KEY为应用的APP_KEY。
        // 2、调用订阅事件接口订阅的事件为企业级事件推送，
        //      此时OWNER_KEY为：企业的appkey（企业内部应用）或SUITE_KEY（三方应用）
        DingCallbackCrypto callbackCrypto = new DingCallbackCrypto(AES_TOKEN, AES_KEY, OWNER_KEY);
        String encryptMsg = json.getString("encrypt");
        String decryptMsg = callbackCrypto.getDecryptMsg(msg_signature, timestamp, nonce, encryptMsg);
        logger.info("解密消息：" + decryptMsg);

        // 3. 反序列化回调事件json数据
        JSONObject eventJson = JSON.parseObject(decryptMsg);
        String eventType = eventJson.getString("EventType");

        // 4. 根据EventType分类处理
        if ("check_url".equals(eventType)) {
            // 测试回调url的正确性
            logger.info("测试回调url的正确性");
        } else if ("user_add_org".equals(eventType)) {
            // 处理通讯录用户增加事件
            logger.info("发生了：" + eventType + "事件");
        } else {
            // 添加其他已注册的
            logger.info("发生了：" + eventType + "事件");
        }

        // 5. 返回success的加密数据
        Map<String, String> successMap = callbackCrypto.getEncryptedMap("success");

        return successMap;
    }

    @GetMapping("/dd/token")
    public ApiResult getToken() throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
        OapiGettokenRequest request = new OapiGettokenRequest();
        request.setAppkey(APP_KEY);
        request.setAppsecret(APP_SECRET);
        request.setHttpMethod("GET");
        OapiGettokenResponse response = client.execute(request);
        logger.info(response.getBody());
        if (response.isSuccess()) {
            DD_TOKEN = response.getAccessToken();
            return ApiResult.ok(JSON.parse(response.getBody()));
        } else {
            return new ApiResult(Math.toIntExact(response.getErrcode()), response.getErrmsg(), response.getBody());
        }
    }

}
