package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {

    Optional<Departement> findByNomIgnoreCase(String nom);

    Page<Departement> findAll(Pageable pageable);
}