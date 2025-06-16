package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.PieceJointe;
import com.symolia.DeskS.repositories.PieceJointeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PieceJointeService {

    private final PieceJointeRepository pieceJointeRepository;

    public PieceJointe createPieceJointe(PieceJointe pieceJointe) {
        return pieceJointeRepository.save(pieceJointe);
    }

    public Optional<PieceJointe> getPieceJointeById(Long id) {
        return pieceJointeRepository.findById(id);
    }

    public PieceJointe updatePieceJointe(Long id, PieceJointe pieceJointe) {
        pieceJointe.setId(id);
        return pieceJointeRepository.save(pieceJointe);
    }

    public void deletePieceJointe(Long id) {
        pieceJointeRepository.deleteById(id);
    }
}