package com.wood.app.entity;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AppleAccount implements Serializable {
    // 苹果appleID Madalin.Lickli@mail.com
    public String appleId;
    // 密码 Xyd002345
    public String password;
    // +855 86705578
    public String phone;
    // SGRLKBH73N
    public String teamId;
    // 123064250
    public String providerId;
    // 6ecd7128-944c-4f5f-a099-3edcae4153f8
    public String issuerId;
    // 6MA8646Q84
    public String keyId;
    // p8私钥
    public String key;

}

//Jordan.Labetskii@mail.com----Xyd002345----QQ5P05LM9p----生日:1988年10月1日密保:朋友x111工作y112父母d113
//        test-Timmy: 963218810
//        Team ID: TLHJ748PKY
//        contentProviderId: 123090896
//        Issuer ID: 6ecd7128-944c-4f5f-a099-3edcae4153f8  6MA8646Q84
