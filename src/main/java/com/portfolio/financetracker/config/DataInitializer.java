package com.portfolio.financetracker.config;

import com.portfolio.financetracker.model.Category;
import com.portfolio.financetracker.model.PaymentMethod;
import com.portfolio.financetracker.model.Role;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.CategoryRepository;
import com.portfolio.financetracker.repository.PaymentMethodRepository;
import com.portfolio.financetracker.repository.RoleRepository;
import com.portfolio.financetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           CategoryRepository categoryRepository, PaymentMethodRepository paymentMethodRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }

        Role userRole = roleRepository.save(Role.builder().name("ROLE_USER").build());
        Role adminRole = roleRepository.save(Role.builder().name("ROLE_ADMIN").build());

        categoryRepository.saveAll(List.of(
                Category.builder().name("Alimentari").icon("bi-cart").build(),
                Category.builder().name("Stipendio").icon("bi-briefcase").build(),
                Category.builder().name("Svago").icon("bi-controller").build(),
                Category.builder().name("Trasporti").icon("bi-car-front").build(),
                Category.builder().name("Salute").icon("bi-heart-pulse").build(),
                Category.builder().name("Abbonamenti").icon("bi-repeat").build(),
                Category.builder().name("Affitto").icon("bi-house-door").build(),
                Category.builder().name("Bollette").icon("bi-lightning").build(),
                Category.builder().name("Mutuo/Prestito").icon("bi-bank").build(),
                Category.builder().name("Altro").icon("bi-three-dots").build()
        ));

        paymentMethodRepository.saveAll(List.of(
                PaymentMethod.builder().name("Contanti").build(),
                PaymentMethod.builder().name("Carta di Credito").build(),
                PaymentMethod.builder().name("Carta di Debito").build(),
                PaymentMethod.builder().name("Bonifico SEPA").build(),
                PaymentMethod.builder().name("PayPal").build()
        ));

        userRepository.save(User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .roles(Set.of(userRole, adminRole))
                .build());
    }
}
