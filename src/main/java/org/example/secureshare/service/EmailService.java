package org.example.secureshare.service;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Value("${spring.sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String senderEmail;

    public void sendKeyIvAndTagJson(String toEmail, String aesKey, String iv, String authTag) throws IOException {
        String jsonContent = "{\n" +
                "  \"aesKey\": \"" + aesKey + "\",\n" +
                "  \"iv\": \"" + iv + "\",\n" +
                "  \"authTag\": \"" + authTag + "\"\n" +
                "}";

        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        Email from = new Email(senderEmail);
        String subject = "Your File Decryption Keys";
        Email to = new Email(toEmail);
        String textContent = "Hello,\n\nAttached is the JSON file containing the AES key, IV, and Authentication Tag.\n\nKeep it safe!";
        Content content = new Content("text/plain", textContent);

        String jsonBase64 = Base64.getEncoder().encodeToString(jsonBytes);
        Attachments attachment = new Attachments();
        attachment.setContent(jsonBase64);
        attachment.setType("application/json");
        attachment.setFilename("decryption_keys.json");
        attachment.setDisposition("attachment");

        Mail mail = new Mail(from, subject, to, content);
        mail.addAttachments(attachment);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println("SendGrid Response Code: " + response.getStatusCode());

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                // Throw an exception if SendGrid failed
                throw new IOException("Failed to send email via SendGrid: " + response.getBody());
            }

        } catch (IOException ex) {
            // This catches network errors
            throw ex;
        }
    }
}