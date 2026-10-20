package com.example.service;

import com.example.model.Auteur;
import com.example.model.Livre;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

public class DataInitService {

    @PersistenceContext
    public EntityManager em;

    public void initData() {
        EntityTransaction tx = em.getTransaction();
        boolean startedHere = false;
        if (!tx.isActive()) {
            tx.begin();
            startedHere = true;
        }

        System.out.println("-> Insertion de 10 auteurs avec 5 livres chacun (50 livres au total)...");
        for (int i = 1; i <= 10; i++) {
            Auteur auteur = new Auteur();
            auteur.setNom("Auteur " + i);

            for (int j = 1; j <= 5; j++) {
                Livre livre = new Livre();
                livre.setTitre("Livre " + j + " de l'auteur " + i);
                auteur.ajouterLivre(livre);
            }

            em.persist(auteur);
        }

        if (startedHere) {
            tx.commit();
        }
        System.out.println("-> Données de test initialisées avec succès !");
    }
}
