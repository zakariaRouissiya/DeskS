package com.symolia.DeskS.config;

import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class SuperAdminConfig {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initSuperAdmin() {
        return args -> {
            if (utilisateurRepository.findByEmail("admin@servicedesk.com").isEmpty()) {
                Utilisateur superAdmin = new Utilisateur();
                superAdmin.setNom("Admin");
                superAdmin.setPrenom("Super");
                superAdmin.setEmail("admin@servicedesk.com");
                superAdmin.setMotDePasse(passwordEncoder.encode("Admin123@"));
                superAdmin.setRole(Role.ADMINISTRATEUR);
                superAdmin.setDateInscription(LocalDateTime.now());
                utilisateurRepository.save(superAdmin);
            }
        };
    }
}