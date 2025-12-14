#include "worki.h"

worek* table;
int id_counter = 0;

void initialize()
{
    *table = { -1, 0, nullptr, nullptr };
    table->my_location = new worek*;
    *table->my_location = table;
}

przedmiot *nowy_przedmiot()
{
    przedmiot *p = new przedmiot;
    p->location = table->my_location;
    return p;
}

worek *nowy_worek()
{
    worek *w = new worek;
    w->id = id_counter; // Assign unique ID as needed
    id_counter++;
    w->elements_count = 0;
    w->location = table->my_location;
    w->my_location = new worek*;
    *w->my_location = w;
    return w;
}

void wloz(przedmiot *co, worek *gdzie)
{
    co->location = gdzie->my_location;
    gdzie->elements_count++;
}

void wloz(worek *co, worek *gdzie)
{
    co->location = gdzie->my_location;
    gdzie->elements_count++;
}

void wyjmij(przedmiot *p)
{
    (*p->location)->elements_count--;
    p->location = table->my_location;
}

void wyjmij(worek *w)
{
    (*w->location)->elements_count-=w->elements_count + 1;
    w->location = table->my_location;
}

int w_ktorym_worku(przedmiot *p)
{
    return (p->location == &table) ? -1 : (*p->location)->id;
}

int w_ktorym_worku(worek *p)
{
    return (p->location == &table) ? -1 : (*p->location)->id;
}

int ile_przedmiotow(worek *w)
{
    return w->elements_count;
}

void na_odwrot(worek *w)
{
    *table->my_location = w;

    *w->my_location = table;
    table->my_location = w->my_location;

    w->location = w->my_location;
}

void gotowe()
{
    // Implement memory cleanup if necessary
}