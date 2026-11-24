package com.chemindeguer.backend.comment;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentNotifierTest {
    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(sender);
        return p;
    }

    @Test
    void inactifSansConfigurationSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        new CommentNotifier(provider(sender), "", "", "admin@example.com", "https://site.fr").newComment("T", "A", "texte");
        verifyNoInteractions(sender);
    }

    @Test
    void envoieUnEmailAvecLeLienDAdministration() {
        JavaMailSender sender = mock(JavaMailSender.class);
        new CommentNotifier(provider(sender), "smtp.example.com", "blog@example.com", "admin@example.com", "https://site.fr/")
                .newComment("Mon article", "Dan", "Bravo");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("admin@example.com", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getText().contains("https://site.fr/admin"));
    }

    @Test
    void neLaissePasPasserDeSautDeLigneDansLeSujet() {
        JavaMailSender sender = mock(JavaMailSender.class);
        new CommentNotifier(provider(sender), "smtp.example.com", "", "admin@example.com", "https://site.fr")
                .newComment("Titre\r\nBcc: pirate@example.com", "A", "x");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertFalse(captor.getValue().getSubject().contains("\n"));
        assertFalse(captor.getValue().getSubject().contains("\r"));
    }

    @Test
    void unEchecDEnvoiNeRemontePas() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("SMTP down")).when(sender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> new CommentNotifier(provider(sender), "smtp.example.com", "", "admin@example.com", "https://site.fr")
                .newComment("T", "A", "x"));
    }
}
