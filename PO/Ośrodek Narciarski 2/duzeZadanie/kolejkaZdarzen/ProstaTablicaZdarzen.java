package duzeZadanie.kolejkaZdarzen;

import java.util.Arrays;

import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;

/**
 * Implementacja kolejki priorytetowej zdarzeń przy pomocy prostej tablicy i balansowania.
 * Jeśli brakuje miejsca na dodanie nowego zdarzenia, tablica jest powiększana dwukrotnie.
 * Jeśli tablica jest zapełniona w <25% pojemności, jest docinana do obecnej liczby zdarzeń.
 * Operacje dodawania i zdejmowania zdarzeń działają w czasie liniowym O(rozmiar kolejki).
 */
public class ProstaTablicaZdarzen implements KolejkaZdarzen {

    private Zdarzenie[] zdarzenia = {null};

    private int ileZdarzen = 0;

    @Override
    public void dodaj(Zdarzenie zdarzenie) {
        if (ileZdarzen >= zdarzenia.length) {
            balansuj();
        }

        zdarzenia[ileZdarzen] = zdarzenie;
        ileZdarzen++;

        posortujRaz();
    }

    private void balansuj() {
        int nowyRomiar = zdarzenia.length;

        if (ileZdarzen + 1 > zdarzenia.length) {
            nowyRomiar = 2 * (ileZdarzen + 1);
        }

        if (ileZdarzen < zdarzenia.length / 4) {
            nowyRomiar = ileZdarzen + 1;
        }

        Zdarzenie[] noweZdarzenia = new Zdarzenie[nowyRomiar];

        for (int i = 0; i < ileZdarzen; i++) {
            noweZdarzenia[i] = zdarzenia[i];
        }

        zdarzenia = noweZdarzenia;
    }

    private void posortujRaz() {
        for (int i = ileZdarzen - 2; i >= 0; i--) {
            if (zdarzenia[i].moment().compareTo(zdarzenia[i + 1].moment()) > 0) {
                Zdarzenie tmp = zdarzenia[i];
                zdarzenia[i] = zdarzenia[i + 1];
                zdarzenia[i + 1] = tmp;
            }
        }
    }

    @Override
    public Zdarzenie zdejmij() {
        assert !czyPusta() : "Nie można zdjąć elementu z pustej kolejki";

        Zdarzenie zdarzenie = zdarzenia[0];

        for (int i = 1; i < ileZdarzen; i++) {
            zdarzenia[i - 1] = zdarzenia[i];
        }

        zdarzenia[ileZdarzen - 1] = null;
        ileZdarzen--;

        return zdarzenie;
    }

    @Override
    public boolean czyPusta() {
        return ileZdarzen == 0;
    }

    @Override
    public String toString() {
        return "ProstaTablicaZdarzen [zdarzenia=" + Arrays.toString(zdarzenia) + "]";
    }
}
