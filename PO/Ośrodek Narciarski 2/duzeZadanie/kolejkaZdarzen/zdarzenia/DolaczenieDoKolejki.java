package duzeZadanie.kolejkaZdarzen.zdarzenia;

import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.sportowcy.Sportowiec;

public class DolaczenieDoKolejki extends Zdarzenie {

    private final Wyciag wyciag;

    private final Sportowiec sportowiec;

    public DolaczenieDoKolejki(Moment moment, Wyciag wyciag, Sportowiec sportowiec) {
        super(moment);
        this.wyciag = wyciag;
        this.sportowiec = sportowiec;
    }

    public Wyciag wyciag() {
        return wyciag;
    }

    public Sportowiec sportowiec() {
        return sportowiec;
    }

    @Override
    public Zdarzenie[] przetworz(Dziennik dziennik) {
        dziennik
            .dodajWpisZeSportowcem(moment, sportowiec, String.format("dołączył do kolejki w %s", wyciag.toString()));

        wyciag.dodajDoKolejki(sportowiec, moment()); // Przekazujemy moment do metody dodajDoKolejki

        return new Zdarzenie[0];
    }

    @Override
    public boolean czyPrzetwarzacPoZakonczeniuSymulacji() {
        return false;
    }

    @Override
    public String toString() {
        return "DolaczenieDoKolejki [wyciag=" + wyciag + ", sportowiec=" + sportowiec + ", super=" + super.toString()
            + "]";
    }
}
