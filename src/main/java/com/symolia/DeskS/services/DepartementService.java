package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Departement;
import com.symolia.DeskS.repositories.DepartementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private static final Logger logger = LoggerFactory.getLogger(DepartementService.class);

    private final DepartementRepository departementRepository;

    public Departement createDepartement(Departement departement) {
        logger.info("Début de la création d'un département avec nom : {}", departement.getNom());
        validateDepartement(departement);
        Departement savedDepartement = departementRepository.save(departement);
        logger.info("Département créé avec succès, ID : {}", savedDepartement.getId());
        return savedDepartement;
    }

    public Page<Departement> getAllDepartements(Pageable pageable, String nom) {
        logger.debug("Récupération des départements avec filtre nom : {}", nom);
        Page<Departement> allDepartements = departementRepository.findAll(pageable);

        List<Departement> filteredDepartements;
        if (nom != null && !nom.isEmpty()) {
            filteredDepartements = allDepartements.getContent().stream()
                    .filter(d -> d.getNom() != null && d.getNom().toString().toLowerCase().contains(nom.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            filteredDepartements = allDepartements.getContent();
        }

        Page<Departement> result = new PageImpl<>(
                filteredDepartements,
                pageable,
                filteredDepartements.size()
        );
        logger.debug("Récupération réussie, total éléments : {}", result.getTotalElements());
        return result;
    }

    public Optional<Departement> getDepartementById(Long id) {
        logger.debug("Recherche d'un département par ID : {}", id);
        Optional<Departement> departement = departementRepository.findById(id);
        if (departement.isEmpty()) {
            logger.warn("Département non trouvé avec ID : {}", id);
        }
        return departement;
    }

    public Departement updateDepartement(Long id, Departement departement) {
        logger.info("Début de la mise à jour du département avec ID : {}", id);
        if (!departementRepository.existsById(id)) {
            logger.error("Département non trouvé avec ID : {}", id);
            throw new IllegalArgumentException("Département non trouvé avec ID : " + id);
        }
        validateDepartement(departement);
        departement.setId(id);
        Departement updatedDepartement = departementRepository.save(departement);
        logger.info("Département mis à jour avec succès, ID : {}", updatedDepartement.getId());
        return updatedDepartement;
    }

    public void deleteDepartement(Long id) {
        logger.info("Début de la suppression du département avec ID : {}", id);
        if (!departementRepository.existsById(id)) {
            logger.error("Département non trouvé avec ID : {}", id);
            throw new IllegalArgumentException("Département non trouvé avec ID : " + id);
        }
        departementRepository.deleteById(id);
        logger.info("Département supprimé avec succès, ID : {}", id);
    }

    private void validateDepartement(Departement departement) {
        logger.debug("Validation du département avec nom : {}", departement.getNom());
        if (departement.getNom() == null) {
            logger.error("Validation échouée : nom du département manquant");
            throw new IllegalArgumentException("Le nom du département est requis");
        }
        logger.debug("Validation réussie pour le département avec nom : {}", departement.getNom());
    }
}