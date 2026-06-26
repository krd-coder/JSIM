package duzeZadanie.osrodek;

import java.util.Arrays;

import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

public class Osrodek {

    private final Wezel[] wezly;

    private final Trasa[] trasy;

    private final Wyciag[] wyciagi;

    public Osrodek(Wezel[] wezly, Trasa[] trasy, Wyciag[] wyciagi) {
        this.wezly = wezly;
        this.trasy = trasy;
        this.wyciagi = wyciagi;
    }

    public Wezel[] wezly() {
        return wezly;
    }

    public Trasa[] trasy() {
        return trasy;
    }

    public Wyciag[] wyciagi() {
        return wyciagi;
    }

    @Override
    public String toString() {
        return "Osrodek [wezly=" + Arrays.toString(wezly) + ", trasy=" + Arrays.toString(trasy) + ", wyciagi="
            + Arrays.toString(wyciagi) + "]";
    }
}
