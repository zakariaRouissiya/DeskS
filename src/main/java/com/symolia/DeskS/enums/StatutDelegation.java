package com.symolia.DeskS.enums;

public enum StatutDelegation {
    EN_ATTENTE("En attente"),
    APPROUVE("Approuvé"),
    REFUSE("Refusé");
    
    private final String libelle;
    
    StatutDelegation(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
}