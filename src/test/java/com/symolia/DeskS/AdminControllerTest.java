package com.symolia.DeskS;

import com.symolia.DeskS.entities.Departement;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.services.DepartementService;
import com.symolia.DeskS.services.UtilisateurService;
import com.symolia.DeskS.web.AdminController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private DepartementService departementService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testCreerUtilisateur_Success() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", "test@servicedesk.com");
        payload.put("motDePasse", "password");
        payload.put("nom", "Test");
        payload.put("prenom", "User");
        payload.put("role", "EMPLOYEE");
        payload.put("departmentId", "1");

        mockMvc.perform(post("/api/admin/creer-utilisateur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@servicedesk.com\",\"motDePasse\":\"password\",\"nom\":\"Test\",\"prenom\":\"User\",\"role\":\"EMPLOYEE\",\"departmentId\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur créé avec succès"));

        verify(utilisateurService, times(1)).createUtilisateur(any(Utilisateur.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testCreerUtilisateur_InvalidData() throws Exception {
        doThrow(new IllegalArgumentException("Données invalides")).when(utilisateurService).createUtilisateur(any(), anyLong());

        mockMvc.perform(post("/api/admin/creer-utilisateur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"motDePasse\":\"\",\"nom\":\"\",\"prenom\":\"\",\"role\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(utilisateurService, times(1)).createUtilisateur(any(), anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testConsulterUtilisateurs_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<Utilisateur> utilisateurs = new ArrayList<>();
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("test@servicedesk.com");
        user.setMotDePasse("password");
        user.setNom("Test");
        user.setPrenom("User");
        user.setRole(Role.EMPLOYEE);
        user.setDepartment(null);
        user.setDateInscription(LocalDateTime.now());
        utilisateurs.add(user);
        Page<Utilisateur> userPage = new PageImpl<>(utilisateurs, pageable, utilisateurs.size());

        when(utilisateurService.getAllUtilisateurs(eq(pageable), isNull(), isNull(), isNull(), isNull(), isNull()))
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

        verify(utilisateurService, times(1)).getAllUtilisateurs(eq(pageable), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testConsulterUtilisateurs_WithFilter() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<Utilisateur> utilisateurs = new ArrayList<>();
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("test@servicedesk.com");
        user.setMotDePasse("password");
        user.setNom("Test");
        user.setPrenom("User");
        user.setRole(Role.EMPLOYEE);
        user.setDepartment(null);
        user.setDateInscription(LocalDateTime.now());
        utilisateurs.add(user);
        Page<Utilisateur> userPage = new PageImpl<>(utilisateurs, pageable, utilisateurs.size());

        when(utilisateurService.getAllUtilisateurs(eq(pageable), any(), anyLong(), eq("Test"), anyString(), anyString()))
                .thenReturn(userPage);

        mockMvc.perform(get("/api/admin/utilisateurs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("nom", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateurs[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(utilisateurService, times(1)).getAllUtilisateurs(eq(pageable), any(), anyLong(), eq("Test"), anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testModifierUtilisateur_Success() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", "test@servicedesk.com");
        payload.put("motDePasse", "newpassword");
        payload.put("nom", "Test Updated");
        payload.put("prenom", "User Updated");
        payload.put("role", "MANAGER");
        payload.put("departmentId", "1");

        mockMvc.perform(put("/api/admin/utilisateur/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@servicedesk.com\",\"motDePasse\":\"newpassword\",\"nom\":\"Test Updated\",\"prenom\":\"User Updated\",\"role\":\"MANAGER\",\"departmentId\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur modifié avec succès"));

        verify(utilisateurService, times(1)).updateUtilisateur(eq(1L), any(Utilisateur.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testModifierUtilisateur_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Utilisateur non trouvé")).when(utilisateurService).updateUtilisateur(eq(1L), any(), anyLong());

        mockMvc.perform(put("/api/admin/utilisateur/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@servicedesk.com\",\"motDePasse\":\"newpassword\",\"nom\":\"Test Updated\",\"prenom\":\"User Updated\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isBadRequest());

        verify(utilisateurService, times(1)).updateUtilisateur(eq(1L), any(), anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testSupprimerUtilisateur_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/utilisateur/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur supprimé avec succès"));

        verify(utilisateurService, times(1)).deleteUtilisateur(1L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testSupprimerUtilisateur_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Utilisateur non trouvé")).when(utilisateurService).deleteUtilisateur(1L);

        mockMvc.perform(delete("/api/admin/utilisateur/1"))
                .andExpect(status().isBadRequest());

        verify(utilisateurService, times(1)).deleteUtilisateur(1L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testCreerDepartement_Success() throws Exception {
        mockMvc.perform(post("/api/admin/creer-departement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"IT\",\"description\":\"IT Department\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Département créé avec succès"));

        verify(departementService, times(1)).createDepartement(any(Departement.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testCreerDepartement_InvalidData() throws Exception {
        doThrow(new IllegalArgumentException("Données invalides")).when(departementService).createDepartement(any());

        mockMvc.perform(post("/api/admin/creer-departement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(departementService, times(1)).createDepartement(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testConsulterDepartements_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<Departement> departements = new ArrayList<>();
        Departement dept = new Departement();
        dept.setId(1L);
        dept.setNom("IT");
        dept.setDescription("IT Department");
        departements.add(dept);
        Page<Departement> deptPage = new PageImpl<>(departements, pageable, departements.size());

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
    public void testConsulterDepartements_WithFilter() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<Departement> departements = new ArrayList<>();
        Departement dept = new Departement();
        dept.setId(1L);
        dept.setNom("IT");
        dept.setDescription("IT Department");
        departements.add(dept);
        Page<Departement> deptPage = new PageImpl<>(departements, pageable, departements.size());

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
    public void testModifierDepartement_Success() throws Exception {
        mockMvc.perform(put("/api/admin/departement/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"IT Updated\",\"description\":\"Updated IT Department\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Département modifié avec succès"));

        verify(departementService, times(1)).updateDepartement(eq(1L), any(Departement.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testModifierDepartement_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Département non trouvé")).when(departementService).updateDepartement(eq(1L), any());

        mockMvc.perform(put("/api/admin/departement/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"IT Updated\",\"description\":\"Updated IT Department\"}"))
                .andExpect(status().isBadRequest());

        verify(departementService, times(1)).updateDepartement(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testSupprimerDepartement_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/departement/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Département supprimé avec succès"));

        verify(departementService, times(1)).deleteDepartement(1L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void testSupprimerDepartement_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Département non trouvé")).when(departementService).deleteDepartement(1L);

        mockMvc.perform(delete("/api/admin/departement/1"))
                .andExpect(status().isBadRequest());

        verify(departementService, times(1)).deleteDepartement(1L);
    }
}