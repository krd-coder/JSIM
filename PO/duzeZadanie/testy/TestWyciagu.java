package duzeZadanie.testy;

import duzeZadanie.czas.Interwal;
import duzeZadanie.czas.Moment;
import duzeZadanie.dziennik.Dziennik;
import duzeZadanie.dziennik.DziennikStandardoweWyjscie;
import duzeZadanie.osrodek.Wezel;
import duzeZadanie.osrodek.krawedz.wyciag.Wyciag;
import duzeZadanie.sportowcy.Sportowiec;
import duzeZadanie.sportowcy.ZachlannySportowiec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestWyciagu {

    @Test
    public void test1() {
        // Scenariusz 1: Pojemność 3, pusta kolejka, 4 sportowców
        Wezel w0 = new Wezel(0,0, 0, 0, false);
        Wezel w1 = new Wezel(1,10, 0, 0, false);
        Wyciag w = new Wyciag(0, w0, w1, new Interwal(1), new Interwal(1), 3);
        w.dodajDoKolejki(new ZachlannySportowiec(0,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(1,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(2,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(3,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));

        w.odjazd(new Moment(0,0,1), new DziennikStandardoweWyjscie());

        assertEquals(1, w.dlugoscKolejki());
    }

    @Test
    public void test2() {
        // Scenariusz 1: Pojemność 3, pusta kolejka, 4 sportowców
        Wezel w0 = new Wezel(0,0, 0, 0, false);
        Wezel w1 = new Wezel(1,10, 0, 0, false);
        Wyciag w = new Wyciag(0, w0, w1, new Interwal(1), new Interwal(1), 3);
        w.dodajDoKolejki(new ZachlannySportowiec(0,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(1,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));

        w.odjazd(new Moment(0,0,1), new DziennikStandardoweWyjscie());

        assertEquals(0, w.dlugoscKolejki());
    }

    @Test
    public void test3() {
        // Scenariusz 1: Pojemność 3, pusta kolejka, 4 sportowców
        Wezel w0 = new Wezel(0,0, 0, 0, false);
        Wezel w1 = new Wezel(1,10, 0, 0, false);
        Wyciag w = new Wyciag(0, w0, w1, new Interwal(1), new Interwal(1), 3);
        w.dodajDoKolejki(new ZachlannySportowiec(0,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(1,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(1,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));
        w.dodajDoKolejki(new ZachlannySportowiec(1,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));

        w.odjazd(new Moment(0,0,1), new DziennikStandardoweWyjscie());
        w.dodajDoKolejki(new ZachlannySportowiec(1,1,1,1,1,1,1,false, w0,null,null, null), new Moment(0,0,0));


        assertEquals(4, w.maksymalnaDlugoscKolejki());
    }
}