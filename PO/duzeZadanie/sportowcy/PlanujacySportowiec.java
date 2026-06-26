package duzeZadanie.sportowcy;

import duzeZadanie.czas.Moment;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import java.util.LinkedList;
import java.util.Queue;

public abstract class PlanujacySportowiec extends Sportowiec {

    // Plan to po prostu kolejka kroków (wyciągów i tras) do wykonania.
    // Używamy Object jako placeholdera. W twoim kodzie może to być bazowy interfejs Krawedz.
    protected Queue<Object> obecnyPlan = new LinkedList<>();

    public PlanujacySportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci, 
                               double wspolczynnikTrudnosci, double wspolczynnikNawierzchni, 
                               double wspolczynnikZnudzenia, double wagaZnudzenia, boolean sledzony, 
                               Wezel wezelStartowy, Moment momentStartu, duzeZadanie.losowosc.MaszynaLosujaca maszynaLosujaca) {
        super(id, poziomZaawansowania, wspolczynnikSpontanicznosci, wspolczynnikTrudnosci, 
              wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia, sledzony, 
              wezelStartowy, momentStartu, maszynaLosujaca);
    }

    @Override
    public Zdarzenie nastepnyKrok(Moment moment, Wezel obecnyWezel) {
        // Jeśli nie mamy planu, wymyślamy nowy. Zmiana planu oraz spontaniczna 
        // decyzja są możliwe tylko gdy poprzedni plan został w całości zrealizowany.
        if (obecnyPlan.isEmpty()) {
            if (maszynaLosujaca.losowyDouble(0, 1) < wspolczynnikSpontanicznosci) {
                return podejmijSpontanicznaDecyzje(moment, obecnyWezel); // Logika analogiczna do lokalnego
            } else {
                wygenerujNowyPlan(obecnyWezel);
            }
        }
        
        // Zdejmujemy pierwszy element z planu i realizujemy go
        Object kolejnyKrok = obecnyPlan.poll();
        if (kolejnyKrok instanceof Trasa) {
            return nastepnyKrokTrasa(moment, (Trasa) kolejnyKrok);
        } else {
            return nastepnyKrokWyciag(moment, (Wyciag) kolejnyKrok);
        }
    }

    /**
     * Abstrakcyjna metoda zlecająca algorytmowi BFS ułożenie planu (ścieżki krawędzi) 
     * i zapisanie go w kolejce `obecnyPlan`. Różni się celem dla Z i K.
     */
    protected abstract void wygenerujNowyPlan(Wezel obecnyWezel);

    // Spontaniczna decyzja implementowana z reguły identycznie jak w klasie LokalnySportowiec
    private Zdarzenie podejmijSpontanicznaDecyzje(Moment moment, Wezel obecnyWezel) {
        // (Wklej tutaj logikę podejmijSpontanicznaDecyzje() z klasy LokalnySportowiec)
        return null; 
    }
}