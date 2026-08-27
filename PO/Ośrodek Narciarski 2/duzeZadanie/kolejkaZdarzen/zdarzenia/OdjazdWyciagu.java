package duzeZadanie.kolejkaZdarzen.zdarzenia;

import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

public class OdjazdWyciagu extends Zdarzenie {

    private final Wyciag wyciag;

    public OdjazdWyciagu(Moment moment, Wyciag wyciag) {
        super(moment);
        this.wyciag = wyciag;
    }

    public Wyciag wyciag() {
        return wyciag;
    }

    @Override
    public Zdarzenie[] przetworz(Dziennik dziennik) {
        return wyciag.odjazd(moment, dziennik);
    }

    @Override
    public boolean czyPrzetwarzacPoZakonczeniuSymulacji() {
        return false;
    }

    @Override
    public String toString() {
        return "OdjazdWyciagu [wyciag=" + wyciag + ", super= " + super.toString() + "]";
    }
}
