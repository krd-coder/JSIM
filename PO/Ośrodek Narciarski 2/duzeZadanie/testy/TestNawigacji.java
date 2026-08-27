package duzeZadanie.testy;

import duzeZadanie.BFS.NawigacjaBFS;
import duzeZadanie.czas.Interwal;
import duzeZadanie.osrodek.Osrodek;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.Trasa;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNawigacji {

    @Test
    public void testSciezkiGrafu() {
        Wezel[] Wezly = new Wezel[6]; 
        Wezly[0] = new Wezel(0, 60, 0, 0, false);
        Wezly[1] = new Wezel(1, 80, 0, 0, false);
        Wezly[2] = new Wezel(2, 70, 0, 0, false);
        Wezly[3] = new Wezel(3, 90, 0, 0, false);
        Wezly[4] = new Wezel(4, 80, 0, 0, false);
        Wezly[5] = new Wezel(5, 100, 0, 0, false);

        Trasa[] Trasy = new Trasa[7];
        Trasy[0] = new Trasa(0, Wezly[1], Wezly[0], new Interwal(1), 1, 1.0, 1.0);
        Trasy[1] = new Trasa(1, Wezly[1], Wezly[2], new Interwal(1), 1, 1.0, 1.0);
        Trasy[2] = new Trasa(2, Wezly[3], Wezly[1], new Interwal(1), 1, 1.0, 1.0);
        Trasy[3] = new Trasa(3, Wezly[3], Wezly[4], new Interwal(1), 1, 1.0, 1.0);
        Trasy[4] = new Trasa(4, Wezly[5], Wezly[3], new Interwal(1), 1, 1.0, 1.0);
        Trasy[5] = new Trasa(5, Wezly[5], Wezly[3], new Interwal(1), 1, 1.0, 1.0);
        Trasy[6] = new Trasa(6, Wezly[2], Wezly[0], new Interwal(1), 1, 1.0, 1.0);

        Wyciag[] Wyciagi = new Wyciag[4];
        Wyciagi[0] = new Wyciag(0, Wezly[0], Wezly[1], new Interwal(1), new Interwal(1), 1);
        Wyciagi[1] = new Wyciag(1, Wezly[2], Wezly[3], new Interwal(1), new Interwal(1), 1);
        Wyciagi[2] = new Wyciag(2, Wezly[2], Wezly[4], new Interwal(1), new Interwal(1), 1);
        Wyciagi[3] = new Wyciag(3, Wezly[4], Wezly[5], new Interwal(1), new Interwal(1), 1);

        Wezly[1].wychodzaceTrasy(new Trasa[]{Trasy[0], Trasy[1]} );
        Wezly[3].wychodzaceTrasy(new Trasa[]{Trasy[2], Trasy[3]} );
        Wezly[5].wychodzaceTrasy(new Trasa[]{Trasy[4], Trasy[5]} );
        Wezly[2].wychodzaceTrasy(new Trasa[]{Trasy[6]} );
        Wezly[0].wychodzaceWyciagi(new Wyciag[]{Wyciagi[0]});
        Wezly[2].wychodzaceWyciagi(new Wyciag[]{Wyciagi[1], Wyciagi[2]});
        Wezly[4].wychodzaceWyciagi(new Wyciag[]{Wyciagi[3]});

        Osrodek graf = new Osrodek(Wezly, Trasy, Wyciagi);
        NawigacjaBFS nawigacja = new NawigacjaBFS();

        // 1. Ścieżka 0 do 4: 0 -> 1 -> 2 -> 4, odległość 3 [cite: 503]
        assertEquals(3, nawigacja.obliczOdleglosc(Wezly[0], Wezly[4]));
        
        // 2. Bezpośrednia ścieżka z 3 do 1 [cite: 504]
        assertEquals(1, nawigacja.obliczOdleglosc(Wezly[3], Wezly[1]));
        
        // 3. Pusta ścieżka z 2 do 2 (odległość 0) [cite: 505]
        assertEquals(0, nawigacja.obliczOdleglosc(Wezly[2], Wezly[2]));
        
        // 4. Ścieżka z 4 do 3: długość 2 [cite: 506]
        assertEquals(2, nawigacja.obliczOdleglosc(Wezly[4], Wezly[3]));
    }
}