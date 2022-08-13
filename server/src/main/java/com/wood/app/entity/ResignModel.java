package com.wood.app.entity;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResignModel implements Serializable {
    // 设备UDID 7c06db4023a02098ea7826d1c4c3cf4d394dc5d7 (苹果回调)
    public String udid;
    // app版本 bundleID
    public String appId;
    // app版本 1.0.0
    public String version;
    // app构建版本号 1
    public String build;
    // 代理渠道号
    public String channelCode;
    // 苹果账号信息
//    public AppleAccount appleAccount;

}
