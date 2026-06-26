package duzeZadanie.sportowcy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Krawedz;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

import java.util.List;

public class LokalnySportowiec extends Sportowiec {

    public LokalnySportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci,
                             double wspolczynnikTrudnosci, double wspolczynnikNawierzchni,
                             double wspolczynnikZnudzenia, double wagaZnudzenia, boolean sledzony,
                             Wezel wezelStartowy, Moment momentStartu, MaszynaLosujaca maszynaLosujaca) {
        super(id, poziomZaawansowania, wspolczynnikSpontanicznosci, wspolczynnikTrudnosci,
              wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia, sledzony,
              wezelStartowy, momentStartu, maszynaLosujaca);
    }

    @Override
    public Zdarzenie nastepnyKrok(Moment moment, Wezel obecnyWezel) {
        // Sprawdzanie decyzji spontanicznej
        if (maszynaLosujaca.losujPrawdopodobienstwo() < wspolczynnikSpontanicznosci) {
            List<Krawedz> wszystkieKrawedzie = obecnyWezel.pobierzWszystkieWychodzace();
            int wylosowanyIndeks = maszynaLosujaca.losujCalkowita(0, wszystkieKrawedzie.size() - 1);
            Krawedz losowa = wszystkieKrawedzie.get(wylosowanyIndeks);
            
            if (losowa instanceof Trasa) return nastepnyKrokTrasa(moment, (Trasa) losowa);
            return nastepnyKrokWyciag(moment, (Wyciag) losowa);
        }

        Krawedz najlepszyWybor = null;
        double maxAtrakcyjnosc = -1.0;

        // Trasy bezpośrednio z węzła
        for (Trasa trasa : obecnyWezel.pobierzTrasy()) {
            double atrakcyjnosc = lacznaAtrakcyjnosc(trasa);
            if (atrakcyjnosc > maxAtrakcyjnosc) {
                maxAtrakcyjnosc = atrakcyjnosc;
                najlepszyWybor = trasa;
            }
        }

        // Trasy dostępne z wyciągów
        for (Wyciag wyciag : obecnyWezel.pobierzWyciagi()) {
            Wezel gornaStacja = wyciag.pobierzKoniec();
            for (Trasa trasaNaGorze : gornaStacja.pobierzTrasy()) {
                double atrakcyjnosc = lacznaAtrakcyjnosc(trasaNaGorze);
                if (atrakcyjnosc > maxAtrakcyjnosc) {
                    maxAtrakcyjnosc = atrakcyjnosc;
                    najlepszyWybor = wyciag; // Decydujemy się na wjazd
                }
            }
        }

        if (najlepszyWybor instanceof Trasa) {
            return nastepnyKrokTrasa(moment, (Trasa) najlepszyWybor);
        } else {
            return nastepnyKrokWyciag(moment, (Wyciag) najlepszyWybor);
        }
    }

    @Override
    public Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu) {
        return new LokalnySportowiec(this.id + przesuniecieId, poziomZaawansowania, wspolczynnikSpontanicznosci,
                wspolczynnikTrudnosci, wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia,
                sledzony, wezelStartowy, momentStartu.dodaj(przesuniecieMomentuStartu), maszynaLosujaca);
    }
}