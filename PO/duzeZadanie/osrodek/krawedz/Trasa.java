package duzeZadanie.osrodek.krawedz;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.osrodek.Wezel;
import java.util.Locale;

public class Trasa extends Krawedz {

    private final int poziomTrudnosci; // {0, 1, ..., 10}
    private final double bazowaAtrakcyjnosc; // [0, 1]
    private final double odpornoscNaNierownosci; // [0, 1]
    private int liczbaZjazdow;

    public Trasa(int id,
                 Wezel poczatek,
                 Wezel koniec,
                 Interwal dlugoscZjazdu,
                 int poziomTrudnosci,
                 double bazowaAtrakcyjnosc,
                 double odpornoscNaNierownosci) {
        super(id, poczatek, koniec, dlugoscZjazdu);

        assert poczatek.wysokosc() > koniec.wysokosc()
            : String.format("Trasa %d prowadzi w górę: %d -> %d", id, poczatek.wysokosc(), koniec.wysokosc());

        this.poziomTrudnosci = poziomTrudnosci;
        this.bazowaAtrakcyjnosc = bazowaAtrakcyjnosc;
        this.odpornoscNaNierownosci = odpornoscNaNierownosci;
        liczbaZjazdow = 0;
    }

    public int poziomTrudnosci() {
        return poziomTrudnosci;
    }

    public double bazowaAtrakcyjnosc() {
        return bazowaAtrakcyjnosc;
    }

    public double odpornoscNaNierownosci() {
        return odpornoscNaNierownosci;
    }

    public int liczbaZjazdow() {
        return liczbaZjazdow;
    }

    public Moment przemierz(Moment start) {
        liczbaZjazdow++;
        return start.dodajInterwal(dlugosc());
    }

    public double wyrownanieNawierzchni() {
        return (bazowaAtrakcyjnosc + (1 - bazowaAtrakcyjnosc) * Math.pow(odpornoscNaNierownosci, liczbaZjazdow));
    }

    /**
     * Zaktualizowane na potrzeby raportu końcowego z części 2.
     * Zwraca liczbę zjazdów oraz końcowe wyrównanie nawierzchni.
     */
    @Override
    public String wypiszStatystyki() {
        return String.format(Locale.US, "Trasa %d: %d zjazdów, wyrównanie: %.2f", 
                             id(), liczbaZjazdow, wyrownanieNawierzchni());
    }

    /**
     * Metoda pomocnicza dla GeneratorMapek (Mapka 1: Parametry tras).
     * Zwraca tekst w wymaganym formacie, np.:
     * t1: poziom: 7, czas: 180s
     * odporność: 0.30, 0.99970
     */
    public String etykietaParametry() {
        return String.format(Locale.US, "t%d: poziom: %d, czas: %ds\nodporność: %.2f, %.5f",
                id(), poziomTrudnosci, dlugosc().wSekundach(), bazowaAtrakcyjnosc, odpornoscNaNierownosci);
    }

    /**
     * Metoda pomocnicza dla GeneratorMapek (Mapka 2: Statystyki tras).
     * Zwraca tekst w wymaganym formacie, np.:
     * t1: śnieg: 0.23
     * zjazdy: 5191
     */
    public String etykietaStatystyki() {
        return String.format(Locale.US, "t%d: śnieg: %.2f\nzjazdy: %d",
                id(), wyrownanieNawierzchni(), liczbaZjazdow);
    }

    @Override
    public String toString() {
        return String.format("Trasa nr %d", id());
    }
}