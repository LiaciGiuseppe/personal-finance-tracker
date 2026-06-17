package com.portfolio.financetracker.service;

import com.portfolio.financetracker.model.PushSubscription;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Service
public class PushNotificationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Value("${vapid.private.key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    public PushNotificationService(PushSubscriptionRepository pushSubscriptionRepository) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private int extractStatusCode(Exception e) {
        String msg = e.getMessage();
        if (msg != null) {
            int idx = msg.indexOf("status code: ");
            if (idx >= 0) {
                try {
                    String codeStr = msg.substring(idx + "status code: ".length());
                    int endIdx = 0;
                    while (endIdx < codeStr.length() && Character.isDigit(codeStr.charAt(endIdx))) {
                        endIdx++;
                    }
                    if (endIdx > 0) {
                        return Integer.parseInt(codeStr.substring(0, endIdx));
                    }
                } catch (Exception ignored) {}
            }
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null) {
                int idx = causeMsg.indexOf("status code: ");
                if (idx >= 0) {
                    try {
                        String codeStr = causeMsg.substring(idx + "status code: ".length());
                        int endIdx = 0;
                        while (endIdx < codeStr.length() && Character.isDigit(codeStr.charAt(endIdx))) {
                            endIdx++;
                        }
                        if (endIdx > 0) {
                            return Integer.parseInt(codeStr.substring(0, endIdx));
                        }
                    } catch (Exception ignored) {}
                }
            }
            cause = cause.getCause();
        }
        return 0;
    }

    public void sendNegativeBalanceAlert(User user, BigDecimal balance) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUser(user);
        if (subscriptions.isEmpty()) {
            return;
        }

        String payload = "{\n" +
                "  \"title\": \"\\u26a0\\ufe0f Saldo negativo\",\n" +
                "  \"body\": \"Il tuo saldo \\u00e8 " + balance.setScale(2, RoundingMode.HALF_UP) + " \\u20ac. Controlla le tue spese!\",\n" +
                "  \"icon\": \"/icons/icon-192x192.png\",\n" +
                "  \"url\": \"/dashboard\"\n" +
                "}";

        PushService pushService;
        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } catch (GeneralSecurityException e) {
            log.error("Errore nella creazione del PushService: {}", e.getMessage());
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes()
                );
                pushService.send(notification);
                log.info("Notifica push inviata all'utente {} (endpoint: {}...)", user.getUsername(),
                        sub.getEndpoint().substring(0, Math.min(50, sub.getEndpoint().length())));
            } catch (Exception e) {
                int statusCode = extractStatusCode(e);
                if (statusCode == 404 || statusCode == 410) {
                    log.warn("Subscription scaduta/revocata (HTTP {}) per endpoint {}... Rimozione dal DB.",
                            statusCode, sub.getEndpoint().substring(0, Math.min(50, sub.getEndpoint().length())));
                    pushSubscriptionRepository.delete(sub);
                } else {
                    log.error("Errore nell'invio della notifica push a {}: {}", sub.getEndpoint(), e.getMessage());
                }
            }
        }
    }
}
