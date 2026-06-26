package duzeZadanie.sportowcy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Krawedz;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.BFS.NawigacjaBFS;

import java.util.List;

public class KolekcjonerSportowiec extends PlanujacySportowiec {

    public KolekcjonerSportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci,
                                 double wspolczynnikTrudnosci, double wspolczynnikNawierzchni,
                                 double wspolczynnikZnudzenia, double wagaZnudzenia, boolean sledzony,
                                 Wezel wezelStartowy, Moment momentStartu, MaszynaLosujaca maszynaLosujaca, Osrodek osrodek) {
        super(id, poziomZaawansowania, wspolczynnikSpontanicznosci, wspolczynnikTrudnosci,
              wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia, sledzony,
              wezelStartowy, momentStartu, maszynaLosujaca, osrodek);
    }

    @Override
    protected Trasa znajdzTraseDocelowa(Wezel obecnyWezel) {
        Trasa wybranaTrasa = null;
        int minZjazdow = Integer.MAX_VALUE;
        int minOdleglosc = Integer.MAX_VALUE;
        double maxAtrakcyjnosc = -1.0;

        Trasa[] wszystkieTrasy = osrodek.trasy();

        for (Trasa trasa : wszystkieTrasy) {
            int liczbaZjazdow = liczbaPrzejazdow(trasa);
            
            if (liczbaZjazdow < minZjazdow) {
                wybranaTrasa = trasa;
                minZjazdow = liczbaZjazdow;
                minOdleglosc = NawigacjaBFS.WyznaczOdleglosc(obecnyWezel, trasa.poczatek());
                maxAtrakcyjnosc = lacznaAtrakcyjnosc(trasa);
            } else if (liczbaZjazdow == minZjazdow) {
                // Remisy rozstrzygane najpierw odległością BFS
                int odleglosc = NawigacjaBFS.WyznaczOdleglosc(obecnyWezel, trasa.poczatek());
                if (odleglosc < minOdleglosc) {
                    wybranaTrasa = trasa;
                    minOdleglosc = odleglosc;
                    maxAtrakcyjnosc = lacznaAtrakcyjnosc(trasa);
                } else if (odleglosc == minOdleglosc) {
                    // Remisy w odległości rozstrzygane atrakcyjnością
                    double atrakcyjnosc = lacznaAtrakcyjnosc(trasa);
                    if (atrakcyjnosc > maxAtrakcyjnosc) {
                        wybranaTrasa = trasa;
                        maxAtrakcyjnosc = atrakcyjnosc;
                    }
                }
            }
        }
        return wybranaTrasa;
    }

    @Override
    public Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu) {
        return new KolekcjonerSportowiec(this.id + przesuniecieId, poziomZaawansowania, wspolczynnikSpontanicznosci,
                wspolczynnikTrudnosci, wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia,
                sledzony, wezelStartowy, momentStartu.dodajInterwal(przesuniecieMomentuStartu), maszynaLosujaca, osrodek);
    }
}