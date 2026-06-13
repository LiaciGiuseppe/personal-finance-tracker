package com.portfolio.financetracker.service;

import com.portfolio.financetracker.dto.ChangePasswordDto;
import com.portfolio.financetracker.dto.UserRegistrationDto;
import com.portfolio.financetracker.exception.PasswordMismatchException;
import com.portfolio.financetracker.exception.UsernameAlreadyExistsException;
import com.portfolio.financetracker.model.Role;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.RoleRepository;
import com.portfolio.financetracker.repository.TransactionRepository;
import com.portfolio.financetracker.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       TransactionRepository transactionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.transactionRepository = transactionRepository;
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

    public void changePassword(String username, ChangePasswordDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La password corrente non è corretta");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Le nuove password non coincidono");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String username, String currentPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("La password non è corretta");
        }

        transactionRepository.deleteByUser(user);
        userRepository.delete(user);
    }
}
