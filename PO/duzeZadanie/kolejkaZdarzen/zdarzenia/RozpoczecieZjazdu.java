package duzeZadanie.kolejkaZdarzen.zdarzenia;

import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.sportowcy.Sportowiec;

public class RozpoczecieZjazdu extends Zdarzenie {

    private final Trasa trasa;

    private final Sportowiec sportowiec;

    public RozpoczecieZjazdu(Moment moment, Trasa trasa, Sportowiec sportowiec) {
        super(moment);
        this.trasa = trasa;
        this.sportowiec = sportowiec;
    }

    public Trasa trasa() {
        return trasa;
    }

    public Sportowiec sportowiec() {
        return sportowiec;
    }

    @Override
    public Zdarzenie[] przetworz(Dziennik dziennik) {
        dziennik.dodajWpisZeSportowcem(moment, sportowiec, String.format("rozpoczyna zjazd %s", trasa.toString()));

        return new Zdarzenie[]{new DotarcieDoWezla(trasa.przemierz(moment), trasa, trasa.koniec(), sportowiec)};
    }

    @Override
    public boolean czyPrzetwarzacPoZakonczeniuSymulacji() {
        return false;
    }

    @Override
    public String toString() {
        return "RozpoczecieZjazdu [trasa=" + trasa + ", sportowiec=" + sportowiec + ", super=" + super.toString() + "]";
    }
}
