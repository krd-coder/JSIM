package duzeZadanie.sportowcy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.kolejkaZdarzen.zdarzenia.DolaczenieDoKolejki;
import duzeZadanie.kolejkaZdarzen.zdarzenia.RozpoczecieZjazdu;
import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

import java.util.HashMap;
import java.util.Map;

public abstract class Sportowiec {

    protected final int WSPOLCZYNNIK_ROZNICY_POZIOMOW_TRUDNOSCI = 5;
    protected final int WSPOLCZYNNIK_ROZNICY_POZIOMOW_TRUDNOSCI_LATWEJ_TRASY = 7;
    protected final double DOMYSLNA_ATRAKCYJNOSC_LATWEJ_TRASY = 0.2;

    protected final int id;
    protected final int poziomZaawansowania;
    protected final double wspolczynnikSpontanicznosci;
    protected final double wspolczynnikTrudnosci;
    protected final double wspolczynnikNawierzchni;
    
    // Pola dla znudzenia
    protected final double wspolczynnikZnudzenia; // beta z przedziału [0, 1]
    protected final double wagaZnudzenia;         // alpha_z

    protected final boolean sledzony;
    protected final Wezel wezelStartowy;
    protected final Moment momentStartu;
    protected final MaszynaLosujaca maszynaLosujaca;

    /**
     * Klasa pomocnicza przechowująca wartość znudzenia oraz moment (indeks) 
     * jej ostatniej aktualizacji, co pozwala na optymalizację obliczeń.
     */
    protected static class StanZnudzenia {
        final double z;
        final int indeksOstatniegoZjazdu;

        StanZnudzenia(double z, int indeksOstatniegoZjazdu) {
            this.z = z;
            this.indeksOstatniegoZjazdu = indeksOstatniegoZjazdu;
        }
    }

    // Mapa przechowująca stan znudzenia dla tras, którymi sportowiec już jechał
    protected final Map<Trasa, StanZnudzenia> znudzenieTrasami = new HashMap<>();
    
    // Całkowita liczba zjazdów wykonana przez tego sportowca
    protected int licznikWszystkichZjazdow = 0;

    public Sportowiec(int id,
                      int poziomZaawansowania,
                      double wspolczynnikSpontanicznosci,
                      double wspolczynnikTrudnosci,
                      double wspolczynnikNawierzchni,
                      double wspolczynnikZnudzenia,
                      double wagaZnudzenia,
                      boolean sledzony,
                      Wezel wezelStartowy,
                      Moment momentStartu,
                      MaszynaLosujaca maszynaLosujaca) {
        this.id = id;
        this.poziomZaawansowania = poziomZaawansowania;
        this.wspolczynnikSpontanicznosci = wspolczynnikSpontanicznosci;
        this.wspolczynnikTrudnosci = wspolczynnikTrudnosci;
        this.wspolczynnikNawierzchni = wspolczynnikNawierzchni;
        this.wspolczynnikZnudzenia = wspolczynnikZnudzenia;
        this.wagaZnudzenia = wagaZnudzenia;
        this.sledzony = sledzony;
        this.wezelStartowy = wezelStartowy;
        this.momentStartu = momentStartu;
        this.maszynaLosujaca = maszynaLosujaca;
    }

    public int id() { return id; }
    public Moment momentStartu() { return momentStartu; }
    public boolean sledzony() { return sledzony; }
    public Wezel wezelStartowy() { return wezelStartowy; }

    /**
     * Zaktualizowany wzór z części 2. Uwzględnia aktualne znudzenie (z_t).
     */
    protected double lacznaAtrakcyjnosc(Trasa trasa) {
        double z = pobierzAktualneZnudzenie(trasa);
        return wspolczynnikTrudnosci * atrakcyjnoscPoziomuTrudnosci(trasa)
             + wspolczynnikNawierzchni * trasa.wyrownanieNawierzchni()
             + wagaZnudzenia * (1.0 - z);
    }

