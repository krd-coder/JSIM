package duzeZadanie.sportowcy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.losowosc.MaszynaLosujaca;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Krawedz;
import duzeZadanie.osrodek.krawedz.Trasa;

import java.util.List;

public class ZachlannySportowiec extends PlanujacySportowiec {

    private final Osrodek osrodek;

    public ZachlannySportowiec(int id, int poziomZaawansowania, double wspolczynnikSpontanicznosci,
                               double wspolczynnikTrudnosci, double wspolczynnikNawierzchni,
                               double wspolczynnikZnudzenia, double wagaZnudzenia, boolean sledzony,
                               Wezel wezelStartowy, Moment momentStartu, MaszynaLosujaca maszynaLosujaca, Osrodek osrodek) {
        super(id, poziomZaawansowania, wspolczynnikSpontanicznosci, wspolczynnikTrudnosci,
              wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia, sledzony,
              wezelStartowy, momentStartu, maszynaLosujaca, osrodek);
        this.osrodek = osrodek;
    }

    @Override
    protected Trasa znajdzTraseDocelowa(Wezel obecnyWezel) {
        Trasa najatrakcyjniejsza = null;
        double maxAtrakcyjnosc = -1.0;

        // Tutaj sportowiec musi mieć dostęp do wszystkich tras w grafie
        List<Trasa> wszystkieTrasy = osrodek.trasy();

        for (Trasa trasa : wszystkieTrasy) {
            double atrakcyjnosc = lacznaAtrakcyjnosc(trasa);
            if (atrakcyjnosc > maxAtrakcyjnosc) {
                maxAtrakcyjnosc = atrakcyjnosc;
                najatrakcyjniejsza = trasa;
            }
        }
        return najatrakcyjniejsza;
    }

    @Override
    protected Krawedz wylosujKrawedzZWezla(Wezel wezel) {
        List<Krawedz> wszystkie = wezel.pobierzWszystkieWychodzace();
        return wszystkie.get(maszynaLosujaca.losowyInt(0, wszystkie.size() - 1));
    }

    @Override
    public Sportowiec kopia(int przesuniecieId, Interwal przesuniecieMomentuStartu) {
        return new ZachlannySportowiec(this.id + przesuniecieId, poziomZaawansowania, wspolczynnikSpontanicznosci,
                wspolczynnikTrudnosci, wspolczynnikNawierzchni, wspolczynnikZnudzenia, wagaZnudzenia,
                sledzony, wezelStartowy, momentStartu.dodaj(przesuniecieMomentuStartu), maszynaLosujaca, osrodek);
    }
}