#include "worki.h"

worek* table;
int id_counter = 0;
bool init = false;

worek* prev_w;
przedmiot* prev_p;

void initialize()
{
    table = new worek;

    table->elements_count = 0;
    table->id = -1;
    table->my_location = new (worek*);
    *table->my_location = table;
    table->prev=nullptr;
    
    prev_w = table;
    prev_p = nullptr;

    init = true;
}

przedmiot *nowy_przedmiot()
{
    if(!init)initialize();
    przedmiot *p = new przedmiot;
    p->location = table->my_location;
    table->elements_count++;
    p->prev=prev_p;
    prev_p=p;
    return p;
}

worek *nowy_worek()
{
    if(!init)initialize();
    worek *w = new worek;
    w->id = id_counter;
    id_counter++;
    w->elements_count=0;
    w->location = table->my_location;
    w->my_location = new (worek*);
    *w->my_location = w;
    w->prev=prev_w;
    prev_w=w;
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
    gdzie->elements_count+=co->elements_count;
}

void wyjmij(przedmiot *p)
{
    (*p->location)->elements_count--;
    p->location = table->my_location;
}

void wyjmij(worek *w)
{
    (*w->location)->elements_count-=w->elements_count;
    w->location = table->my_location;
}

int w_ktorym_worku(przedmiot *p)
{
    return (*p->location)->id;
}

int w_ktorym_worku(worek *p)
{
    return (*p->location)->id;
}

int ile_przedmiotow(worek *w)
{
    return w->elements_count;
}

void na_odwrot(worek *w)
{
    worek **tmp = table->my_location;
    *table->my_location = w;

    *w->my_location = table;
    table->my_location = w->my_location;

    w->location = w->my_location;

    w->my_location = tmp;
    w->elements_count = table->elements_count - w->elements_count;
}

void gotowe()
{
    while (prev_w!=nullptr)
    {
        worek* curr = prev_w;
        prev_w = prev_w->prev;
        delete curr->my_location;
        delete curr;
    }
    while (prev_p!=nullptr)
    {
        przedmiot* curr = prev_p;
        prev_p = prev_p->prev;
        delete curr;
    }
}