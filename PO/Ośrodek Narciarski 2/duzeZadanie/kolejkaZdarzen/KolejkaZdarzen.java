package duzeZadanie.kolejkaZdarzen;

import duzeZadanie.kolejkaZdarzen.zdarzenia.Zdarzenie;

/**
 * Reprezentuje kolejkę priorytetową zdarzeń posortowanych po momencie.
 * Kolejka jest stabilna: zdarzenia równe w porządku są zdejmowane w kolejności dodania do kolejki.
 */
public interface KolejkaZdarzen {

    void dodaj(Zdarzenie zdarzenie);

    Zdarzenie zdejmij();

    boolean czyPusta();
}
