package com.chemindeguer.backend.comment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Prévient l'administrateur par email d'un nouveau commentaire à modérer.
 * Inactif tant que MAIL_HOST n'est pas renseigné : aucune configuration ne casse le site.
 */
@Component
public class CommentNotifier {
    private static final Logger log = LoggerFactory.getLogger(CommentNotifier.class);
    private static final int EXCERPT = 600;

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;
    private final String to;
    private final String publicUrl;

    public CommentNotifier(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${spring.mail.host:}") String host,
                           @Value("${app.mail.from:}") String from,
                           @Value("${app.mail.to:}") String to,
                           @Value("${app.public-url:http://localhost}") String publicUrl) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
        this.to = to;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    boolean enabled() {
        return !host.isBlank() && !to.isBlank();
    }

    @Async
    public void newComment(String articleTitle, String author, String content) {
        if (!enabled()) {
            return;
        }
        var sender = mailSender.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            var message = new SimpleMailMessage();
            message.setFrom(from.isBlank() ? to : from);
            message.setTo(to);
            message.setSubject(oneLine("Nouveau commentaire à modérer : " + articleTitle));
            String excerpt = content.length() > EXCERPT ? content.substring(0, EXCERPT) + "…" : content;
            message.setText("Article : " + oneLine(articleTitle) + "\nAuteur : " + oneLine(author)
                    + "\n\n" + excerpt + "\n\nModérer : " + publicUrl + "/admin\n");
            sender.send(message);
        } catch (Exception e) {
            // Un échec d'envoi ne doit jamais empêcher l'enregistrement du commentaire
            log.warn("Notification email impossible : {}", e.getMessage());
        }
    }

    /** Empêche l'injection d'en-têtes email via un saut de ligne. */
    private static String oneLine(String s) {
        return s.replaceAll("[\\r\\n]+", " ").trim();
    }
}
