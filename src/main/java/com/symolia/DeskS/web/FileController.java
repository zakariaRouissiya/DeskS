package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.PieceJointe;
import com.symolia.DeskS.repositories.PieceJointeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final PieceJointeRepository pieceJointeRepository;
    
    @GetMapping("/download/{pieceJointeId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long pieceJointeId) {
        try {
            logger.info("Téléchargement demandé pour la pièce jointe ID: {}", pieceJointeId);
            
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe introuvable"));
            
            String filename = extractFilenameFromUrl(pieceJointe.getUrl());
            Path filePath = Paths.get("uploads/attachments").resolve(filename).normalize();
            
            logger.info("Tentative de téléchargement du fichier: {}", filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                logger.error("Fichier non trouvé: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                logger.error("Fichier non lisible: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            String contentType = determineContentType(filePath);
            
            logger.info("Téléchargement réussi - Fichier: {}, Type: {}, Taille: {}", 
                pieceJointe.getNomDuFichier(), contentType, pieceJointe.getTaille());
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + pieceJointe.getNomDuFichier() + "\"")
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Erreur lors du téléchargement de la pièce jointe {}: ", pieceJointeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/preview/{pieceJointeId}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long pieceJointeId) {
        try {
            logger.info("Aperçu demandé pour la pièce jointe ID: {}", pieceJointeId);
            
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe introuvable"));
            
            String filename = extractFilenameFromUrl(pieceJointe.getUrl());
            Path filePath = Paths.get("uploads/attachments").resolve(filename).normalize();
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = determineContentType(filePath);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pieceJointe.getNomDuFichier() + "\"")
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Erreur lors de l'aperçu de la pièce jointe {}: ", pieceJointeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String extractFilenameFromUrl(String url) {
        if (url == null) return "";
        return url.substring(url.lastIndexOf("/") + 1);
    }
    
    private String determineContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}