#include "rstack.h"
#include <stdlib.h>
#include <stdbool.h>

#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <ctype.h>

// Deklaracja zapowiadająca (Forward declaration), żeby struktura elementu
// mogła używać wskaźnika na rstack_t, zanim sam rstack_t zostanie zdefiniowany.
typedef struct rstack rstack_t;

// 1. Tag (znacznik) informujący, co dokładnie leży w danym slocie na stosie.
typedef enum {
    TYPE_UINT64,
    TYPE_STACK
} elem_type_t;

// 2. Struktura pojedynczego elementu na stosie (Tagged Union).
typedef struct rstack_elem {
    struct rstack_elem* prev;       // Wskaźnik do poprzedniego elementu (stos jest trzymany jako lista jednokierunkowa)
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
    return true; // Nie znaleźliśmy żadnej liczby, więc stos jest pusty
}

bool rstack_empty(rstack_t *rs) {
    if (rs == NULL) return true;
    return rstack_empty_recursive(rs, NULL);
}

typedef struct result_depth 
{
    bool flag;  // To pole mówi, czy pole value zawiera wynik.
    uint64_t value; // W tym polu jest właściwy wynik.
    size_t depth; // Głębokość rekurencji, przy której znaleźliśmy wynik
} result_depth_t;

result_depth_t rstack_front_recursive(rstack_t *rs, history_node_t *history, size_t current_depth)
{
    history_node_t *curr = history;
    while (curr != NULL) 
    {
        if (curr->current_stack == rs) 
        {
            result_depth_t result = { .flag = false, .value = 0 }; // Znaleźliśmy cykl, zwracamy domyślny wynik
            return result;
        }
        curr = curr->prev;
    }

    // Dodajemy bieżący stos do historii
    history_node_t new_history = { .current_stack = rs, .prev = history };

    rstack_elem_t* elem = rs->top_element;
    result_depth_t best_result = { .flag = false, .value = 0, .depth = 0 }; // Najlepszy wynik znaleziony do tej pory
    while (elem != NULL) 
    {
        if (elem->type == TYPE_UINT64) 
        {
            result_depth_t result = { .flag = true, .value = elem->data.number, .depth = current_depth }; // Znaleźliśmy liczbę - ustawiamy flagę na true i zapisujemy wartość
            if(!best_result.flag || result.depth > best_result.depth) 
            {
                best_result = result; // Aktualizujemy najlepszy wynik, jeśli jest głębiej
            }
        } 
        else if (elem->type == TYPE_STACK) 
        {
            // Jeśli element to stos, sprawdzamy go rekurencyjnie
            result_depth_t result = rstack_front_recursive(elem->data.stack, &new_history, current_depth + 1);
            if(!best_result.flag || result.depth > best_result.depth) 
            {
                best_result = result; // Aktualizujemy najlepszy wynik, jeśli jest głębiej
            }
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu
    }
    
    return best_result; // Zwracamy najlepszy wynik znaleziony w tej gałęzi
}

result_t rstack_front(rstack_t *rs)
{
    if (rs == NULL) {
        result_t result = { .flag = false, .value = 0 }; // Jeśli stos jest NULL, zwracamy domyślny wynik
        return result;
    }   
    result_depth_t best_result = rstack_front_recursive(rs, NULL, 0);
    result_t final_result = { .flag = best_result.flag, .value = best_result.value }; // Przekształcamy wynik z result_depth_t na result_t, ignorując głębokość
    return final_result;
}

// Stany odczytu z pliku, które mogą wystąpić podczas próby odczytania liczby uint64_t z pliku tekstowego
typedef enum {
    READ_OK,     // Udało się odczytać poprawną liczbę
    READ_EOF,    // Koniec pliku (nie ma więcej liczb)
    READ_ERROR   // Błąd (zły znak, za duża liczba itp.)
} read_status_t;

read_status_t read_next_uint64(FILE *file, uint64_t *out_value) {
    int c;

    // 1. Zignoruj wszystkie białe znaki na początku
    do {
        c = fgetc(file);
    } while (c != EOF && isspace(c));

    // Jeśli po ominięciu spacji/enterów trafiliśmy na koniec pliku, to znaczy
    // że po prostu plik się skończył. To nie jest błąd.
    if (c == EOF) {
        return READ_EOF; 
    }

    // 2. Zbieraj znaki do bufora, aż trafisz na kolejny biały znak lub koniec pliku
    // Maksymalna wartość uint64_t ma 20 cyfr (18446744073709551615).
    // Bufor o wielkości 64 znaków jest więc aż nadto bezpieczny.
    char buffer[64];
    size_t idx = 0;

    while (c != EOF && !isspace(c)) {
        if (idx < sizeof(buffer) - 1) {
            buffer[idx++] = (char)c;
        } else {
            // Zabezpieczenie: "słowo" w pliku jest nienaturalnie długie, 
            // na pewno nie jest to poprawne uint64_t.
            errno = EINVAL; 
            return READ_ERROR;
        }
        c = fgetc(file);
    }

    buffer[idx] = '\0'; // Pamiętaj o znaku końca łańcucha!

    // 3. Konwersja bezpieczną funkcją strtoull
    char *endptr;
    errno = 0; // Zawsze zerujemy errno przed wywołaniem funkcji z <stdlib.h>
    unsigned long long val = strtoull(buffer, &endptr, 10);

    // 4. Dokładna weryfikacja poprawności (tego wymaga zadanie!)
    if (buffer == endptr) {
        // Nie było żadnych cyfr (np. w pliku było słowo "pies")
        errno = EINVAL;
        return READ_ERROR;
    }
    if (*endptr != '\0') {
        // Po cyfrach były inne znaki (np. "1234abc")
        errno = EINVAL;
        return READ_ERROR;
    }
    if (errno == ERANGE) {
        // Liczba składała się z samych cyfr, ale była za duża na 64 bity (Overflow)
        errno = EINVAL;
        return READ_ERROR;
    }

    // Wszystko poszło idealnie, zapisujemy wynik
    *out_value = (uint64_t)val;
    return READ_OK;
}

rstack_t* rstack_read(char const *path)
{
    if (path == NULL) {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli argument jest NULL
        return NULL;
    }

    FILE *file = fopen(path, "rb");
    if (file == NULL) 
    {
        return NULL;
    }

    rstack_t *rs = rstack_new();
    if (rs == NULL) {
        fclose(file);
        return NULL;
    }

    uint64_t value;
    read_status_t status;
    while ((status = read_next_uint64(file, &value)) == READ_OK) 
    {
        if (rstack_push_value(rs, value) != 0) 
        {
            rstack_delete(rs);
            fclose(file);
            return NULL;
        }
    }

    fclose(file);
    return rs;
}

// Pomocnicza funkcja do zapisu stosu rekurencyjnie
int write_stack_recursive(FILE *file, rstack_t *stack) 
{
    if (stack == NULL) return 0; // Nic do zapisania

    rstack_elem_t* elem = stack->top_element;
    while (elem != NULL) {
        if (elem->type == TYPE_UINT64) {
            // Zapisujemy liczbę do pliku
            if (fprintf(file, "%" PRIu64 "\n", elem->data.number) < 0) {
                return -1; // Błąd podczas zapisu
            }
        } else if (elem->type == TYPE_STACK) {
            // Rekurencyjnie zapisujemy zagnieżdżony stos
            if (write_stack_recursive(file, elem->data.stack) != 0) {
                return -1; // Błąd podczas zapisu
            }
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu
    }
    return 0; // Sukces
}

int rstack_write(char const *path, rstack_t *rs)
{
    if (path == NULL || rs == NULL) {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli któryś z argumentów jest NULL
        return -1;
    }

    FILE *file = fopen(path, "wb");
    if (file == NULL) {
        return -1;
    }

    int result = write_stack_recursive(file, rs);
    int saved_errno = errno; // Zapisujemy errno, bo fclose może je nadpisać
    fclose(file);
    errno = saved_errno; // Przywracamy errno do stanu sprzed fclose
    return result; // Zwracamy wynik operacji zapisu
}
