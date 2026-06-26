package duzeZadanie.czas;

public class Interwal {

    private final int sekundy;

    public Interwal(int sekundy) {
        this.sekundy = sekundy;
    }

    public int sekundy() {
        return sekundy;
    }

    public Interwal dodajInterwal(Interwal i) {
        return new Interwal(sekundy + i.sekundy);
    }

    @Override
    public String toString() {
        return "Interwal [sekundy=" + sekundy + "]";
    }
}
