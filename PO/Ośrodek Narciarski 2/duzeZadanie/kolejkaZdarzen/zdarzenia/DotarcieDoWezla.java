package duzeZadanie.kolejkaZdarzen.zdarzenia;

import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Krawedz;
import duzeZadanie.sportowcy.Sportowiec;

public class DotarcieDoWezla extends Zdarzenie {

    private final Krawedz poprzedniaKrawedz;

    private final Wezel wezel;

    private final Sportowiec sportowiec;

    public DotarcieDoWezla(Moment moment, Krawedz poprzedniaKrawedz, Wezel wezel, Sportowiec sportowiec) {
        super(moment);
        this.poprzedniaKrawedz = poprzedniaKrawedz;
        this.wezel = wezel;
        this.sportowiec = sportowiec;
    }

    public Krawedz poprzedniaKrawedz() {
        return poprzedniaKrawedz;
    }

    public Wezel wezel() {
        return wezel;
    }

    public Sportowiec sportowiec() {
        return sportowiec;
    }

    @Override
    public Zdarzenie[] przetworz(Dziennik dziennik) {
        dziennik.dodajWpisZeSportowcem(moment,
            sportowiec,
            String.format("zakończył przemierzanie %s i znajduje się w %s",
                poprzedniaKrawedz.toString(),
                wezel.toString()));

        return new Zdarzenie[]{sportowiec.nastepnyKrok(moment, wezel)};
    }

    /**
     * Zgodnie z treścią, jesteśmy zobowiązani do przetworzenia zdarzeń
     * kończących zjazd trasą lub wjazd wyciągiem, nawet jeśli ma to miejsce po
     * zakończeniu symulacji, o ile zjazd/wjazd zaczął się przed jej końcem.
     */
    @Override
    public boolean czyPrzetwarzacPoZakonczeniuSymulacji() {
        return true;
    }

    @Override
    public String toString() {
        return "DotarcieDoWezla [poprzedniaKrawedz=" + poprzedniaKrawedz + ", wezel=" + wezel + ", sportowiec="
            + sportowiec + ", super = " + super.toString() + "]";
    }
}
