package duzeZadanie.sportowcy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;

public class KolekcjonerSportowiec extends Sportowiec {
    private final Osrodek osrodek;

    public KolekcjonerSportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci, 
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
        // Tu docelowo wstawisz wywołanie BFS (podobnie jak u Zachłannego, ale szukając trasy z min. zjazdami)
    }

    @Override
    public Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu) {
        return new KolekcjonerSportowiec(id + przesuniecieId, poziomZaawansowania, wspolczynnikSpontanicznosci,
                wspolczynnikTrudnosci, wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia,
                sledzony, wezelStartowy, momentStartu.dodajInterwal(przesuniecieMomentuStartu), maszynaLosujaca, osrodek);
    }
}