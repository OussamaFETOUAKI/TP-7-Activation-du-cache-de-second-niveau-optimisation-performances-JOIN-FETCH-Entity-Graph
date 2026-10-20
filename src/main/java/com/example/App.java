package com.example;

import com.example.model.Auteur;
import com.example.model.Livre;
import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        System.out.println("========================================================================================");
        System.out.println(" TP 7 : CACHE DE SECOND NIVEAU, OPTIMISATION DES PERFORMANCES & SUPPRESSION DU N+1      ");
        System.out.println("========================================================================================");
        System.out.println();

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        EntityManager em = emf.createEntityManager();

        DataInitService dataInitService = new DataInitService();
        PerformanceTestService performanceTestService = new PerformanceTestService();

        dataInitService.em = em;
        performanceTestService.em = em;

        // Étape 6 : Initialisation des données de test
        System.out.println(">>> 1. INITIALISATION DES DONNEES (DataInitService) <<<");
        em.getTransaction().begin();
        dataInitService.initData();
        em.getTransaction().commit();
        System.out.println();

        // Étape 7 : Test de performance sans cache (mise en évidence du problème N+1)
        System.out.println(">>> 2. MESURE DES PERFORMANCES AVANT OPTIMISATION (PROBLEME N+1) <<<");
        System.out.println("Observation attendue : 1 requête SQL pour les auteurs + 10 requêtes SQL pour les livres (LAZY).");
        em.clear(); // Vider le cache de premier niveau L1 pour observer les requêtes réelles
        performanceTestService.testPerformanceSansCache();
        System.out.println();

        // Étape 8 : Suppression du problème N+1 avec JOIN FETCH
        System.out.println(">>> 3. SUPPRESSION DU PROBLEME N+1 AVEC JOIN FETCH <<<");
        System.out.println("Observation attendue : 1 SEULE requête SQL avec jointure EAGER pour charger auteurs et livres.");
        em.clear(); // Vider le cache L1
        performanceTestService.testPerformanceAvecJoinFetch();
        System.out.println();

        // Étape 9 : Utilisation des Entity Graphs pour optimiser les requêtes
        System.out.println(">>> 4. OPTIMISATION DECLARATIVE AVEC ENTITY GRAPH (Auteur.livres) <<<");
        System.out.println("Observation attendue : Chargement optimisé sans modification du JPQL.");
        em.clear(); // Vider le cache L1
        performanceTestService.testPerformanceAvecEntityGraph();
        System.out.println();

        // Étape 10 : Activation et test du cache de second niveau
        System.out.println(">>> 5. VALIDATION DU CACHE DE SECOND NIVEAU (L2 CACHE AVEC EHCACHE) <<<");
        em.clear();
        performanceTestService.testPerformanceAvecCache();
        System.out.println();

        // Démonstration multi-sessions (fermeture de em1 et ouverture de em2)
        System.out.println(">>> 6. DEMONSTRATION DU CACHE L2 ENTRE DEUX SESSIONS DISTINCTES <<<");
        em.close(); // Fermeture définitive du 1er EntityManager (Cache L1 détruit)

        EntityManager em2 = emf.createEntityManager();
        System.out.println("-> Ouverture d'un second EntityManager (em2 - Cache L1 vide)");
        System.out.println("-> Recherche de l'Auteur ID=1 via em2.find(Auteur.class, 1L) :");

        long tStart = System.currentTimeMillis();
        Auteur auteurFromL2 = em2.find(Auteur.class, 1L);
        long tEnd = System.currentTimeMillis();

        System.out.println("   Entité récupérée : " + auteurFromL2.getNom() + " (en " + (tEnd - tStart) + " ms)");
        System.out.println("   --> AUCUNE requête SQL SELECT exécutée car l'entité est servie directement par le Cache L2 !");
        System.out.println();

        // Affichage des statistiques Hibernate
        try {
            SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
            Statistics stats = sessionFactory.getStatistics();
            if (stats != null && stats.isStatisticsEnabled()) {
                System.out.println("========================================================================================");
                System.out.println(" STATISTIQUES DU CACHE DE SECOND NIVEAU HIBERNATE (EHCACHE)                             ");
                System.out.println("========================================================================================");
                System.out.println(" - Nombre total de mises en cache L2 (Put Count) : " + stats.getSecondLevelCachePutCount());
                System.out.println(" - Nombre d'accès réussis au cache L2 (Hit Count) : " + stats.getSecondLevelCacheHitCount());
                System.out.println(" - Nombre de défauts de cache L2 (Miss Count)    : " + stats.getSecondLevelCacheMissCount());
                System.out.println(" - Nombre de requêtes SQL exécutées au total     : " + stats.getQueryExecutionCount());
                System.out.println("========================================================================================");
            }
        } catch (Exception ignored) {
        }

        em2.close();
        emf.close();

        System.out.println();
        System.out.println("========================================================================================");
        System.out.println(" TP 7 TERMINE AVEC SUCCES : PERFORMANCES OPTIMISEES & CACHE L2 OPERATIONNEL !           ");
        System.out.println("========================================================================================");
    }
}
