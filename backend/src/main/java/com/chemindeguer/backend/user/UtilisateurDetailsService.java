package com.chemindeguer.backend.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UtilisateurDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public UtilisateurDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Utilisateur> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .map(utilisateur -> User.builder()
                        .username(utilisateur.getEmail())
                        .password(utilisateur.getPassword())
                        .authorities("ROLE_" + utilisateur.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}