package com.wood.app.controller;

import com.wood.app.api.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: Hello服务测试
 * @Author wood
 * @Date 2020-12-09
 */

@RestController
public class HelloController {

    /**
     * keytool -genkeypair -alias app -keyalg RSA -keysize 2048 -keystore app.jks -validity 3650
     * keytool -importkeystore -srckeystore app.jks -destkeystore app.p12 -deststoretype pkcs12
     * @param name
     * @return
     */

    @GetMapping("/test")
    public ApiResult test(String name) {
        String regex = "\\[/?[a-zA-Z0-9\\u4e00-\\u9fa5]+\\]";
        String string = " /:oY投降 /:#-0fasdf /:hiphot[色]";
        Matcher matcher = Pattern.compile(regex).matcher(string);
        System.out.println(matcher.toMatchResult().groupCount());
        System.out.println(matcher.toString());
        return ApiResult.ok();
    }

}
