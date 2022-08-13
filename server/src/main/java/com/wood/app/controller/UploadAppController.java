package com.wood.app.controller;

import com.alibaba.fastjson.JSON;
import com.wood.app.api.ApiResult;
import com.wood.app.entity.AppConfigModel;
import com.wood.app.entity.AppModel;
import com.wood.app.service.ResignService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@RestController
public class UploadAppController {

    private static final Logger logger = LoggerFactory.getLogger(UploadAppController.class);

    @Value("${app.path}")
    private String appPath;
    @Value("${app.config.json}")
    private String configJsonPath;
    @Value("${app.domain}")
    private String appDomain;

    @Autowired
    private ResignService resignService;

    @ResponseBody
    @RequestMapping(value = "/test/dir")
    public ApiResult getApp() {
        File file = new File(appPath);
        if (file.exists() && file.isDirectory()) {
            return new ApiResult(0, appPath + "目录可正常访问", null);
        }
        return new ApiResult(-1006, appPath + "目录不存在", null);
    }

    @ResponseBody
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ApiResult uploadApp(MultipartFile file, MultipartFile plistfile, AppModel model) {
        logger.debug(model.toString());
        if (file == null || file.isEmpty()) {
            return new ApiResult(-1001, "文件不能为空", null);
        }
        // 如果服务器目录不存在，创建
        File appPathFile = new File(appPath);
        if (!appPathFile.exists()) {
            appPathFile.mkdirs();
        }
        // 读取config.json
        AppConfigModel configModel = null;
        File configJsonFile = new File(configJsonPath);
        if (!configJsonFile.exists()) { // 文件不存在创建config.json
            configModel = new AppConfigModel();
            configModel.domain = appDomain;
            configModel.title = "很高兴邀请您安装我们的App，测试并反馈问题，便于我们及时解决您遇到的问题，十分谢谢！Thanks";
            configModel.version = "0.1.0";
            configModel.desc = "每个版本的配置文件，记录着所有历史版本。";
            String jsonString = JSON.toJSONString(configModel);
            logger.debug(jsonString);
            writeFile(configJsonFile, jsonString);
        } else { // 文件存在读取配置内容
            configModel = JSON.parseObject(readFile(configJsonFile), AppConfigModel.class);
        }
        if (configModel == null) {
            return new ApiResult(-1002, "config.json文件读取失败", null);
        }
        String datetime = new DateFormatter("yyyyMMddHHmmss").print(new Date(), Locale.CHINA);
        model.time = System.currentTimeMillis() / 1000;
        model.domain = appDomain;
        if (model.size == 0) {
            model.size = file.getSize();
        }
        model.name = file.getOriginalFilename();
        String originalFilename = file.getOriginalFilename();
        // 根据文件名区分平台
        String[] list = originalFilename.split("\\.");
        boolean ios = true;
        if (list.length > 1) {
            ios = list[1].equalsIgnoreCase("ipa");
            model.path = (ios ? "ios" : "android") + "/"+ list[0] +"/" + datetime + "/";
            if (ios) {
                configModel.ipaList.add(0, model);
            } else {
                configModel.apkList.add(0, model);
            }
        } else {
            ios = false;
            model.path = datetime + "/";
            configModel.appList.add(0, model);
        }
        try {
            // 先删除文件再创建文件
            FileUtils.forceDelete(configJsonFile);
            writeFile(configJsonFile, JSON.toJSONString(configModel));
            // 创建目标文件夹
            String desPath = appPath + model.path;
            File desPathFile = new File(desPath);
            if (!desPathFile.exists()) {
                desPathFile.mkdirs();
            }
            if (ios) {
                // 写入plist
                File plistFile = new File(desPath + "manifest.plist");
                writeFile(plistFile, plistString(model));
            }
            // 写入package
            IOUtils.write(file.getBytes(), new FileOutputStream(desPath + model.name));
            // 写入Info.plist
            if (plistfile != null) {
                IOUtils.write(plistfile.getBytes(), new FileOutputStream(desPath + plistfile.getOriginalFilename()));
            }
        } catch (IOException e) {
            return new ApiResult(-1004, "文件读写失败", null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("plist", model.domain + "download/" + model.path + "manifest.plist");

        // 用第一个账号签名
        new Thread(() -> resignService.resign(model)).start();

        return ApiResult.ok(data);
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

    private String plistString(AppModel model) {
        StringBuilder string = new StringBuilder();
        string.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        string.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        string.append("<plist version=\"1.0\">\n");
        string.append("<dict>\n");
        string.append("<key>items</key>\n");
        string.append("<array>\n");
        string.append("<dict>\n");
        string.append("<key>assets</key>\n");
        string.append("<array>\n");
        string.append("<dict>\n");
        string.append("<key>kind</key>\n");
        string.append("<string>software-package</string>\n");
        string.append("<key>url</key>\n");
        string.append("<string>"+ model.domain + "download/" + model.path + model.name +"</string>\n");
        string.append("</dict>\n");
        string.append("<dict>\n");
        string.append("<key>kind</key>\n");
        string.append("<string>full-size-image</string>\n");
        string.append("<key>url</key>\n");
        string.append("<string>"+ model.domain + "download/" +"ios/icon-512.png</string>\n");
        string.append("</dict>\n");
        string.append("</array>\n");
        string.append("<key>metadata</key>\n");
        string.append("<dict>\n");
        string.append("<key>bundle-identifier</key>\n");
        string.append("<string>"+ model.bundleId +"</string>\n");
        string.append("<key>bundle-version</key>\n");
        string.append("<string>"+ model.version +"</string>\n");
        string.append("<key>kind</key>\n");
        string.append("<string>software</string>\n");
        string.append("<key>platform-identifier</key>\n");
        string.append("<string>com.apple.platform.iphoneos</string>\n");
        string.append("<key>title</key>\n");
        string.append("<string>"+ model.name +"</string>\n");
        string.append("</dict>\n");
        string.append("</dict>\n");
        string.append("</array>\n");
        string.append("</dict>\n");
        string.append("</plist>\n");

        return string.toString();
    }

}
