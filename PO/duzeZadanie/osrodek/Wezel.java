package duzeZadanie.osrodek;

import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

public class Wezel {

    private final int id;

    private final int wysokosc;

    private final boolean czyStartowy;

    private Trasa[] wychodzaceTrasy = {};

    private Wyciag[] wychodzaceWyciagi = {};

    private final int wspolrzednaX;

    private final int wspolrzednaY;

    public Wezel(int id, int wysokosc, int wspolrzednaX, int wspolrzednaY, boolean czyStartowy) {
        this.id = id;
        this.wysokosc = wysokosc;
        this.wspolrzednaX = wspolrzednaX;
        this.wspolrzednaY = wspolrzednaY;
        this.czyStartowy = czyStartowy;
    }

    public int wysokosc() {
        return wysokosc;
    }

    public int wspolrzednaX() {
        return wspolrzednaX;
    }

    public int wspolrzednaY() {
        return wspolrzednaY;
    }

    public boolean czyStartowy() {
        return czyStartowy;
    }

    public Trasa[] wychodzaceTrasy() {
        return wychodzaceTrasy;
    }

    public Wyciag[] wychodzaceWyciagi() {
        return wychodzaceWyciagi;
    }

    public void wychodzaceTrasy(Trasa[] wychodzaceTrasy) {
        this.wychodzaceTrasy = wychodzaceTrasy;
    }

    public void wychodzaceWyciagi(Wyciag[] wychodzaceWyciagi) {
        this.wychodzaceWyciagi = wychodzaceWyciagi;
    }

    @Override
    public String toString() {
        return String.format("Węzeł nr %d", id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Wezel wezel) {
            return id == wezel.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
