package duzeZadanie.wczytywacz;

import java.util.Arrays;
import java.util.Scanner;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.sportowcy.GrupaSportowcow;
import duzeZadanie.sportowcy.Sportowiec;
import duzeZadanie.sportowcy.LokalnySportowiec;
import duzeZadanie.sportowcy.ZachlannySportowiec;
import duzeZadanie.sportowcy.KolekcjonerSportowiec;

public class Wczytywacz {

    private final Scanner scanner;
    private final MaszynaLosujaca maszynaLosujaca;

    public Wczytywacz(Scanner scanner, MaszynaLosujaca maszynaLosujaca) {
        this.scanner = scanner;
        this.maszynaLosujaca = maszynaLosujaca;
    }

    public DaneWejsciowe wczytajWejscie() {
        Wezel[] wezly = wczytajWezly();
        Wyciag[] wyciagi = wczytajWyciagi(wezly);
        Trasa[] trasy = wczytajTrasy(wezly);

        // ZMIANA: Tworzymy i wiążemy struktury Ośrodka PRZED wczytaniem sportowców,
        // ponieważ nowi sportowcy (Zachłanny i Kolekcjoner) muszą otrzymać referencję 
        // do całego grafu, by uruchamiać BFS.
        for (Wezel wezel : wezly) {
            wezel.wychodzaceTrasy(Arrays.stream(trasy)
                    .filter(t -> t.poczatek().equals(wezel))
                    .toArray(Trasa[]::new));
            wezel.wychodzaceWyciagi(Arrays.stream(wyciagi)
                    .filter(w -> w.poczatek().equals(wezel))
                    .toArray(Wyciag[]::new));
        }
        Osrodek osrodek = new Osrodek(wezly, trasy, wyciagi);

        GrupaSportowcow[] grupySportowcow = wczytajGrupySportowcow(wezly, osrodek);
        
        return new DaneWejsciowe(wezly, trasy, wyciagi, grupySportowcow);
    }

    private Wezel[] wczytajWezly() {
        int liczbaWezlow = scanner.nextInt();
        Wezel[] wezly = new Wezel[liczbaWezlow];

        for (int id = 0; id < liczbaWezlow; id++) {
            int wysokosc = scanner.nextInt();
            int wspolrzednaX = scanner.nextInt();
            int wspolrzednaY = scanner.nextInt();
            boolean czyStartowy = scanner.findInLine("s") != null;
            wezly[id] = new Wezel(id, wysokosc, wspolrzednaX, wspolrzednaY, czyStartowy);
        }

        return wezly;
    }

    private Wyciag[] wczytajWyciagi(Wezel[] wezly) {
        int liczbaWyciagow = scanner.nextInt();
        Wyciag[] wyciagi = new Wyciag[liczbaWyciagow];

        for (int id = 0; id < liczbaWyciagow; id++) {
            int poczatek = scanner.nextInt();
            int koniec = scanner.nextInt();
            int odstep = scanner.nextInt();
            int maksymalnaWielkoscGrupy = scanner.nextInt();
            int czasPrzejazdu = scanner.nextInt();

            Wyciag wyciag = new Wyciag(id,
                wezly[poczatek],
                wezly[koniec],
                new Interwal(odstep),
                new Interwal(czasPrzejazdu),
                maksymalnaWielkoscGrupy);

            wyciagi[id] = wyciag;
        }

        return wyciagi;
    }

    private Trasa[] wczytajTrasy(Wezel[] wezly) {
        int liczbaTras = scanner.nextInt();
        Trasa[] trasy = new Trasa[liczbaTras];

        for (int id = 0; id < liczbaTras; id++) {
            int poczatek = scanner.nextInt();
            int koniec = scanner.nextInt();
            int poziomTrudnosci = scanner.nextInt();
            int czasPrzejazdu = scanner.nextInt();
            Interwal dlugosc = new Interwal(czasPrzejazdu);
            double bazowaAtrakcyjnosc = scanner.nextDouble();
            double odpornoscNaNierownosci = scanner.nextDouble();

            Trasa trasa = new Trasa(id,
                wezly[poczatek],
                wezly[koniec],
                dlugosc,
                poziomTrudnosci,
                bazowaAtrakcyjnosc,
                odpornoscNaNierownosci);

            trasy[id] = trasa;
        }

        return trasy;
    }

