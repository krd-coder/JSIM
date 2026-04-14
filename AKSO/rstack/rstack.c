#include "rstack.h"
#include <stdlib.h>
#include<stdbool.h>

#include <stdint.h>
#include <stddef.h>

// Deklaracja zapowiadająca (Forward declaration), żeby struktura elementu
// mogła używać wskaźnika na rstack_t, zanim sam rstack_t zostanie zdefiniowany.
typedef struct rstack rstack_t;

// 1. Tag (znacznik) informujący, co dokładnie leży w danym slocie na stosie.
typedef enum {
    TYPE_UINT64,
    TYPE_STACK
} elem_type_t;

// 2. Struktura pojedynczego elementu na stosie (Tagged Union).
typedef struct {
    rstack_elem_t* prev;       // Wskaźnik do poprzedniego elementu (stos jest trzymany jako lista jednokierunkowa)
    elem_type_t type;       // Przechowuje informację, którego pola z unii należy użyć
    union {
        uint64_t number;    // Wartość, jeśli element to liczba
        rstack_t* stack;    // Wskaźnik (referencja), jeśli element to inny stos
    } data;
} rstack_elem_t;

// 3. Właściwa struktura reprezentująca stos
typedef struct rstack {
    size_t ref_count;        // Licznik odwołań - najważniejsze pole z perspektywy pamięci!
    
    size_t size;             // Ile elementów jest aktualnie na stosie (indeks wierzchołka)
    size_t capacity;         // Rozmiar zaalokowanej tablicy (ile elementów się zmieści)
    rstack_elem_t* top_element; // Dynamiczna tablica z elementami stosu
}rstack_t;

rstack_t* rstack_new()
{
    rstack_t *rs = malloc(sizeof(rstack_t));
    if (rs == NULL)
        return NULL;
    // Inicjalizacja struktury rstack_t.
    rs->ref_count = 1;
    rs->size = 0;
    rs->capacity = 0;
    rs->top_element = NULL;
    return rs;
}

void rstack_delete(rstack_t *rs)
{
    if (rs == NULL)
        return;
    // Zmnniejszenie licznika odwołań. Jeśli osiągnie zero, to można bezpiecznie zwolnić pamięć.
    if (--rs->ref_count == 0) {
        // Najpierw trzeba zwolnić wszystkie elementy na stosie, które są stosami
        while (rs->top_element != NULL) {
            rstack_elem_t* elem = rs->top_element;
            rs->top_element = elem->prev;
            if (elem->type == TYPE_STACK) {
                rstack_delete(elem->data.stack);
            }
            free(elem);
        }
        
        // Teraz można zwolnić tablicę elementów i samą strukturę rstack_t
        free(rs->top_element);
        free(rs);
    }
}

int rstack_push_value(rstack_t *rs, uint64_t value)
{
    if (rs == NULL)
    {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli argument jest NULL
        return -1;
    }
    // Tworzymy nowy element na stosie
    rstack_elem_t* new_elem = malloc(sizeof(rstack_elem_t));
    if (new_elem == NULL)
    {
        errno = ENOMEM; // Ustawiamy errno na ENOMEM, jeśli alokacja pamięci się nie powiodła
        return -1;
    }    
    new_elem->type = TYPE_UINT64;
    new_elem->data.number = value;
    new_elem->prev = rs->top_element; // Nowy element wskazuje na poprzedni wierzchołek
    rs->top_element = new_elem; // Nowy element staje się wierzchołkiem stosu
    rs->size++; // Zwiększamy rozmiar stosu
    
    return 0; // Sukces
}

int rstack_push_rstack(rstack_t *rs1, rstack_t *rs2) {
    if (rs1 == NULL || rs2 == NULL)
    {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli któryś z argumentów jest NULL
        return -1;
    }
    // Tworzymy nowy element na stosie
    rstack_elem_t* new_elem = malloc(sizeof(rstack_elem_t));
    if (new_elem == NULL)
    {
        errno = ENOMEM; // Ustawiamy errno na ENOMEM, jeśli alokacja pamięci się nie powiodła
        return -1;
    }
    new_elem->type = TYPE_STACK;
    new_elem->data.stack = rs2;
    new_elem->prev = rs1->top_element; // Nowy element wskazuje na poprzedni wierzchołek
    rs1->top_element = new_elem; // Nowy element staje się wierzchołkiem stosu
    rs1->size++; // Zwiększamy rozmiar stosu

    return 0; // Sukces
}

