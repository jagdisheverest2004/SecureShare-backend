package org.example.secureshare.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendKeyAndIvJson(String toEmail, String aesKey, String iv) throws MessagingException {
        // Prepare JSON content
        String jsonContent = "{\n" +
                "  \"aesKey\": \"" + aesKey + "\",\n" +
                "  \"iv\": \"" + iv + "\"\n" +
                "}";

        // Convert to byte array
        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        // Build email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Your File Decryption Key");
        helper.setText("Hello,\n\nAttached is the JSON file containing the AES key and IV for decrypting your file.\n\nKeep it safe!");

        // Attach JSON
        helper.addAttachment("decryption_keys.json",
                new ByteArrayResource(jsonBytes));

        mailSender.send(message);
    }
}
