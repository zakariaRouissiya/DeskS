package com.symolia.DeskS.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class DelegationRequestDTO {
    @NotNull(message = "L'ID du ticket est obligatoire")
    private Long ticketId;
    
    @NotNull(message = "L'ID du technicien destinataire est obligatoire")
    private Long toTechnicianId;
    
    @NotBlank(message = "La justification est obligatoire")
    @Size(min = 10, max = 1000, message = "La justification doit contenir entre 10 et 1000 caractères")
    private String justification;
}