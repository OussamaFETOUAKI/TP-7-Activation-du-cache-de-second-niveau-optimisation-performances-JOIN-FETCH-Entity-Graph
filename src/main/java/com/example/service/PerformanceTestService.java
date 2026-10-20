package com.example.service;

import com.example.model.Auteur;
import com.example.model.Livre;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class PerformanceTestService {

    @PersistenceContext
    public EntityManager em;

    /**
     * Étape 7 : Mesure des performances SANS optimisation (Problème N+1)
     * 1 requête pour charger les auteurs + N requêtes (10) pour charger les livres en LAZY.
     */
    public void testPerformanceSansCache() {
        System.out.println("--- [1] Début du test SANS optimisation (Problème N+1) ---");
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        int totalLivres = 0;
        // Accès aux livres pour provoquer le chargement LAZY (N requêtes supplémentaires)
        for (Auteur auteur : auteurs) {
            totalLivres += auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        long duree = (end - start);
        System.out.println("Résultat : " + auteurs.size() + " auteurs et " + totalLivres + " livres chargés.");
        System.out.println("Durée sans cache (avec requêtes N+1) : " + duree + " ms");
    }

    /**
     * Étape 8 : Suppression du problème N+1 avec JOIN FETCH
     * Une seule requête SQL récupère les auteurs et leurs livres en une jointure EAGER.
     */
    public void testPerformanceAvecJoinFetch() {
        System.out.println("--- [2] Début du test avec JOIN FETCH (Suppression N+1) ---");
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery(
                "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        int totalLivres = 0;
        // L'accès aux livres ne déclenche plus de requêtes supplémentaires
        for (Auteur auteur : auteurs) {
            totalLivres += auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        long duree = (end - start);
        System.out.println("Résultat : " + auteurs.size() + " auteurs et " + totalLivres + " livres chargés.");
        System.out.println("Durée avec JOIN FETCH : " + duree + " ms");
    }

    /**
     * Étape 9 : Utilisation des Entity Graphs pour optimiser les requêtes
     * Déclaration déclarative sans modification du JPQL.
     */
    public void testPerformanceAvecEntityGraph() {
        System.out.println("--- [3] Début du test avec Entity Graph (Auteur.livres) ---");
        long start = System.currentTimeMillis();

        EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");

        TypedQuery<Auteur> query = em.createQuery("SELECT DISTINCT a FROM Auteur a", Auteur.class);
        query.setHint("javax.persistence.fetchgraph", graph);
        List<Auteur> auteurs = query.getResultList();

        int totalLivres = 0;
        for (Auteur auteur : auteurs) {
            totalLivres += auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        long duree = (end - start);
        System.out.println("Résultat : " + auteurs.size() + " auteurs et " + totalLivres + " livres chargés.");
        System.out.println("Durée avec Entity Graph : " + duree + " ms");
    }

    /**
     * Étape 10 : Activation et test du cache de second niveau
     * Première lecture : chargement et alimentation du cache de 2nd niveau.
     * Deuxième lecture : restitution directe depuis le cache L2 sans accès SQL.
     */
    public void testPerformanceAvecCache() {
        System.out.println("--- [4] Début du test avec Cache de Second Niveau (EhCache L2) ---");

        // 1ère lecture - chargement depuis la base
        System.out.println("-> Exécution 1ère lecture (alimentation de la base vers le cache) :");
        testPerformanceAvecJoinFetch();

        // 2ème lecture - chargement depuis le cache de second niveau
        System.out.println("-> Exécution 2ème lecture (lecture depuis le cache de second niveau) :");
        long start = System.currentTimeMillis();
        TypedQuery<Auteur> query = em.createQuery(
                "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        // Activation du cache de requête pour exploiter le L2 query cache
        query.setHint("org.hibernate.cacheable", true);
        List<Auteur> auteurs = query.getResultList();

        int totalLivres = 0;
        for (Auteur auteur : auteurs) {
            totalLivres += auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        long duree = (end - start);
        System.out.println("Résultat : " + auteurs.size() + " auteurs et " + totalLivres + " livres trouvés.");
        System.out.println("Durée avec cache (lecture 2) : " + duree + " ms");
    }
}
