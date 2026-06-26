package duzeZadanie.sportowcy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

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
        if (maszynaLosujaca.losowyDouble(0, 1) < wspolczynnikSpontanicznosci) {
            return podejmijSpontanicznaDecyzje(moment, obecnyWezel);
        } else {
            return podejmijPrzemyslanaDecyzje(moment, obecnyWezel);
        }
    }

    private Zdarzenie podejmijSpontanicznaDecyzje(Moment moment, Wezel obecnyWezel) {
        Trasa[] bezposrednieTrasy = obecnyWezel.wychodzaceTrasy();
        Wyciag[] wyciagi = obecnyWezel.wychodzaceWyciagi();
        int losowyWybor = maszynaLosujaca.losowyInt(0, bezposrednieTrasy.length + wyciagi.length);

        if (losowyWybor < bezposrednieTrasy.length) {
            return nastepnyKrokTrasa(moment, bezposrednieTrasy[losowyWybor]);
        } else {
            return nastepnyKrokWyciag(moment, wyciagi[losowyWybor - bezposrednieTrasy.length]);
        }
    }

    private Zdarzenie podejmijPrzemyslanaDecyzje(Moment moment, Wezel obecnyWezel) {
        Trasa[] bezposrednieTrasy = obecnyWezel.wychodzaceTrasy();
        Wyciag[] wyciagi = obecnyWezel.wychodzaceWyciagi();

        double najwiekszaAtrakcyjnosc = -1;
        Trasa najlepszaTrasa = null;
        Wyciag nastepnyWyciag = null;

        for (Trasa trasa : bezposrednieTrasy) {
            double atrakcyjnosc = lacznaAtrakcyjnosc(trasa);
            if (atrakcyjnosc > najwiekszaAtrakcyjnosc) {
                najwiekszaAtrakcyjnosc = atrakcyjnosc;
                najlepszaTrasa = trasa;
                nastepnyWyciag = null; // Resetujemy wyciąg, jeśli wybrano trasę z obecnego węzła
            }
        }

        for (Wyciag wyciag : wyciagi) {
            for (Trasa trasa : wyciag.koniec().wychodzaceTrasy()) {
                double atrakcyjnosc = lacznaAtrakcyjnosc(trasa);
                if (atrakcyjnosc > najwiekszaAtrakcyjnosc) {
                    najwiekszaAtrakcyjnosc = atrakcyjnosc;
                    najlepszaTrasa = trasa;
                    nastepnyWyciag = wyciag;
                }
            }
        }

        if (najlepszaTrasa == null) {
            return nastepnyKrokWyciag(moment, wyciagi[0]);
        } else if (nastepnyWyciag == null) {
            return nastepnyKrokTrasa(moment, najlepszaTrasa);
        } else {
            return nastepnyKrokWyciag(moment, nastepnyWyciag);
        }
    }

    @Override
    public Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu) {
        return new LokalnySportowiec(id + przesuniecieId, poziomZaawansowania, wspolczynnikSpontanicznosci,
                wspolczynnikTrudnosci, wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia,
                sledzony, wezelStartowy, momentStartu.dodajInterwal(przesuniecieMomentuStartu), maszynaLosujaca);
    }
}