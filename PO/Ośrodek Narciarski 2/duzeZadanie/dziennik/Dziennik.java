package duzeZadanie.dziennik;

import duzeZadanie.czas.Moment;
import duzeZadanie.sportowcy.Sportowiec;

public interface Dziennik {

    /**
     * Dodaje wpis o zadanym momencie i treści.
     */
    void dodajWpis(Moment moment, String wpis);

    /**
     * Dodaje wpis o zadanym momencie, dotyczący danego sportowca.
     */
    void dodajWpisZeSportowcem(Moment moment, Sportowiec sportowiec, String coZrobil);

    /**
     * Dodaje wpis złożony z zadanej tabeli, używane do wypisania końcowych statystyk.
     */
    void dodajTabele(String[][] wiersze);
}
