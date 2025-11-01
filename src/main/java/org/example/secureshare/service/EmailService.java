package org.example.secureshare.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from}") // <-- ADD THIS LINE
    private String senderEmail;

    public void sendKeyIvAndTagJson(String toEmail, String aesKey, String iv, String authTag) throws MessagingException {
        String jsonContent = "{\n" +
                "  \"aesKey\": \"" + aesKey + "\",\n" +
                "  \"iv\": \"" + iv + "\",\n" +
                "  \"authTag\": \"" + authTag + "\"\n" +
                "}";

        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your File Decryption Keys");
        helper.setText("Hello,\n\nAttached is the JSON file containing the AES key, IV, and Authentication Tag.\n\nKeep it safe!");

        helper.addAttachment("decryption_keys.json", new ByteArrayResource(jsonBytes));

        mailSender.send(message);
    }

}
