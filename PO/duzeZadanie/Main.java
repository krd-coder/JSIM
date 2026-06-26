package duzeZadanie;

import java.util.Locale;
import java.util.Scanner;

import duzeZadanie.dziennik.DziennikStandardoweWyjscie;
import duzeZadanie.kolejkaZdarzen.BibliotecznaKolejkaZdarzen;
import duzeZadanie.losowosc.DeterministycznaMaszynaLosujaca;
import duzeZadanie.symulacja.Symulacja;
import duzeZadanie.wczytywacz.DaneWejsciowe;
import duzeZadanie.wczytywacz.Wczytywacz;

public class Main {

    public static void main(String[] args) {
        DaneWejsciowe daneWejsciowe = wczytajWejscie();

        new Symulacja().przeprowadzSymulacje(new DziennikStandardoweWyjscie(),
            new BibliotecznaKolejkaZdarzen(),
            daneWejsciowe.osrodek(),
            daneWejsciowe.sportowcy(), args.length > 0 ? args[0] : "mapki");
    }

    private static DaneWejsciowe wczytajWejscie() {
        Scanner scanner = new Scanner(System.in);

        // Ustawiamy region na angielski żeby Scanner parsował liczby
        // zmiennoprzecinkowe z '.' zamiast ','.
        scanner.useLocale(Locale.ENGLISH);

        Wczytywacz wczytywacz = new Wczytywacz(scanner, new DeterministycznaMaszynaLosujaca(0));
        return wczytywacz.wczytajWejscie();
    }
}
