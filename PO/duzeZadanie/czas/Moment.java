package duzeZadanie.czas;

import java.util.Objects;

public class Moment {

    private static final int LICZBA_SEKUND_W_MINUCIE = 60;

    private static final int LICZBA_SEKUND_W_GODZINIE = 3600;

    private final int godzina;

    private final int minuta;

    private final int sekunda;

    public Moment(int godzina, int minuta, int sekunda) {
        this.godzina = godzina;
        this.minuta = minuta;
        this.sekunda = sekunda;
    }

    public int godzina() {
        return godzina;
    }

    public int minuta() {
        return minuta;
    }

    public int sekunda() {
        return sekunda;
    }

    public Moment dodajInterwal(Interwal interwal) {
        return zSekund(sekundy() + interwal.sekundy());
    }

    public boolean wczesniejNiz(Moment moment) {
        return compareTo(moment) < 0;
    }

    public long odlegloscWSekundach(Moment inny) {
    long tenCzas = this.godzina * 3600L + this.minuta * 60L + this.sekunda;
    long innyCzas = inny.godzina * 3600L + inny.minuta * 60L + inny.sekunda;
    return Math.abs(tenCzas - innyCzas);
    }


    private int sekundy() {
        return LICZBA_SEKUND_W_GODZINIE * godzina + LICZBA_SEKUND_W_MINUCIE * minuta + sekunda;
    }

    private static Moment zSekund(int sekundy) {
        int godzina = sekundy / LICZBA_SEKUND_W_GODZINIE;
        sekundy %= LICZBA_SEKUND_W_GODZINIE;
        int minuta = sekundy / LICZBA_SEKUND_W_MINUCIE;
        sekundy %= LICZBA_SEKUND_W_MINUCIE;

        return new Moment(godzina, minuta, sekundy);
    }

    public int compareTo(Moment moment) {
        if (godzina != moment.godzina) {
            return Integer.compare(godzina, moment.godzina);
        } else if (minuta != moment.minuta) {
            return Integer.compare(minuta, moment.minuta);
        } else {
            return Integer.compare(sekunda, moment.sekunda);
        }
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d", godzina, minuta, sekunda);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Moment moment) {
            return godzina == moment.godzina && minuta == moment.minuta && sekunda == moment.sekunda;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(godzina, minuta, sekunda);
    }
}
