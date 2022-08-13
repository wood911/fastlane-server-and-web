package com.wood.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

@Component
public class EmailService {
    @Resource
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;
    // template模板引擎
    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 发送纯文本邮件.
     *
     * @param to      目标email 地址
     * @param params  纯文本内容
     */
    public void sendMail(String to, Map<String, Object> params) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject("设备UDID");
        // 使用模板thymeleaf
        Context context = new Context();
        // 定义模板数据
        context.setVariables(params);
        // 获取thymeleaf的html模板
        String emailContent = templateEngine.process("mail", context); // 指定模板路径
        helper.setText(emailContent,true);
        // 发送邮件
        javaMailSender.send(mimeMessage);
    }

    /**
     * 发送纯文本邮件.
     *
     * @param to      目标email 地址
     * @param subject 邮件主题
     * @param text    纯文本内容
     */
    public void sendMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }

    /**
     * 发送邮件并携带附件.
     * 请注意 from 、 to 邮件服务器是否限制邮件大小
     *
     * @param to       目标email 地址
     * @param subject  邮件主题
     * @param text     纯文本内容
     * @param filePath 附件的路径 当然你可以改写传入文件
     */
    public void sendRichMail(String to, String subject, String text, String[] filePath) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(mimeMessage,true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        Arrays.stream(filePath).map(File::new).forEach(file -> {
            try {
                helper.addAttachment(file.getName(), file);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        });
        javaMailSender.send(mimeMessage);
    }
}
