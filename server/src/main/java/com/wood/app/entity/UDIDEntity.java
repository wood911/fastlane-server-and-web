package com.wood.app.entity;

import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: UDIDEntity
 * @Author wood
 * @Date 2020-12-09
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class UDIDEntity implements Serializable {
    private String UDID;
    private String IMEI;
    private String VERSION;
    private String PRODUCT;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("UDID", this.UDID);
        map.put("IMEI", this.IMEI);
        map.put("VERSION", this.VERSION);
        map.put("PRODUCT", this.PRODUCT);
        return map;
    }
}
