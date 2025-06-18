package com.symolia.DeskS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.symolia.DeskS.entities.Departement;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.services.DepartementService;
import com.symolia.DeskS.services.UtilisateurService;
import com.symolia.DeskS.web.AdminController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private DepartementService departementService;

    @Autowired
    private ObjectMapper objectMapper;


    @Nested
    @DisplayName("Utilisateur CRUD")
    class UtilisateurCrud {

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void creerUtilisateur_success() throws Exception {
            Utilisateur user = new Utilisateur();
            user.setId(1L);
            user.setEmail("test@servicedesk.com");
            user.setMotDePasse("password");
            user.setNom("Test");
            user.setPrenom("User");
            user.setRole(Role.EMPLOYEE);

            when(utilisateurService.createUtilisateur(any(Utilisateur.class), eq(1L))).thenReturn(user);

            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "test@servicedesk.com");
            payload.put("motDePasse", "password");
            payload.put("nom", "Test");
            payload.put("prenom", "User");
            payload.put("role", "EMPLOYEE");
            payload.put("departmentId", 1);

            mockMvc.perform(post("/api/admin/creer-utilisateur")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Utilisateur créé avec succès"));

            verify(utilisateurService, times(1)).createUtilisateur(any(Utilisateur.class), eq(1L));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void creerUtilisateur_invalidData() throws Exception {
            doThrow(new IllegalArgumentException("Données invalides"))
                    .when(utilisateurService).createUtilisateur(any(), anyLong());

            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "");
            payload.put("motDePasse", "");
            payload.put("nom", "");
            payload.put("prenom", "");
            payload.put("role", "");
            payload.put("departmentId", "");

            mockMvc.perform(post("/api/admin/creer-utilisateur")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());

            verify(utilisateurService, times(1)).createUtilisateur(any(), anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void consulterUtilisateurs_success() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Utilisateur user = new Utilisateur();
            user.setId(1L);
            user.setEmail("test@servicedesk.com");
            user.setMotDePasse("password");
            user.setNom("Test");
            user.setPrenom("User");
            user.setRole(Role.EMPLOYEE);
            user.setDateInscription(LocalDateTime.now());

            List<Utilisateur> utilisateurs = Collections.singletonList(user);
            Page<Utilisateur> userPage = new PageImpl<>(utilisateurs, pageable, 1);

            when(utilisateurService.getAllUtilisateurs(eq(pageable), any(), any(), any(), any(), any()))
                    .thenReturn(userPage);

            mockMvc.perform(get("/api/admin/utilisateurs")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.utilisateurs[0].id").value(1))
                    .andExpect(jsonPath("$.utilisateurs[0].email").value("test@servicedesk.com"))
                    .andExpect(jsonPath("$.utilisateurs[0].nom").value("Test"))
                    .andExpect(jsonPath("$.utilisateurs[0].prenom").value("User"))
                    .andExpect(jsonPath("$.utilisateurs[0].role").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0));

            verify(utilisateurService, times(1)).getAllUtilisateurs(eq(pageable), any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void modifierUtilisateur_success() throws Exception {
            Utilisateur user = new Utilisateur();
            user.setId(1L);

            when(utilisateurService.updateUtilisateur(eq(1L), any(Utilisateur.class), eq(1L))).thenReturn(user);

            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "test@servicedesk.com");
            payload.put("motDePasse", "newpassword");
            payload.put("nom", "Test Updated");
            payload.put("prenom", "User Updated");
            payload.put("role", "MANAGER");
            payload.put("departmentId", 1);

            mockMvc.perform(put("/api/admin/utilisateur/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Utilisateur modifié avec succès"));

            verify(utilisateurService, times(1)).updateUtilisateur(eq(1L), any(Utilisateur.class), eq(1L));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void modifierUtilisateur_notFound() throws Exception {
            doThrow(new IllegalArgumentException("Utilisateur non trouvé"))
                    .when(utilisateurService).updateUtilisateur(eq(1L), any(), anyLong());

            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "test@servicedesk.com");
            payload.put("motDePasse", "newpassword");
            payload.put("nom", "Test Updated");
            payload.put("prenom", "User Updated");
            payload.put("role", "MANAGER");
            payload.put("departmentId", 1);

            mockMvc.perform(put("/api/admin/utilisateur/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());

            verify(utilisateurService, times(1)).updateUtilisateur(eq(1L), any(), anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void supprimerUtilisateur_success() throws Exception {
            doNothing().when(utilisateurService).deleteUtilisateur(1L);

            mockMvc.perform(delete("/api/admin/utilisateur/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Utilisateur supprimé avec succès"));

            verify(utilisateurService, times(1)).deleteUtilisateur(1L);
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void supprimerUtilisateur_notFound() throws Exception {
            doThrow(new IllegalArgumentException("Utilisateur non trouvé"))
                    .when(utilisateurService).deleteUtilisateur(1L);

            mockMvc.perform(delete("/api/admin/utilisateur/1"))
                    .andExpect(status().isBadRequest());

            verify(utilisateurService, times(1)).deleteUtilisateur(1L);
        }
    }


    @Nested
    @DisplayName("Departement CRUD")
    class DepartementCrud {

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void creerDepartement_success() throws Exception {
            Departement dept = new Departement();
            dept.setId(1L);
            dept.setNom("IT");
            dept.setDescription("IT Department");

            when(departementService.createDepartement(any(Departement.class))).thenReturn(dept);

            Map<String, Object> payload = new HashMap<>();
            payload.put("nom", "IT");
            payload.put("description", "IT Department");

            mockMvc.perform(post("/api/admin/creer-departement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Département créé avec succès"));

            verify(departementService, times(1)).createDepartement(any(Departement.class));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void creerDepartement_invalidData() throws Exception {
            doThrow(new IllegalArgumentException("Données invalides"))
                    .when(departementService).createDepartement(any());

            Map<String, Object> payload = new HashMap<>();
            payload.put("nom", "");
            payload.put("description", "");

            mockMvc.perform(post("/api/admin/creer-departement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());

            verify(departementService, times(1)).createDepartement(any());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void consulterDepartements_success() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Departement dept = new Departement();
            dept.setId(1L);
            dept.setNom("IT");
            dept.setDescription("IT Department");

            List<Departement> departements = Collections.singletonList(dept);
            Page<Departement> deptPage = new PageImpl<>(departements, pageable, 1);

            when(departementService.getAllDepartements(eq(pageable), anyString())).thenReturn(deptPage);

            mockMvc.perform(get("/api/admin/departements")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.departements[0].id").value(1))
                    .andExpect(jsonPath("$.departements[0].nom").value("IT"))
                    .andExpect(jsonPath("$.departements[0].description").value("IT Department"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0));

            verify(departementService, times(1)).getAllDepartements(eq(pageable), anyString());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void consulterDepartements_withFilter() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Departement dept = new Departement();
            dept.setId(1L);
            dept.setNom("IT");
            dept.setDescription("IT Department");

            List<Departement> departements = Collections.singletonList(dept);
            Page<Departement> deptPage = new PageImpl<>(departements, pageable, 1);

            when(departementService.getAllDepartements(eq(pageable), eq("IT"))).thenReturn(deptPage);

            mockMvc.perform(get("/api/admin/departements")
                            .param("page", "0")
                            .param("size", "10")
                            .param("nom", "IT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.departements[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(departementService, times(1)).getAllDepartements(eq(pageable), eq("IT"));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void modifierDepartement_success() throws Exception {
            Departement dept = new Departement();
            dept.setId(1L);

            when(departementService.updateDepartement(eq(1L), any(Departement.class))).thenReturn(dept);

            Map<String, Object> payload = new HashMap<>();
            payload.put("nom", "IT Updated");
            payload.put("description", "Updated IT Department");

            mockMvc.perform(put("/api/admin/departement/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Département modifié avec succès"));

            verify(departementService, times(1)).updateDepartement(eq(1L), any(Departement.class));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void modifierDepartement_notFound() throws Exception {
            doThrow(new IllegalArgumentException("Département non trouvé"))
                    .when(departementService).updateDepartement(eq(1L), any());

            Map<String, Object> payload = new HashMap<>();
            payload.put("nom", "IT Updated");
            payload.put("description", "Updated IT Department");

            mockMvc.perform(put("/api/admin/departement/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());

            verify(departementService, times(1)).updateDepartement(eq(1L), any());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void supprimerDepartement_success() throws Exception {
            doNothing().when(departementService).deleteDepartement(1L);

            mockMvc.perform(delete("/api/admin/departement/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Département supprimé avec succès"));

            verify(departementService, times(1)).deleteDepartement(1L);
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATEUR")
        void supprimerDepartement_notFound() throws Exception {
            doThrow(new IllegalArgumentException("Département non trouvé"))
                    .when(departementService).deleteDepartement(1L);

            mockMvc.perform(delete("/api/admin/departement/1"))
                    .andExpect(status().isBadRequest());

            verify(departementService, times(1)).deleteDepartement(1L);
        }
    }
}