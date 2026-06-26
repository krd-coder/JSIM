package duzeZadanie.osrodek.krawedz.wyciag;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.kolejkaZdarzen.zdarzenia.DotarcieDoWezla;
import duzeZadanie.kolejkaZdarzen.zdarzenia.OdjazdWyciagu;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Krawedz;
import duzeZadanie.sportowcy.Sportowiec;

import java.util.Locale;

public class Wyciag extends Krawedz {

    private final Interwal odstepMiedzyOdjazdami;
    private final int ladownosc;
    private final KolejkaSportowcow obecnaKolejka;

    // --- Statystyki ---
    private int lacznaLiczbaPasazerow;
    private int maksymalnaDlugoscKolejki;
    private int liczbaOdjazdow; // Potrzebne do wyliczenia maksymalnej przepustowości
    
    // Zmienne do liczenia średniej długości kolejki ważonej czasem
    private long sumaDlugosciKolejkiZCzasem;
    private Moment czasOstatniejOperacji;
    private Moment czasStartuSymulacji;

    public Wyciag(int id,
                  Wezel poczatek,
                  Wezel koniec,
                  Interwal odstepMiedzyOdjazdami,
                  Interwal dlugoscPrzejazdu,
                  int ladownosc) {
        super(id, poczatek, koniec, dlugoscPrzejazdu);

        assert poczatek.wysokosc() < koniec.wysokosc()
                : String.format("Wyciąg %d prowadzi w dół: %d -> %d", id, poczatek.wysokosc(), koniec.wysokosc());

        this.odstepMiedzyOdjazdami = odstepMiedzyOdjazdami;
        this.ladownosc = ladownosc;
        this.obecnaKolejka = new BuforCyklicznySportowcow();
        
        this.lacznaLiczbaPasazerow = 0;
        this.maksymalnaDlugoscKolejki = 0;
        this.liczbaOdjazdow = 0;
        
        this.sumaDlugosciKolejkiZCzasem = 0;
        this.czasOstatniejOperacji = null;
        this.czasStartuSymulacji = null;
    }

    /**
     * Zmieniono sygnaturę! Musimy przekazywać 'moment', 
     * aby poprawnie aktualizować statystyki czasowe kolejki.
     */
    public void dodajDoKolejki(Sportowiec sportowiec, Moment moment) {
        aktualizujStatystykiKolejki(moment);
        
        obecnaKolejka.dodaj(sportowiec);
        maksymalnaDlugoscKolejki = Math.max(maksymalnaDlugoscKolejki, obecnaKolejka.rozmiar());
    }

    public Zdarzenie[] odjazd(Moment moment, Dziennik dziennik) {
        // Przed zabraniem ludzi aktualizujemy sumę czasu przebywania w kolejce
        aktualizujStatystykiKolejki(moment);
        
        Sportowiec[] odjezdzajacySportowcy = obecnaKolejka.zdejmij(Math.min(obecnaKolejka.rozmiar(), ladownosc));

        for (Sportowiec sportowiec : odjezdzajacySportowcy) {
            dziennik.dodajWpisZeSportowcem(moment, sportowiec, String.format("rozpoczął wjazd %s", toString()));
        }

        Zdarzenie[] noweZdarzenia = new Zdarzenie[1 + odjezdzajacySportowcy.length];
        noweZdarzenia[0] = new OdjazdWyciagu(moment.dodajInterwal(odstepMiedzyOdjazdami), this);

        for (int i = 0; i < odjezdzajacySportowcy.length; i++) {
            noweZdarzenia[1 + i] = new DotarcieDoWezla(moment.dodajInterwal(dlugosc()),
                    this,
                    koniec(),
                    odjezdzajacySportowcy[i]);
        }

        lacznaLiczbaPasazerow += odjezdzajacySportowcy.length;
        liczbaOdjazdow++; // Rejestrujemy odjazd, by znać maksymalną potencjalną przepustowość

        return noweZdarzenia;
    }

    /**
     * Optymalizacja liczenia średniej (bez odpytywania co sekundę).
     * Wywoływana ZANIM zmienimy rozmiar kolejki.
     */
    private void aktualizujStatystykiKolejki(Moment obecnyMoment) {
        if (czasStartuSymulacji == null) {
            czasStartuSymulacji = obecnyMoment;
        }
        if (czasOstatniejOperacji != null) {
            long uplynieteSekundy = obecnyMoment.odlegloscWSekundach(czasOstatniejOperacji);
            if (uplynieteSekundy > 0) {
                sumaDlugosciKolejkiZCzasem += uplynieteSekundy * obecnaKolejka.rozmiar();
            }
        }
        czasOstatniejOperacji = obecnyMoment;
    }

    /**
     * Oblicza średnią długość kolejki zaokrągloną do najbliższej liczby całkowitej.
     */
    private int sredniaDlugoscKolejki() {
        if (czasOstatniejOperacji == null || czasStartuSymulacji == null) return 0;
        long czasCalkowity = czasOstatniejOperacji.odlegloscWSekundach(czasStartuSymulacji);
        if (czasCalkowity == 0) return 0;
        
        return (int) Math.round((double) sumaDlugosciKolejkiZCzasem / czasCalkowity);
    }

    /**
     * Generuje etykietę dla pierwszej mapki z parametrami (wg przykładu z treści).
     */
    public String etykietaParametry() {
        return String.format(Locale.US, "w%d: %d os. co %ds\nczas: %ds",
                id(), ladownosc, odstepMiedzyOdjazdami.wSekundach(), dlugosc().wSekundach());
    }

    /**
     * Generuje etykietę dla drugiej mapki ze statystykami końcowymi (wg przykładu z treści).
     */
    public String etykietaStatystyki() {
        int maksymalnaPojemnosc = liczbaOdjazdow * ladownosc;
        int procentZajetych = (maksymalnaPojemnosc == 0) ? 0 : 
                              (int) Math.round((lacznaLiczbaPasazerow * 100.0) / maksymalnaPojemnosc);

        return String.format(Locale.US, "w%d: kol: %d(śr), %d (maks)\nwjazdy: %d / %d (%d%%)",
                id(), sredniaDlugoscKolejki(), maksymalnaDlugoscKolejki,
                lacznaLiczbaPasazerow, maksymalnaPojemnosc, procentZajetych);
    }

    @Override
    public String wypiszStatystyki() {
        return etykietaStatystyki(); // Podpinamy nową statystykę pod starą metodę
    }

    @Override
    public String toString() {
        return String.format("Wyciąg nr %d", id());
    }
}