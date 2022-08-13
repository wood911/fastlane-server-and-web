package com.wood.app.controller;

import com.alibaba.fastjson.JSONObject;
import com.wood.app.api.ApiResult;
import com.wood.app.entity.ResignModel;
import com.wood.app.util.RuntimeExec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
public class ResignController {

    @RequestMapping(value = "/resign", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult resign(@RequestBody ResignModel model) {
        log.info(model.toString());
//        RuntimeExec.runtimeExec("cd /root/wood/resign && pwd");
        String cmd = "fastlane ios resign_app " +
                "channelcode:'1111111' " +
                "udid:'7c06db4023a02098ea7826d1c4c3cf4d394dc5d7' " +
                "app_identifier:'com.soo.yiappvi' " +
                "apple_id:'Madalin.Lickli@mail.com' " +
                "team_id:'SGRLKBH73N' " +
                "version:'1.0.0'";
        cmd = "/bin/sh /root/wood/resign/build.sh";
        log.info("开始调用fastlane重签 = {}", cmd);
        Map<String, Object> map = RuntimeExec.runtimeExec(cmd);
        log.info("CMD执行结果 = {}", map);
        int code = (int) map.get("status");
        if (code != 0) {
            return new ApiResult(code, map.toString(), map);
        }

        return ApiResult.ok();
    }


    @RequestMapping(value = "/app/revoke", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult revoke(@RequestBody Map<String, Object> model) {
        log.debug(JSONObject.toJSONString(model));
        return ApiResult.ok();
    }

    @RequestMapping(value = "/app/profile", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult profile(@RequestBody Map<String, Object> model) {
        log.debug(JSONObject.toJSONString(model));
        return ApiResult.ok();
    }

    @RequestMapping(value = "/app/error", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult error(@RequestBody Map<String, Object> model) {
        log.debug(JSONObject.toJSONString(model));
        return ApiResult.ok();
    }


}
