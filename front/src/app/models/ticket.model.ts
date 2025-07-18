export enum Statut {
OUVERT = 'OUVERT',
EN_COURS = 'EN_COURS',
RESOLU = 'RESOLU',
VALIDE = 'VALIDE',
FERME = 'FERME'
}

export enum Priorite {
FAIBLE = 'FAIBLE',
MOYENNE = 'MOYENNE',
HAUTE = 'HAUTE',
CRITIQUE = 'CRITIQUE'
}

export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  department?: { id: number, nom: string };
  departement?: { id: number, nom: string };
}

export interface PieceJointe {
id?: number;
nomDuFichier: string;
typeDuFichier: string;
url: string;
}

export interface Commentaire {
id?: number;
commentaire: string;
dateCreation?: Date;
auteur: Utilisateur;
}

export interface Ticket {
id?: number;
titre: string;
description: string;
statut: Statut;
priorite: Priorite;
dateCreation?: Date;
tempsResolution?: string;
type: string;
user?: Utilisateur;
department?: any;
commentaires?: Commentaire[];
pieceJointe?: PieceJointe;
assignedTo?: Utilisateur;
delegatedTo?: Utilisateur;
}

export interface TicketFilters {
statut?: Statut;
priorite?: Priorite;
dateDebut?: Date;
dateFin?: Date;
search?: string;
userId?: number;
departmentId?: number;
assignedToId?: number;
}