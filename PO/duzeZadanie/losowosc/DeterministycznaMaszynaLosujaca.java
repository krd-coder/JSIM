package duzeZadanie.losowosc;

import java.util.Random;

/**
 * Implementuje interfejs maszyny losującej korzystając z obiektu klasy Random z ustalonym ziarnem.
 * Dzięki temu "losowe" zachowanie łatwo jest reprodukować.
 */
public class DeterministycznaMaszynaLosujaca implements MaszynaLosujaca {

    private final Random random;

    public DeterministycznaMaszynaLosujaca(long ziarno) {
        this.random = new Random(ziarno);
    }

    @Override
    public int losowyInt(int a, int b) {
        assert a < b : String.format("Przedział losowania jest pusty: [%d, %d)", a, b);
        return random.nextInt(a, b);
    }

    @Override
    public double losowyDouble(double a, double b) {
        assert a < b : String.format("Przedział losowania jest pusty: [%f, %f)", a, b);
        return random.nextDouble(a, b);
    }
}
