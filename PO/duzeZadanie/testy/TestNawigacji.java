import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;

public class TestWyciagu {

    @Test
    public void testPojemnoscIObciazenie() {
        // Scenariusz 1: Pojemność 3, pusta kolejka, 4 sportowców [cite: 487]
        Wyciag w = new Wyciag(3); 
        w.dodajSportowcow(4); 
        w.odjazd();
        
        assertEquals(3, w.liczbaPrzewiezionych()); // Sprawdzenie, czy odjechało 3 [cite: 488]
        assertEquals(3, w.getLicznikOdjazdow());   // Licznik odjazdów wzrósł o 3 [cite: 488]
    }

    @Test
    public void testDwochSportowcow() {
        // Scenariusz 2: Pojemność 3, ustawia się dwóch [cite: 489]
        Wyciag w = new Wyciag(3);
        w.dodajSportowcow(2);
        w.odjazd();
        
        assertEquals(2, w.liczbaPrzewiezionych()); // Sprawdzenie czy obydwaj odjechali [cite: 489]
    }

    @Test
    public void testMaksymalnejDlugosciKolejki() {
        // Scenariusz 3: 4 sportowców, odjazd, potem 1 nowy [cite: 490]
        Wyciag w = new Wyciag(3);
        w.dodajSportowcow(4);
        w.odjazd();
        w.dodajSportowcow(1);
        
        assertEquals(4, w.getMaxDlugoscKolejki()); // Sprawdzenie maks. długości [cite: 491]
    }
}