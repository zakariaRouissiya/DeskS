package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.PieceJointe;
import com.symolia.DeskS.repositories.PieceJointeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PieceJointeService {

    private final PieceJointeRepository pieceJointeRepository;

    public PieceJointe createPieceJointe(PieceJointe pieceJointe) {
        return pieceJointeRepository.save(pieceJointe);
    }
    public String saveFileAndGetUrl(MultipartFile file) throws IOException {
        String uploadDir = "uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.write(filePath, file.getBytes());
        return "/" + uploadDir + fileName;
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