package duzeZadanie.wczytywacz;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.sportowcy.GrupaSportowcow;
import duzeZadanie.sportowcy.Sportowiec;

public class DaneWejsciowe {

    private final Osrodek osrodek;
    private final Sportowiec[] sportowcy;

    public DaneWejsciowe(Wezel[] wezly, Trasa[] trasy, Wyciag[] wyciagi, GrupaSportowcow[] grupySportowcow) {
        for (Wezel wezel : wezly) {
            // Używamy strumieni do zwięzłego filtrowania krawędzi
            wezel.wychodzaceTrasy(Arrays.stream(trasy)
                    .filter(t -> t.poczatek().equals(wezel))
                    .toArray(Trasa[]::new));
                    
            wezel.wychodzaceWyciagi(Arrays.stream(wyciagi)
                    .filter(w -> w.poczatek().equals(wezel))
                    .toArray(Wyciag[]::new));
        }
        
        this.osrodek = new Osrodek(wezly, trasy, wyciagi);
        this.sportowcy = przetworzGrupy(grupySportowcow);
    }

    public Osrodek osrodek() {
        return osrodek;
    }

    public Sportowiec[] sportowcy() {
        return sportowcy;
    }

    private Sportowiec[] przetworzGrupy(GrupaSportowcow[] grupySportowcow) {
        // Łączymy wszystkich sportowców ze wszystkich grup w jedną tablicę za pomocą strumieni
        return Arrays.stream(grupySportowcow)
                .flatMap(grupa -> Arrays.stream(grupa.podajSportowcow()))
                .toArray(Sportowiec[]::new);
    }

    @Override
    public String toString() {
        return "DaneWejsciowe [osrodek=" + osrodek + ", sportowcy=" + Arrays.toString(sportowcy) + "]";
    }
}