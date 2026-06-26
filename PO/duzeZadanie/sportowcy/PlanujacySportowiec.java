package duzeZadanie.sportowcy;

import duzeZadanie.czas.Moment;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Krawedz;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.BFS.NawigacjaBFS;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class PlanujacySportowiec extends Sportowiec {

    protected Queue<Krawedz> aktualnyPlan = new LinkedList<>();
    protected final Osrodek osrodek;
    public PlanujacySportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci,
                               double wspolczynnikTrudnosci, double wspolczynnikNawierzchni,
                               double wspolczynnikZnudzenia, double wagaZnudzenia, boolean sledzony,
                               Wezel wezelStartowy, Moment momentStartu, MaszynaLosujaca maszynaLosujaca, Osrodek osrodek) {
        super(id, poziomZaawansowania, wspolczynnikSpontanicznosci, wspolczynnikTrudnosci,
              wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia, sledzony,
              wezelStartowy, momentStartu, maszynaLosujaca);
        this.osrodek = osrodek;
    }

    @Override
    public Zdarzenie nastepnyKrok(Moment moment, Wezel obecnyWezel) {
        // Zmiana planu i decyzje spontaniczne tylko gdy plan jest pusty
        if (aktualnyPlan.isEmpty()) {
            
            // Decyzja spontaniczna
            if (maszynaLosujaca.losowyDouble(0, 1) < wspolczynnikSpontanicznosci) {
                Krawedz losowaKrawedz = wylosujKrawedzZWezla(obecnyWezel);
                return wygenerujZdarzenie(moment, losowaKrawedz);
            }

            // Ułożenie nowego planu
            Trasa cel = znajdzTraseDocelowa(obecnyWezel);
            List<Krawedz> trasaBFS = NawigacjaBFS.WyznaczPlan(obecnyWezel, cel.poczatek());
            
            aktualnyPlan.addAll(trasaBFS);
            aktualnyPlan.add(cel); // Dodajemy sam zjazd na koniec planu
        }

        Krawedz nastepnaKrawedz = aktualnyPlan.poll();
        return wygenerujZdarzenie(moment, nastepnaKrawedz);
    }

    private Zdarzenie wygenerujZdarzenie(Moment moment, Krawedz krawedz) {
        if (krawedz instanceof Trasa) {
            return nastepnyKrokTrasa(moment, (Trasa) krawedz);
        } else {
            return nastepnyKrokWyciag(moment, (Wyciag) krawedz);
        }
    }

    // Prosta metoda losująca dowolną krawędź z węzła (do implementacji własnej zależnie od Wezla)
    protected abstract Krawedz wylosujKrawedzZWezla(Wezel wezel);

    // Abstrakcyjna metoda do zaimplementowania przez Zachłannego i Kolekcjonera
    protected abstract Trasa znajdzTraseDocelowa(Wezel obecnyWezel);
}