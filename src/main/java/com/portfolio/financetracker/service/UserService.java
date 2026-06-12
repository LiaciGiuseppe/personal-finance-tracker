package com.portfolio.financetracker.service;

import com.portfolio.financetracker.dto.UserRegistrationDto;
import com.portfolio.financetracker.exception.PasswordMismatchException;
import com.portfolio.financetracker.exception.UsernameAlreadyExistsException;
import com.portfolio.financetracker.model.Role;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.RoleRepository;
import com.portfolio.financetracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuovo utente con ruolo ROLE_USER.
     *
     * @param dto i dati di registrazione
     * @return l'utente salvato
     * @throws UsernameAlreadyExistsException se l'email è già registrata
     * @throws PasswordMismatchException      se le password non coincidono
     */
    public User register(UserRegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UsernameAlreadyExistsException("Email già registrata: " + dto.getUsername());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new PasswordMismatchException("Le password non coincidono");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Ruolo ROLE_USER non trovato"));

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(user);
    }
}
