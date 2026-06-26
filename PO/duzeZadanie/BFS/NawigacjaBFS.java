package duzeZadanie.BFS;

import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.osrodek.krawedz.Krawedz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList; // Wbudowana lista
import java.util.List;
import java.util.Map;
import java.util.Queue; // Wbudowany interfejs kolejki

public class NawigacjaBFS {
    private final Osrodek osrodek;

    public NawigacjaBFS(Osrodek osrodek) {
        this.osrodek = osrodek;
    }

    /**
     * Struktura pomocnicza przechowująca informacje potrzebne do odtworzenia ścieżki.
     */
    private static class InfoBFS {
        final Krawedz krawedzWchodzaca; // Może to być Trasa lub Wyciag
        final Wezel poprzednik;
        final int odleglosc;

        InfoBFS(Krawedz krawedzWchodzaca, Wezel poprzednik, int odleglosc) {
            this.krawedzWchodzaca = krawedzWchodzaca;
            this.poprzednik = poprzednik;
            this.odleglosc = odleglosc;
        }
    }

    /**
     * Główna metoda wyznaczająca najkrótszą ścieżkę algorytmem BFS.
     * Zwraca listę krawędzi (Tras i Wyciągów), które należy pokonać, by dotrzeć do celu.
     */
    public static List<Krawedz> WyznaczPlan(Wezel start, Wezel cel) {
        // Mapa przechowująca węzły, które już odwiedziliśmy, wraz z historią
        Map<Wezel, InfoBFS> odwiedzone = new HashMap<>();
        
        Queue<Wezel> kolejka = new LinkedList<>();

        // Inicjalizacja dla wierzchołka startowego
        odwiedzone.put(start, new InfoBFS(null, null, 0));
        kolejka.add(start); // Metoda add() wstawia na koniec kolejki

        boolean znaleziono = false;

        // 1. Przeszukiwanie grafu (BFS)
        while (!kolejka.isEmpty()) {
            // Pobieramy i automatycznie usuwamy element z początku kolejki
            Wezel obecny = kolejka.poll(); 

            // Jeśli dotarliśmy do celu, przerywamy wyszukiwanie
            if (obecny == cel) {
                znaleziono = true;
                break;
            }

            int odlegloscObecnego = odwiedzone.get(obecny).odleglosc;

            // Analizujemy wszystkie trasy wychodzące z obecnego wierzchołka
            for (Trasa trasa : obecny.wychodzaceTrasy()) {
                Wezel sasiad = trasa.koniec(); 
                
                // Odwiedzamy tylko te węzły, w których jeszcze nie byliśmy
                if (!odwiedzone.containsKey(sasiad)) {
                    odwiedzone.put(sasiad, new InfoBFS(trasa, obecny, odlegloscObecnego + 1));
                    kolejka.add(sasiad);
                }
            }

            // Analizujemy wszystkie wyciągi wychodzące z obecnego wierzchołka
            for (Wyciag wyciag : obecny.wychodzaceWyciagi()) {
                Wezel sasiad = wyciag.koniec();
                
                if (!odwiedzone.containsKey(sasiad)) {
                    odwiedzone.put(sasiad, new InfoBFS(wyciag, obecny, odlegloscObecnego + 1));
                    kolejka.add(sasiad);
                }
            }
        }

        // 2. Odtwarzanie wyznaczonej ścieżki
        List<Krawedz> plan = new ArrayList<>();
        
        // Zabezpieczenie przed brakiem ścieżki (choć graf w zadaniu jest silnie spójny)
        if (!znaleziono && start != cel) {
            return plan; 
        }

        Wezel kursor = cel;
        
        // Cofamy się od celu do startu po zapamiętanych krawędziach
        while (kursor != start) {
            InfoBFS info = odwiedzone.get(kursor);
            plan.add(info.krawedzWchodzaca);
            kursor = info.poprzednik;
        }

        // Ścieżka została odtworzona od końca, więc musimy ją odwrócić
        Collections.reverse(plan);

        return plan;
    }

    public int obliczOdleglosc(Wezel start, Wezel cel) {
        List<Krawedz> plan = WyznaczPlan(start, cel);
        return plan.size(); // Liczba krawędzi w planie to odległość
    }
}