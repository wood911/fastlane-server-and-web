package com.wood.app.entity;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AppConfigModel implements Serializable {
    public ArrayList<AppModel> apkList = new ArrayList<>();
    public ArrayList<AppModel> appList = new ArrayList<>();
    public ArrayList<AppModel> ipaList = new ArrayList<>();
    public String desc;
    public String domain;
    public String title;
    public String version;
}
