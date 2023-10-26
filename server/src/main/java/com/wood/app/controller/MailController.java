package com.wood.app.controller;

import com.wood.app.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MailController {

    @Autowired
    private EmailService emailService;

    @RequestMapping(value = "/email", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> sendEmail(@RequestBody Map<String, Object> params) {
        System.out.println("params=" + params);
        String email = "" + params.get("email");
        String udid = "" + params.get("UDID");
        Map<String, Object> result = new HashMap<>();
        if (email.isEmpty() || udid.isEmpty() || email.equals("null") || udid.equals("null")) {
            result.put("code", -1);
            result.put("msg", "email或udid错误");
            return result;
        }
        result.put("code", 0);
        result.put("msg", "邮件发送成功");
        try {
            emailService.sendMail(email, params);
        } catch (MessagingException e) {
            result.put("code", -2);
            result.put("msg", "邮件发送失败");
            e.printStackTrace();
        }
        return result;
    }

}
