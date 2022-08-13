package com.wood.app.service;

import com.wood.app.api.ApiResult;
import com.wood.app.entity.AppModel;
import com.wood.app.util.RuntimeExec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ResignService {

    public void resign(AppModel model) {
        log.info(model.toString());
        // fastlane ios resign_app app_identifier:$1 version:$2 udid:$3 channelcode:$4 domain:$5 path:$6
        String cmd = "/bin/sh /root/appsign/build.sh " +
                model.bundleId +
                model.version +
                " '' '' " +
                model.domain +
                model.path +
                " > /dev/null 2>&1";
        log.info("开始调用fastlane重签 = {}", cmd);
        Map<String, Object> map = RuntimeExec.runtimeExec(cmd);
        log.info("CMD执行结果 = {}", map);
        int code = (int) map.get("status");
        if (code != 0) {
            new ApiResult(code, map.toString(), map);
        }
    }

}
