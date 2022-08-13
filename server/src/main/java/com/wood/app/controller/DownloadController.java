package com.wood.app.controller;

import com.wood.app.api.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController
public class DownloadController {

    @Value("${app.path}")
    private String appPath;

    @CrossOrigin
    @GetMapping("/download/**")
    public ApiResult test(HttpServletRequest request, HttpServletResponse response) {
        String fullPath = request.getServletPath();
        System.out.println(fullPath.substring(10));
        String filepath = fullPath.substring(10);
        File file = new File(appPath + filepath);
        if(!file.exists()){
            return new ApiResult(-1005, "下载文件不存在", null);
        }
        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + filepath);

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "PUT, POST, GET, OPTIONS");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "content-type, authorization");

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buff = new byte[1024];
            OutputStream os  = response.getOutputStream();
            int i = 0;
            while ((i = bis.read(buff)) != -1) {
                os.write(buff, 0, i);
                os.flush();
            }
        } catch (IOException e) {
            return new ApiResult(-1005, "下载文件不存在", null);
        }
        return ApiResult.ok();
    }

}