    private GrupaSportowcow[] wczytajGrupySportowcow(Wezel[] wezly, Osrodek osrodek) {
        int nastepneId = 0;
        int liczbaGrup = scanner.nextInt();

        GrupaSportowcow[] grupySportowcow = new GrupaSportowcow[liczbaGrup];

        for (int grupa = 0; grupa < liczbaGrup; grupa++) {
            grupySportowcow[grupa] = wczytajGrupeSportowcow(nastepneId, wezly, osrodek);
            nastepneId += grupySportowcow[grupa].krotnosc();
        }

        return grupySportowcow;
    }

    private GrupaSportowcow wczytajGrupeSportowcow(int nastepneId, Wezel[] wezly, Osrodek osrodek) {
        // --- NOWY FORMAT: Linia 1 ---
        int liczbaSportowcowWGrupie = scanner.nextInt();
        int poziomZaawansowania = scanner.nextInt();
        double wspolczynnikSpontanicznosci = scanner.nextDouble();
        double wspolczynnikZnudzenia = scanner.nextDouble(); // Nowe [cite: 48]
        String rodzajSportowca = scanner.next();             // Nowe: "L", "Z" lub "K" [cite: 51]
        boolean czySledzeni = scanner.findInLine("s") != null; // [cite: 48]

        // --- NOWY FORMAT: Linia 2 ---
        double wagaDopasowania = scanner.nextDouble();
        double wagaJakosciNawierzchni = scanner.nextDouble();
        double wagaZnudzenia = scanner.nextDouble();         // Nowe 

        // --- NOWY FORMAT: Linia 3 ---
        int idPoczatkowegoWezla = scanner.nextInt();
        Moment start = wczytajMoment();
        Interwal odstepCzasowy = new Interwal(0);

        if (liczbaSportowcowWGrupie > 1) {
            odstepCzasowy = new Interwal(scanner.nextInt()); // [cite: 50]
        }

        Sportowiec pierwszySportowiec;

        // Na podstawie wczytanego rodzaju inicjujemy odpowiednią klasę dziedziczącą
        switch (rodzajSportowca) {
            case "Z":
                pierwszySportowiec = new ZachlannySportowiec(nastepneId, poziomZaawansowania, 
                    wspolczynnikSpontanicznosci, wagaDopasowania, wagaJakosciNawierzchni, 
                    wspolczynnikZnudzenia, wagaZnudzenia, czySledzeni, 
                    wezly[idPoczatkowegoWezla], start, maszynaLosujaca, osrodek);
                break;
            case "K":
                pierwszySportowiec = new KolekcjonerSportowiec(nastepneId, poziomZaawansowania, 
                    wspolczynnikSpontanicznosci, wagaDopasowania, wagaJakosciNawierzchni, 
                    wspolczynnikZnudzenia, wagaZnudzenia, czySledzeni, 
                    wezly[idPoczatkowegoWezla], start, maszynaLosujaca, osrodek);
                break;
            case "L":
            default:
                pierwszySportowiec = new LokalnySportowiec(nastepneId, poziomZaawansowania, 
                    wspolczynnikSpontanicznosci, wagaDopasowania, wagaJakosciNawierzchni, 
                    wspolczynnikZnudzenia, wagaZnudzenia, czySledzeni, 
                    wezly[idPoczatkowegoWezla], start, maszynaLosujaca);
                break;
        }

        return new GrupaSportowcow(pierwszySportowiec, liczbaSportowcowWGrupie, odstepCzasowy);
    }

    private Moment wczytajMoment() {
        String napis = scanner.next();
        String[] napisy = napis.split(":");
        int[] liczby = new int[napisy.length];

        for (int i = 0; i < liczby.length; i++) {
            liczby[i] = Integer.parseInt(napisy[i]);
        }

        return new Moment(liczby[0], liczby[1], liczby[2]);
    }
}