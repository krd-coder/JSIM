package duzeZadanie.losowosc;

public interface MaszynaLosujaca {

    /**
     * Daje losową liczbę całkowitą w przedziale [a, b).
     */
    int losowyInt(int a, int b);

    /**
     * Daje losową liczbę zmiennoprzecinkową w przedziale [a, b).
     */
    double losowyDouble(double a, double b);
}
