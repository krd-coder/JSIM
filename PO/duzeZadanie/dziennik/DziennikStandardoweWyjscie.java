package duzeZadanie.dziennik;

import duzeZadanie.czas.Moment;
import duzeZadanie.sportowcy.Sportowiec;

public class DziennikStandardoweWyjscie implements Dziennik {

    @Override
    public void dodajWpis(Moment moment, String wpis) {
        System.out.printf("%s: %s%n", moment.toString(), wpis);
    }

    @Override
    public void dodajWpisZeSportowcem(Moment moment, Sportowiec sportowiec, String coZrobil) {
        if (sportowiec.sledzony()) {
            dodajWpis(moment, String.format("%s %s.", sportowiec, coZrobil));
        }
    }

    @Override
    public void dodajTabele(String[][] wiersze) {
        int maxDlugoscKlucza = 0;

        for (String[] wiersz : wiersze) {
            maxDlugoscKlucza = Math.max(maxDlugoscKlucza, wiersz[0].length());
        }

        String formatWiersza = "%-" + maxDlugoscKlucza + "s: %s%n";

        for (String[] wiersz : wiersze) {
            System.out.format(formatWiersza, wiersz[0], wiersz[1]);
        }
    }
}
