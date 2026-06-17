package com.portfolio.financetracker.controller;

import com.portfolio.financetracker.dto.PushSubscriptionDto;
import com.portfolio.financetracker.model.PushSubscription;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.PushSubscriptionRepository;
import com.portfolio.financetracker.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    public PushController(PushSubscriptionRepository pushSubscriptionRepository,
                          UserRepository userRepository) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionDto dto, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Optional<PushSubscription> existingOpt = pushSubscriptionRepository.findByEndpoint(dto.getEndpoint());
        if (existingOpt.isPresent()) {
            PushSubscription existing = existingOpt.get();
            existing.setP256dh(dto.getP256dh());
            existing.setAuth(dto.getAuth());
            pushSubscriptionRepository.save(existing);
        } else {
            PushSubscription newSub = new PushSubscription();
            newSub.setEndpoint(dto.getEndpoint());
            newSub.setP256dh(dto.getP256dh());
            newSub.setAuth(dto.getAuth());
            newSub.setUser(user);
            pushSubscriptionRepository.save(newSub);
        }

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody PushSubscriptionDto dto) {
        pushSubscriptionRepository.deleteByEndpoint(dto.getEndpoint());
        return ResponseEntity.ok().build();
    }
}
