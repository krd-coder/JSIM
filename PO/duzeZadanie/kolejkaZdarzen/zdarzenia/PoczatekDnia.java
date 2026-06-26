package duzeZadanie.kolejkaZdarzen.zdarzenia;

import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.sportowcy.Sportowiec;

public class PoczatekDnia extends Zdarzenie {

    private final Wezel wezel;

    private final Sportowiec sportowiec;

    public PoczatekDnia(Moment moment, Wezel wezel, Sportowiec sportowiec) {
        super(moment);
        this.wezel = wezel;
        this.sportowiec = sportowiec;
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
            String.format("rozpoczął swój dzień na stoku w %s", wezel.toString()));

        return new Zdarzenie[]{sportowiec.nastepnyKrok(moment, wezel)};
    }

    @Override
    public boolean czyPrzetwarzacPoZakonczeniuSymulacji() {
        return false;
    }

    @Override
    public String toString() {
        return "PoczatekDnia [wezel=" + wezel + ", sportowiec=" + sportowiec + ", super=" + super.toString() + "]";
    }
}
