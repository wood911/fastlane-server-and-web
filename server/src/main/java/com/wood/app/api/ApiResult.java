package com.wood.app.api;

import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ApiResult implements Serializable {
    public int code;
    public String msg;
    public Object data;

    public static ApiResult ok() {
        return new ApiResult(0, "success", new HashMap<>());
    }

    public static ApiResult ok(Object data) {
        return new ApiResult(0, "success", data);
    }
}
