package duzeZadanie.osrodek.krawedz.wyciag;

import duzeZadanie.sportowcy.Sportowiec;

/**
 * Reprezentuje kolejkę prostą sportowców do wyciągu.
 */
public interface KolejkaSportowcow {

    void dodaj(Sportowiec sportowiec, Moment moment);

    Sportowiec[] zdejmij(int ile);

    int rozmiar();
}
