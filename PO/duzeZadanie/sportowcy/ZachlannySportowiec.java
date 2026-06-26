package duzeZadanie.sportowcy;

import duzeZadanie.algorytmy.BFS;
import duzeZadanie.czas.Interwal;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import java.util.List;

public class ZachlannySportowiec extends PlanujacySportowiec {

    // Zachłanny sportowiec potrzebuje wiedzy o całym ośrodku, 
    // aby ocenić wszystkie dostępne w nim trasy.
    private final Osrodek osrodek;

    public ZachlannySportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci, 
                               double wspolczynnikTrudnosci, double wspolczynnikNawierzchni, 
                               double wspolczynnikZnudzenia, double wagaZnudzenia, boolean sledzony, 
                               Wezel wezelStartowy, duzeZadanie.czas.Moment momentStartu, 
                               duzeZadanie.losowosc.MaszynaLosujaca maszynaLosujaca, Osrodek osrodek) {
        
        super(id, poziomZaawansowania, wspolczynnikSpontanicznosci, wspolczynnikTrudnosci, 
              wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia, sledzony, 
              wezelStartowy, momentStartu, maszynaLosujaca);
        this.osrodek = osrodek;
    }

    @Override
    protected void wygenerujNowyPlan(Wezel obecnyWezel) {
        // 1. Przeszukaj cały Ośrodek w poszukiwaniu najatrakcyjniejszej trasy
        Trasa najlepszaTrasa = null;
        double najwiekszaAtrakcyjnosc = -1.0;

        // UWAGA: Upewnij się, że masz metodę zwracającą wszystkie trasy w klasie Osrodek
        // (np. pobierzWszystkieTrasy(), zwracającą List<Trasa> lub Trasa[])
        for (Trasa trasa : osrodek.pobierzWszystkieTrasy()) {
            double atrakcyjnosc = lacznaAtrakcyjnosc(trasa);
            
            if (atrakcyjnosc > najwiekszaAtrakcyjnosc) {
                najwiekszaAtrakcyjnosc = atrakcyjnosc;
                najlepszaTrasa = trasa;
            }
        }

        // Zabezpieczenie na wypadek braku tras w grafie
        if (najlepszaTrasa == null) {
            return;
        }

        // 2. Uruchom BFS z `obecnyWezel` do wierzchołka początkowego upatrzonej trasy
        NawigacjaBFS nawigacja = new NawigacjaBFS(osrodek);
        Wezel celPodrozy = najlepszaTrasa.poczatek(); // Dojeżdżamy do początku wymarzonej trasy
        
        List<Object> sciezkaDojazdu = nawigacja.wyznaczPlan(obecnyWezel, celPodrozy);

        // 3. Załaduj wyznaczoną ścieżkę krawędzi (dojazd) do obecnego planu
        obecnyPlan.clear(); // Czyścimy kolejkę dla pewności
        obecnyPlan.addAll(sciezkaDojazdu);
        
        // 4. Na koniec planu dodajemy sam docelowy zjazd wymarzoną trasą 
        obecnyPlan.add(najlepszaTrasa);
    }

    @Override
    public Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu) {
        return new ZachlannySportowiec(id + przesuniecieId, poziomZaawansowania, wspolczynnikSpontanicznosci,
                wspolczynnikTrudnosci, wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia,
                sledzony, wezelStartowy, momentStartu.dodajInterwal(przesuniecieMomentuStartu), 
                maszynaLosujaca, osrodek);
    }
}