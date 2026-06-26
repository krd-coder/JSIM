package duzeZadanie.kolejkaZdarzen;

import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import java.util.PriorityQueue;

public class BibliotecznaKolejkaZdarzen implements KolejkaZdarzen {

    /**
     * Prywatna klasa pomocnicza (rekordowa w logice), która opakowuje zdarzenie
     * wraz z unikalnym numerem porządkowym, aby zagwarantować stabilność.
     */
    private static class OpakowaneZdarzenie {
        final Zdarzenie zdarzenie;
        final long numerPorzadkowy;

        OpakowaneZdarzenie(Zdarzenie zdarzenie, long numerPorzadkowy) {
            this.zdarzenie = zdarzenie;
            this.numerPorzadkowy = numerPorzadkowy;
        }
    }

    private final PriorityQueue<OpakowaneZdarzenie> kolejka;
    private long licznikWstawien;

    public BibliotecznaKolejkaZdarzen() {
        this.licznikWstawien = 0;
        
        // Komparator zdefiniowany za pomocą lambdy.
        // Najpierw porównujemy czasy zdarzeń, a w przypadku remisu - kolejność wstawienia.
        this.kolejka = new PriorityQueue<>((a, b) -> {
            // Zakładam, że klasa Zdarzenie posiada metodę moment() zwracającą obiekt klasy Moment,
            // a klasa Moment poprawnie implementuje interfejs Comparable<Moment>.
            int porownanieCzasu = a.zdarzenie.moment().compareTo(b.zdarzenie.moment());
            
            if (porownanieCzasu != 0) {
                return porownanieCzasu;
            }
            
            // Jeśli czasy są równe, zdarzenie wstawione wcześniej (mniejszy numer) 
            // ma wyższy priorytet (musi wyjść z kolejki jako pierwsze).
            return Long.compare(a.numerPorzadkowy, b.numerPorzadkowy);
        });
    }

    @Override
    public void dodaj(Zdarzenie zdarzenie) {
        kolejka.add(new OpakowaneZdarzenie(zdarzenie, licznikWstawien++));
    }

    @Override
    public Zdarzenie zdejmij() {
        if (czyPusta()) {
            // Próba pobrania zdarzenia z pustej kolejki powinna być wychwytywana[cite: 414].
            throw new IllegalStateException("Próba pobrania zdarzenia z pustej kolejki!"); 
        }
        // Pobieramy (usuwając) korzeń kopca i zwracamy "odpakowane" z niego zdarzenie
        return kolejka.poll().zdarzenie;
    }

    @Override
    public boolean czyPusta() {
        return kolejka.isEmpty();
    }
}