void rstack_pop(rstack_t *rs)
{
    if (rs == NULL || rs->size == 0)
        return; // Nic nie robimy, jeśli stos jest pusty lub wskaźnik jest NULL
    
    rstack_elem_t* top_elem = rs->top_element; // Pobieramy wierzchołek stosu
    rs->top_element = top_elem->prev; // Przesuwamy wierzchołek na poprzedni element
    rs->size--; // Zmniejszamy rozmiar stosu
    
    if (top_elem->type == TYPE_STACK) 
    {
        rstack_delete(top_elem->data.stack); // Jeśli element to stos, usuwamy go rekurencyjnie
    }
    
    free(top_elem); // Zwolnienie pamięci zajmowanej przez element
}

// Pomocnicza struktura zawierająca historię odwiedzonych stosów, aby wykrywać cykle
typedef struct history_node 
{
    rstack_t *current_stack;
    struct history_node *prev;
} history_node_t;

//wewnętrzna funkcja rekurencyjna do sprawdzania, czy stos jest pusty, z wykrywaniem cykli
bool rstack_empty_recursive(rstack_t* rs, history_node_t* history)
{
    history_node_t *curr = history;
    while (curr != NULL) 
    {
        if (curr->current_stack == rs) return true; // Znaleźliśmy cykl, przerywamy gałąź
        curr = curr->prev;
    }

    // Dodajemy bieżący stos do historii
    history_node_t new_history = { .current_stack = rs, .prev = history };

    rstack_elem_t* elem = rs->top_element;
    while (elem != NULL) {
        if (elem->type == TYPE_UINT64) 
        {
            return false; // Znaleźliśmy liczbę - stos nie jest pusty
        } 
        else if (elem->type == TYPE_STACK) 
        {
            if (!rstack_empty_recursive(elem->data.stack, &new_history)) 
            {
                return false; // Znaleźliśmy niepusty stos, więc cały stos nie jest pusty
            }
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu
    }
    
}

bool rstack_empty(rstack_t *rs) {
    if (rs == NULL) return true;
    return check_empty_recursive(rs, NULL);
}

result_t rstack_front_recursive(rstack_t *rs, history_node_t *history)
{
    history_node_t *curr = history;
    while (curr != NULL) 
    {
        if (curr->current_stack == rs) 
        {
            result_t result = { .flag = false, .value = 0 }; // Znaleźliśmy cykl, zwracamy domyślny wynik
            return result;
        }
        curr = curr->prev;
    }

    // Dodajemy bieżący stos do historii
    history_node_t new_history = { .current_stack = rs, .prev = history };

    rstack_elem_t* elem = rs->top_element;
    while (elem != NULL) {
        if (elem->type == TYPE_UINT64) {
            result_t result = { .flag = true, .value = elem->data.number }; // Znaleźliśmy liczbę - ustawiamy flagę na true i zapisujemy wartość
            return result; // Zwracamy wynik
        } else if (elem->type == TYPE_STACK) {
            // Jeśli element to stos, sprawdzamy go rekurencyjnie
            result_t result = rstack_front_recursive(elem->data.stack, &new_history);
            if (result.flag) {
                return result; // Znaleźliśmy liczbę w zagnieżdżonym stosie, zwracamy wynik
            }
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu
    }
    
    result_t result = { .flag = false, .value = 0 }; // Jeśli nie znaleźliśmy żadnej liczby, zwracamy domyślny wynik
    return result;
}

result_t rstack_front(rstack_t *rs)
{
    result_t result = { .flag = false, .value = 0 }; // Domyślny wynik - stos jest pusty
    if (rs == NULL) return result; // Jeśli wskaźnik jest NULL, zwracamy domyślny wynik

    rstack_elem_t* elem = rs->top_element;
    while (elem != NULL) {
        if (elem->type == TYPE_UINT64) {
            result.flag = true; // Znaleźliśmy liczbę - ustawiamy flagę na true
            result.value = elem->data.number; // Zapisujemy wartość liczby
            return result; // Zwracamy wynik
        } else if (elem->type == TYPE_STACK) {
            // Jeśli element to stos, sprawdzamy go rekurencyjnie
            result = rstack_front(elem->data.stack);
            if (result.flag) {
                return result; // Znaleźliśmy liczbę w zagnieżdżonym stosie, zwracamy wynik
            }
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu
    }
    
    return result; // Jeśli nie znaleźliśmy żadnej liczby, zwracamy domyślny wynik
}