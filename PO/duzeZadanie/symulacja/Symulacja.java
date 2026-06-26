package duzeZadanie.symulacja;

import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.kolejkaZdarzen.KolejkaZdarzen;
import duzeZadanie.kolejkaZdarzen.zdarzenia.OdjazdWyciagu;
import duzeZadanie.kolejkaZdarzen.zdarzenia.PoczatekDnia;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.sportowcy.Sportowiec;

// Zakładane importy z dostarczonej paczki GeneratorMapek (dostosuj do struktury projektu)
import generatorMapek.GeneratorMapek;
import generatorMapek.WyjatekSystemuPlikow;
import generatorMapek.StylWezla;
import generatorMapek.GruboscKonturu;
import generatorMapek.StylKrawedzi;
import generatorMapek.StylLinii;

import java.util.Arrays;
import java.util.List;

public class Symulacja {

    private static final Moment POCZATEK_DNIA = new Moment(9, 0, 0);
    private static final Moment KONIEC_SYMULACJI = new Moment(15, 0, 0);

    /**
     * Główna metoda przeprowadzająca symulację. 
     * Dodano parametr sciezkaMapek do zapisu plików .tex.
     */
    public void przeprowadzSymulacje(Dziennik dziennik,
                                     KolejkaZdarzen kolejkaZdarzen,
                                     Osrodek osrodek,
                                     Sportowiec[] sportowcy,
                                     String sciezkaMapek) {
        przygotujPoczatkoweZdarzenia(kolejkaZdarzen, osrodek, sportowcy);
        glownaPetla(kolejkaZdarzen, dziennik);
        zbierzStatystyki(osrodek, dziennik);
        
        // Nowy krok: Generowanie plików .tex z mapkami [cite: 78, 80]
        generujMapki(osrodek, sportowcy, sciezkaMapek);
    }

    private void przygotujPoczatkoweZdarzenia(KolejkaZdarzen kolejkaZdarzen, Osrodek osrodek, Sportowiec[] sportowcy) {
        for (Sportowiec sportowiec : sportowcy) {
            kolejkaZdarzen.dodaj(new PoczatekDnia(sportowiec.momentStartu(), sportowiec.wezelStartowy(), sportowiec));
        }
        for (Wyciag wyciag : osrodek.wyciagi()) {
            kolejkaZdarzen.dodaj(new OdjazdWyciagu(POCZATEK_DNIA, wyciag));
        }
    }

    private void glownaPetla(KolejkaZdarzen kolejkaZdarzen, Dziennik dziennik) {
        while (!kolejkaZdarzen.czyPusta()) {
            Zdarzenie nastepneZdarzenie = kolejkaZdarzen.zdejmij();
            Zdarzenie[] noweZdarzenia = nastepneZdarzenie.przetworz(dziennik);

            for (Zdarzenie noweZdarzenie : noweZdarzenia) {
                if (noweZdarzenie.moment().wczesniejNiz(KONIEC_SYMULACJI)
                    || noweZdarzenie.czyPrzetwarzacPoZakonczeniuSymulacji()) {
                    kolejkaZdarzen.dodaj(noweZdarzenie);
                }
            }
        }
    }

    private void zbierzStatystyki(Osrodek osrodek, Dziennik dziennik) {
        String[][] statystyki = new String[osrodek.trasy().length + osrodek.wyciagi().length][2];

        for (int i = 0; i < osrodek.trasy().length; i++) {
            statystyki[i] = new String[]{osrodek.trasy()[i].toString(), osrodek.trasy()[i].wypiszStatystyki()};
        }
        for (int i = 0; i < osrodek.wyciagi().length; i++) {
            statystyki[osrodek.trasy().length + i] = new String[]{osrodek.wyciagi()[i].toString(),
                osrodek.wyciagi()[i].wypiszStatystyki()};
        }

        dziennik.dodajTabele(statystyki);
    }

