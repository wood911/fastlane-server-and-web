package com.wood.app.entity;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AppModel implements Serializable {
//    build: 114,
//    desc: "android 5.31.0提测，上传alioss服务器测试 测试环境",
//    domain: "https://dl.superbuy.com/",
//    name: "superbuy_v5.31.0_a_googlepaly_1.apk",
//    path: "app/superbuy/Android/20191128164855/",
//    size: 35350718,
//    time: 1574930935,
//    version: "5.31.0"
    public int build;
    public String desc;
    public String domain;
    public String name;
    public String path;
    public long size;
    public long time;
    public String version;
    public String bundleId;
}