    /**
     * Oblicza bieżące znudzenie daną trasą na podstawie opóźnionej aktualizacji (decay).
     */
    public double pobierzAktualneZnudzenie(Trasa trasa) {
        StanZnudzenia stan = znudzenieTrasami.get(trasa);
        if (stan == null) {
            return 0.0; // Początkowe znudzenie to zawsze 0.
        }
        
        // i - liczba przejazdów innymi trasami od czasu ostatniego zjazdu tą trasą
        int i = licznikWszystkichZjazdow - stan.indeksOstatniegoZjazdu;
        
        // Obliczamy wyblakłe znudzenie: z_k * (1 - beta)^i
        return stan.z * Math.pow(1.0 - wspolczynnikZnudzenia, i);
    }

    /**
     * Metoda, którą należy wywołać w zdarzeniu ZjazdTrasą (np. w RozpoczecieZjazdu),
     * aby zaktualizować poziom znudzenia sportowca.
     */
    public void odnotujZjazd(Trasa trasa) {
        // Pobieramy aktualne z_t (uwzględniające wyblaknięcie od poprzednich zjazdów)
        double aktualneZ = pobierzAktualneZnudzenie(trasa);
        
        // Aktualizujemy zgodnie ze wzorem dla x_t = 1 (zjazd WŁAŚNIE tą trasą)
        double noweZ = wspolczynnikZnudzenia * 1.0 + (1.0 - wspolczynnikZnudzenia) * aktualneZ;
        
        // Inkrementujemy licznik zjazdów (wykonano ruch)
        licznikWszystkichZjazdow++;
        
        // Zapisujemy nowy stan znudzenia dla tej trasy
        znudzenieTrasami.put(trasa, new StanZnudzenia(noweZ, licznikWszystkichZjazdow));
    }

    protected double atrakcyjnoscPoziomuTrudnosci(Trasa trasa) {
        int poziomTrudnosci = trasa.poziomTrudnosci();
        double roznicaPoziomow = poziomTrudnosci - poziomZaawansowania;

        if (poziomTrudnosci >= poziomZaawansowania + WSPOLCZYNNIK_ROZNICY_POZIOMOW_TRUDNOSCI) {
            return 0;
        } else if (poziomZaawansowania + WSPOLCZYNNIK_ROZNICY_POZIOMOW_TRUDNOSCI > poziomTrudnosci
                   && poziomTrudnosci >= poziomZaawansowania) {
            return 1.0 - roznicaPoziomow / (double) WSPOLCZYNNIK_ROZNICY_POZIOMOW_TRUDNOSCI;
        } else {
            return Math.max(DOMYSLNA_ATRAKCYJNOSC_LATWEJ_TRASY,
                1 - (-roznicaPoziomow) / (double) WSPOLCZYNNIK_ROZNICY_POZIOMOW_TRUDNOSCI_LATWEJ_TRASY);
        }
    }

    protected DolaczenieDoKolejki nastepnyKrokWyciag(Moment moment, Wyciag wyciag) {
        return new DolaczenieDoKolejki(moment, wyciag, this);
    }

    protected RozpoczecieZjazdu nastepnyKrokTrasa(Moment moment, Trasa trasa) {
        return new RozpoczecieZjazdu(moment, trasa, this);
    }

    public abstract Zdarzenie nastepnyKrok(Moment moment, Wezel obecnyWezel);

    public abstract Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu);

    // W klasie Sportowiec
    public int liczbaPrzejazdow(duzeZadanie.osrodek.krawedz.Krawedz krawedz) {
        return licznikWszystkichZjazdow - (znudzenieTrasami.getOrDefault(krawedz, new StanZnudzenia(0.0, 0))).indeksOstatniegoZjazdu; 
    }

    public String pobierzHistorieKrawedzi(duzeZadanie.osrodek.krawedz.Krawedz krawedz) {
        // TODO: Zwróć string typu "1, 4, 7" dla podanej krawędzi
        return ""; 
    }

    @Override
    public String toString() {
        return String.format("Sportowiec nr %d (%s)", id, this.getClass().getSimpleName());
    }
}