    /**
     * Obsługuje proces tworzenia wszystkich trzech rodzajów mapek.
     */
    private void generujMapki(Osrodek osrodek, Sportowiec[] sportowcy, String sciezkaMapek) {
        if (sciezkaMapek == null || sciezkaMapek.isEmpty()) {
            System.err.println("Błąd: Nie podano ścieżki do katalogu na mapki. Uruchom program z poprawnym argumentem.");
            return; // Przerwanie bez wyjątku, program wraca do main i kończy się naturalnie [cite: 162, 163]
        }

        try {
            // Wszystkie mapki należy wygenerować tym samym (jedynym) obiektem generatora [cite: 158]
            GeneratorMapek generator = new GeneratorMapek(sciezkaMapek);
            
            // Styl ciągły dla tras, przerywany dla wyciągów [cite: 106]
            StylKrawedzi stylTrasy = new StylKrawedzi(StylLinii.CIAGLA);
            StylKrawedzi stylWyciagu = new StylKrawedzi(StylLinii.PRZERYWANA);

            // --- MAPKA 1: PARAMETRY ---
            generator.zeruj();
            rysujWezly(generator, osrodek);
            for (Trasa trasa : osrodek.trasy()) {
                List<String> tekst = Arrays.asList(trasa.etykietaParametry().split("\n"));
                generator.dodajKrawedz(trasa.poczatek().id(), trasa.koniec().id(), stylTrasy, tekst);
            }
            for (Wyciag wyciag : osrodek.wyciagi()) {
                List<String> tekst = Arrays.asList(wyciag.etykietaParametry().split("\n"));
                generator.dodajKrawedz(wyciag.poczatek().id(), wyciag.koniec().id(), stylWyciagu, tekst);
            }
            generator.zapiszMapke("parametry.tex");

            // --- MAPKA 2: STATYSTYKI ---
            generator.zeruj();
            rysujWezly(generator, osrodek);
            for (Trasa trasa : osrodek.trasy()) {
                List<String> tekst = Arrays.asList(trasa.etykietaStatystyki().split("\n"));
                generator.dodajKrawedz(trasa.poczatek().id(), trasa.koniec().id(), stylTrasy, tekst);
            }
            for (Wyciag wyciag : osrodek.wyciagi()) {
                List<String> tekst = Arrays.asList(wyciag.etykietaStatystyki().split("\n"));
                generator.dodajKrawedz(wyciag.poczatek().id(), wyciag.koniec().id(), stylWyciagu, tekst);
            }
            generator.zapiszMapke("statystyki.tex");

            // --- MAPKA 3: HISTORIE ŚLEDZONYCH SPORTOWCÓW ---
            for (Sportowiec s : sportowcy) {
                if (s.sledzony()) {
                    generator.zeruj();
                    rysujWezly(generator, osrodek);
                    
                    // Zakładam istnienie metody w klasie Sportowiec zwracającej string przejazdów 
                    // np. "3,10" dla danej krawędzi. Jeśli pusty/null, to krawędzi nie podpisujemy.
                    for (Trasa trasa : osrodek.trasy()) {
                        String historia = s.pobierzHistorieKrawedzi(trasa);
                        if (historia != null && !historia.isEmpty()) {
                            String etykieta = String.format("t%d (%d): %s", trasa.id(), s.liczbaPrzejazdow(trasa), historia);
                            // Korzystamy z przeciążonej metody dodajKrawedz (przyjmującej String zamiast Listy), 
                            // która automatycznie tnie i dzieli tekst [cite: 151, 152]
                            generator.dodajKrawedz(trasa.poczatek().id(), trasa.koniec().id(), stylTrasy, etykieta);
                        } else {
                            // Rysujemy krawędź bez tekstu, by struktura grafu była pełna
                            generator.dodajKrawedz(trasa.poczatek().id(), trasa.koniec().id(), stylTrasy, Arrays.asList());
                        }
                    }
                    for (Wyciag wyciag : osrodek.wyciagi()) {
                        String historia = s.pobierzHistorieKrawedzi(wyciag);
                        if (historia != null && !historia.isEmpty()) {
                            String etykieta = String.format("w%d (%d): %s", wyciag.id(), s.liczbaPrzejazdow(wyciag), historia);
                            generator.dodajKrawedz(wyciag.poczatek().id(), wyciag.koniec().id(), stylWyciagu, etykieta);
                        } else {
                            generator.dodajKrawedz(wyciag.poczatek().id(), wyciag.koniec().id(), stylWyciagu, Arrays.asList());
                        }
                    }
                    
                    // Nazwa pliku zawiera numer sportowca [cite: 154]
                    generator.zapiszMapke("historia_sportowca_" + s.id() + ".tex");
                }
            }

        } catch (WyjatekSystemuPlikow e) {
            System.err.println("Błąd systemu plików podczas generowania mapek. Upewnij się, że podana ścieżka jest poprawna i posiadasz uprawnienia do zapisu.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Wystąpił nieoczekiwany błąd podczas generowania mapek. Prosimy o zgłoszenie tego błędu zespołowi deweloperskiemu.");
            e.printStackTrace();
        }
    }

    /**
     * Metoda pomocnicza odrysowująca węzły ośrodka dla generatora mapek.
     */
    private void rysujWezly(GeneratorMapek generator, Osrodek osrodek) {
        for (Wezel w : osrodek.wezly()) {
            StylWezla styl = w.czySkomunikowany() ? 
                             new StylWezla(GruboscKonturu.POGRUBIONY) : 
                             new StylWezla(GruboscKonturu.ZWYKLY);
                             
            // Węzły skomunikowane mają mieć pogrubiony kontur względem pozostałych [cite: 106]
            // Uwaga: Zakładam że węzeł ma metody x() i y() zczytane z formatu wejścia z cz. 1
            generator.dodajWezel(w.id(), w.x(), w.y(), styl);
        }
    }
}