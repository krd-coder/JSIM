package duzeZadanie.osrodek.krawedz.wyciag;

import java.util.Arrays;

import duzeZadanie.sportowcy.Sportowiec;

/**
 * Implementuje intefejs kolejki sportowców przy pomocy bufora cyklicznego.
 * Podwaja rozmiar bufora jeśli jest to konieczne.
 * Wszystkie operacje działają w czasie amortyzowanym O(1).
 */
public class BuforCyklicznySportowcow implements KolejkaSportowcow {

    private Sportowiec[] sportowcy;

    private int poczatek;

    private int rozmiar;

    public BuforCyklicznySportowcow() {
        sportowcy = new Sportowiec[1];
        poczatek = rozmiar = 0;
    }

    @Override
    public void dodaj(Sportowiec sportowiec) {
        if (rozmiar == sportowcy.length) {
            podwojRozmiar();
        }

        sportowcy[(poczatek + rozmiar) % sportowcy.length] = sportowiec;
        rozmiar++;
    }

    private void podwojRozmiar() {
        Sportowiec[] nowiSportowcy = new Sportowiec[2 * sportowcy.length];

        for (int i = 0; i < rozmiar; i++) {
            nowiSportowcy[i] = sportowcy[(poczatek + i) % sportowcy.length];
        }

        sportowcy = nowiSportowcy;
        poczatek = 0;
    }

    @Override
    public Sportowiec[] zdejmij(int ile) {
        assert ile <= rozmiar : String.format("Nie można zdjąć %d sportowców, kolejka ma rozmiar %d", ile, rozmiar);

        Sportowiec[] doZdjecia = new Sportowiec[ile];

        for (int i = 0; i < ile; i++) {
            doZdjecia[i] = sportowcy[(poczatek + i) % sportowcy.length];
        }

        poczatek = (poczatek + ile) % sportowcy.length;
        rozmiar -= ile;

        return doZdjecia;
    }

    @Override
    public int rozmiar() {
        return rozmiar;
    }

    @Override
    public String toString() {
        return "BuforCyklicznySportowcow [sportowcy=" + Arrays.toString(sportowcy) + "]";
    }
}
