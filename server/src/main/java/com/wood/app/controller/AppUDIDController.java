package com.wood.app.controller;

import com.alibaba.fastjson.JSON;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import com.wood.app.api.ApiResult;
import com.wood.app.entity.UDIDEntity;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: App UDID获取
 * @Author wood
 * @Date 2020-12-09
 */

@RestController
public class AppUDIDController {
    private final Logger logger = LoggerFactory.getLogger(AppUDIDController.class);

    @Value("${devices.path}")
    private String devicePath;
    @Value("${devices.master.path}")
    private String deviceMasterPath;
    @Value("${local.service}")
    private String localService;

    @RequestMapping(value={"/udid", "/app/udid"})
    public ModelAndView getUDID(HttpServletRequest request, HttpServletResponse response) {
        UDIDEntity entity = new UDIDEntity();
        try {
            // 类型是 application/pkcs7-signature 签名的信息
            logger.info(request.getContentType());

            // 获取输入流
            InputStream stream = request.getInputStream();
            byte[] buffer = new byte[512];
            StringBuilder builder = new StringBuilder();
            while (stream.read(buffer) != -1) {
                builder.append(new String(buffer));
            }
            // 输入流的字符串
            String string = builder.toString();
            // 获取plist
            String plistString = string.substring(string.indexOf("<?xml"), string.indexOf("</plist>") + 8);
            logger.info(plistString);
            // 用google的dd-plist解析plist格式文件
            NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(plistString.getBytes());
            logger.info(rootDict.toString());
            // 将获取到的内容绑定实体
            if (rootDict.containsKey("IMEI")) {
                entity.setIMEI(rootDict.get("IMEI").toString());
            }
            if (rootDict.containsKey("PRODUCT")) {
                entity.setPRODUCT(rootDict.get("PRODUCT").toString());
            }
            if (rootDict.containsKey("UDID")) {
                entity.setUDID(rootDict.get("UDID").toString());
            }
            if (rootDict.containsKey("VERSION")) {
                entity.setVERSION(rootDict.get("VERSION").toString());
            }
            logger.info(entity.toString());

        } catch (Exception e) {
            logger.info("plist解析失败");
            e.printStackTrace();
        }

//        sendRequest(entity);
        saveLocal(entity);

        // 301之后iOS设备会自动打开safari浏览器，不设置会导致app安装描述文件失败
        RedirectView redirectView = new RedirectView("udid.html");
        redirectView.setAttributesMap(entity.toMap());
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return new ModelAndView(redirectView);
    }

    // 发送请求
    private void sendRequest(UDIDEntity entity) {
        String requestUrl = localService + "update/udid";
        //拼接入参
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        //入参
        RequestBody body = RequestBody.create(JSON.toJSONString(entity.toMap()), mediaType);

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(requestUrl)
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                //请求成功
                String result = response.body().string();
                logger.info(result);
            }
            @Override
            public void onFailure(@NotNull Call call, IOException e) {
                //请求失败
                String err = e.getMessage();
                logger.error(err);
            }
        });
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @RequestMapping(value = "/update/udid", method = RequestMethod.POST)
    public ApiResult receiveUdid(@org.springframework.web.bind.annotation.RequestBody String json) {
        logger.info(json);
        UDIDEntity entity = JSON.parseObject(json, UDIDEntity.class);
        if (entity != null && entity.getUDID() != null) {
            String udid = entity.getUDID();
            List<String> list = new ArrayList<>();
            list.add(devicePath);
            for (String s : list) {
                File file = new File(s);
                String content = readFile(file);
                if (content != null && !udid.isEmpty() && !content.contains(udid)) {
                    String builder = content + "\n" +
                            udid +
                            "\t" +
                            entity.getPRODUCT();
                    writeFile(file, builder);
                    logger.debug("成功添加UDID(" + udid + ")到文件" + file.getName());
                } else {
                    logger.debug(file.getName() + "已经包含该UDID(" + udid + ")");
                }
            }
            logger.debug("=====重新生成证书=====");

            return ApiResult.ok();
        }
        return new ApiResult(-1008, "无法获取UDID", null);
    }

    private void saveLocal(UDIDEntity entity) {
        String udid = entity.getUDID();
        List<String> list = new ArrayList<>();
        list.add(devicePath);
        list.add(deviceMasterPath);
        for (String s : list) {
            File file = new File(s);
            String content = readFile(file);
            if (content != null && !udid.isEmpty() && !content.contains(udid)) {
                String builder = content + "\n" +
                        udid +
                        "\t" +
                        entity.getPRODUCT();
                writeFile(file, builder);
                logger.debug("成功添加UDID(" + udid + ")到文件" + file.getName());
            } else {
                logger.debug(file.getName() + "已经包含该UDID(" + udid + ")");
            }
        }
    }

    private String readFile(File file) {
        try {
            return FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeFile(File file, String string) {
        try {
            FileUtils.writeStringToFile(file, string, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


