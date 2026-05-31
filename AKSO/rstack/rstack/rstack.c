#include "rstack.h"
#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <ctype.h>
#include <errno.h>

typedef struct rstack rstack_t;

// Typ elementu na stosie - może być albo liczbą uint64_t, albo wskaźnikiem do innego stosu.
typedef enum 
{
    TYPE_UINT64,
    TYPE_STACK
} 
elem_type_t;

// Struktura pojedynczego elementu na stosie. Każdy element może być albo liczbą uint64_t, albo wskaźnikiem do innego stosu. Stos jest trzymany jako lista dwukierunkowa.
typedef struct rstack_elem 
{
    struct rstack_elem* prev;       
    struct rstack_elem* next;       
    elem_type_t type;               
    union 
    {
        uint64_t number;            
        rstack_t* stack;            
    } 
    data;
} 
rstack_elem_t;

// Właściwa struktura reprezentująca stos
typedef struct rstack 
{
    size_t ref_count;               
    size_t size;                    
    size_t capacity;                
    rstack_elem_t* top_element;     
    rstack_elem_t* bottom_element;  
    bool marked;                    
} 
rstack_t;

static rstack_t* root_stack = NULL;
static bool is_initialized = false;

// Definicje 
int rstack_push_rstack(rstack_t *rs1, rstack_t *rs2);
void rstack_free(rstack_t *rs);
void gc();

// Funkcja pomocnicza do resetowania flagi marked dla wszystkich elementów w root_stack.
void reset_all_marks()
{
    if (root_stack == NULL) return;

    rstack_elem_t* elem = root_stack->top_element;
    while (elem != NULL)
    {
        if (elem->type == TYPE_STACK)
        {
            elem->data.stack->marked = false;
        }
        elem = elem->prev;
    }
}

// Funkcja tworząca nowy stos i inicjaluzująca root_stack.
rstack_t* rstack_new()
{
    if (!is_initialized)
    {
        root_stack = malloc(sizeof(rstack_t));
        if (root_stack == NULL) return NULL;
        root_stack->ref_count = 1; // Root stack jest zawsze używany, więc zaczynamy od 1
        root_stack->size = 0;
        root_stack->capacity = 0; 
        root_stack->top_element = NULL;
        root_stack->bottom_element = NULL;
        root_stack->marked = false; 
        is_initialized = true; 
    }

    rstack_t *rs = malloc(sizeof(rstack_t));
    if (rs == NULL)
        return NULL;

    // Inicjalizacja struktury rstack_t.
    rs->ref_count = 0; // Dodanie do root_stack zwiększy ref_count, więc zaczynamy od 0
    rs->size = 0;
    rs->capacity = 0;
    rs->top_element = NULL;
    rs->bottom_element = NULL;
    rs->marked = false; 

    if (rstack_push_rstack(root_stack, rs) != 0) // Dodajemy nowy stos do root_stack, żeby był śledzony przez GC
    {
        free(rs);
        return NULL;
    }

    return rs;
}

// Funkcja pomocnicza do GC. Rekurencyjnie oznacza wszystkie stosy, do których można dotrzeć z danego stosu, jako "marked = true".
void gc_mark_dfs(rstack_t* stack)
{
    if (stack == NULL) return; 
    if (stack->marked) return; 
    stack->marked = true; 

    rstack_elem_t* elem = stack->top_element;
    while (elem != NULL)
    {
        if (elem->type == TYPE_STACK)
        {
            rstack_t* nested_stack = elem->data.stack;
            if (!nested_stack->marked)
            {
                gc_mark_dfs(nested_stack); // Rekurencyjnie oznaczamy wszystko, do czego ten stos się odwołuje.
            }
        }
        elem = elem->prev; 
    }
}

// Dynamiczny Garbage Collector, który jest wywoływany po każdej operacji usuwania stosu.
void gc()
{
    if (root_stack == NULL) return;

    rstack_elem_t* elem = root_stack->top_element;

    // Przechodzimy przez wszystkie elementy root_stack, zmniejszając licznik odwołań dla wszystkich zagnieżdżonych stosów, 
    // bo teraz będziemy sprawdzać, które z nich są nadal używane przez użytkownika. 
    // Jednocześnie resetujemy flagę marked dla wszystkich elementów, bo zaczynamy nową rundę GC.
    while (elem != NULL)
    {
        elem->data.stack->marked = false; // Resetujemy flagę marked dla wszystkich elementów, bo zaczynamy nową rundę GC
        rstack_t* stack = elem->data.stack;
        rstack_elem_t* elem2 = stack->top_element;
        while (elem2 != NULL)
        {
            if (elem2->type == TYPE_STACK)
            {
                elem2->data.stack->ref_count--; // Zmniejszamy licznik odwołań dla zagnieżdżonych stosów.       
            }
            elem2 = elem2->prev; // Przechodzimy do kolejnego elementu.
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu.
    }

    // Oznaczamy elementy, do których można dotrzeć z elementów o niezerowym ref_count jako "marked = true". To znaczy, że są one nadal używane i nie powinny być usuwane.
    elem = root_stack->top_element;
    while (elem != NULL)
    {  
        if (elem->type == TYPE_STACK && elem->data.stack->ref_count > 0)
        {
            gc_mark_dfs(elem->data.stack); // Oznaczamy wszystko, do czego ten stos się odwołuje.
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu.
    }

    // Teraz usuwamy wszystkie elementy, które nie zostały oznaczone jako "marked = true", bo są one już niedostępne z root_stack i można bezpiecznie zwolnić ich pamięć.
    // Jednocześnie odzyskujemy ilość referencji, bo wcześniej zmniejszyliśmy je dla wszystkich, a teraz musimy przywrócić dla tych, które są nadal żywe.
    elem = root_stack->top_element;
    while (elem != NULL)
    {
        rstack_elem_t* prev_elem = elem->prev; // Zapisujemy wskaźnik na poprzedni element, bo możemy usunąć bieżący element.
        if (elem->type == TYPE_STACK)
        {            
            rstack_t* stack = elem->data.stack;
            if (stack->marked)
            {
                rstack_elem_t* elem2 = stack->top_element;
                while (elem2 != NULL)
                {
                    if (elem2->type == TYPE_STACK)
                    {
                        elem2->data.stack->ref_count++; // Zwiększamy licznik odwołań dla zagnieżdżonych stosów       
                    }
                    elem2 = elem2->prev; // Przechodzimy do kolejnego elementu.
                }
            }
            // Usuwamy element z listy dwukierunkowej root_stack.
            else
            {
                rstack_free(stack);
                free(stack);

                if (elem->next != NULL) elem->next->prev = elem->prev;
                else root_stack->top_element = elem->prev;

                if (elem->prev != NULL) elem->prev->next = elem->next;
                else root_stack->bottom_element = elem->next;

                free(elem); 
                root_stack->size--;
            }
        }
        elem = prev_elem; 
    }    

    if (root_stack->size == 0)
    {
        free(root_stack); 
        root_stack = NULL; 
        is_initialized = false; 
    }
}

//funkcja do usuwania jedynie stosu, bez usuwania zagnieżdżonych stosów, używana w GC, gdzie wszystkie stosy są zapisane w root_stack.
void rstack_free(rstack_t *rs)
{
    while (rs->top_element != NULL)
    {
        rstack_elem_t* elem = rs->top_element;
        rs->top_element = elem->prev;
        free(elem);
    }
}

// Funkcja do usuwania stosu, która jest wywoływana przez użytkownika. Zmniejsza licznik odwołań i wywołuje GC.
void rstack_delete(rstack_t *rs)
{
    if (rs == NULL) return;

    rs->ref_count--;
    gc();
}

// Funkcja do dodawania liczby uint64_t na stos. Tworzy nowy element, który jest dodawany na wierzchołek stosu. Zwiększa rozmiar stosu.
int rstack_push_value(rstack_t *rs, uint64_t value)
{
    if (rs == NULL)
    {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli argument jest NULL.
        return -1;
    }
    // Tworzymy nowy element na stosie.
    rstack_elem_t* new_elem = malloc(sizeof(rstack_elem_t));
    if (new_elem == NULL)
    {
        errno = ENOMEM; // Ustawiamy errno na ENOMEM, jeśli alokacja pamięci się nie powiodła.
        return -1;
    }    
    
    new_elem->type = TYPE_UINT64;
    new_elem->data.number = value;
    new_elem->prev = rs->top_element; // Nowy element wskazuje na poprzedni wierzchołek.
    new_elem->next = NULL; // Nowy element nie wskazuje na żaden następny element.
    
    if (rs->top_element != NULL) rs->top_element->next = new_elem; // Poprzedni wierzchołek wskazuje na nowy element.
    rs->top_element = new_elem; // Nowy element staje się wierzchołkiem stosu.
    if (rs->bottom_element == NULL) rs->bottom_element = new_elem; // Jeśli stos był pusty, nowy element staje się też dnem stosu.

    rs->size++; 
    return 0; 
}

// Funkcja do dodawania stosu rs2 na stos rs1. 
int rstack_push_rstack(rstack_t *rs1, rstack_t *rs2) {
    if (rs1 == NULL || rs2 == NULL)
    {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli któryś z argumentów jest NULL.
        return -1;
    }
    // Tworzymy nowy element na stosie
    rstack_elem_t* new_elem = malloc(sizeof(rstack_elem_t));
    if (new_elem == NULL)
    {
        errno = ENOMEM; // Ustawiamy errno na ENOMEM, jeśli alokacja pamięci się nie powiodła.
        return -1;
    }
    
    new_elem->type = TYPE_STACK;
    new_elem->data.stack = rs2;
    new_elem->prev = rs1->top_element; // Nowy element wskazuje na poprzedni wierzchołek.
    new_elem->next = NULL; // Nowy element nie wskazuje na żaden następny element.
    
    if (rs1->top_element != NULL) rs1->top_element->next = new_elem; // Poprzedni wierzchołek wskazuje na nowy element.
    rs1->top_element = new_elem; // Nowy element staje się wierzchołkiem stosu.    
    if (rs1->bottom_element == NULL) rs1->bottom_element = new_elem; // Jeśli stos był pusty, nowy element staje się też dnem stosu.
    
    rs1->size++; 
    rs2->ref_count++; // Zwiększamy licznik odwołań dla rs2, bo jest teraz częścią rs1

    return 0; 
}

// Funkcja do usuwania wierzchołka stosu. Zmniejsza rozmiar stosu i zwalnia pamięć zajmowaną przez wierzchołek.
void rstack_pop(rstack_t *rs)
{
    if (rs == NULL || rs->size == 0) return;
    
    rstack_elem_t* top_elem = rs->top_element;
    rs->top_element = top_elem->prev; // Przesuwamy wierzchołek w dół
    rs->size--;
        
    if (rs->top_element != NULL) rs->top_element->next = NULL; // Nowy wierzchołek nie ma już nic nad sobą. 
    else rs->bottom_element = NULL; // Jeśli stos jest pusty, dno też musi być NULL.

    if (top_elem->type == TYPE_STACK)
    {
        rstack_delete(top_elem->data.stack);
    }
    free(top_elem);
}

// Wewnętrzna funkcja rekurencyjna do sprawdzania, czy stos jest pusty, z wykrywaniem cykli.
bool rstack_empty_recursive(rstack_t* rs)
{
    if(rs == NULL) return true; // Pusty stos jest pusty.
    if (rs->marked) return true; // Znaleźliśmy cykl.
    rs->marked = true; // Oznaczamy ten stos jako odwiedzony, żeby wykrywać cykle.

    rstack_elem_t* elem = rs->top_element;
    while (elem != NULL) {
        if (elem->type == TYPE_UINT64)
        {
            return false; // Znaleźliśmy liczbę - stos nie jest pusty.
        }
        else if (elem->type == TYPE_STACK)
        {
            if (!rstack_empty_recursive(elem->data.stack)) return false; // Znaleźliśmy niepusty stos, więc cały stos nie jest pusty.
        }
        elem = elem->prev; // Przechodzimy do kolejnego elementu
    }
    return true; // Nie znaleźliśmy żadnej liczby, więc stos jest pusty.
}

// Funkcja do sprawdzania, czy stos jest pusty. Wywołuje funkcję rekurencyjną, która sprawdza wszystkie zagnieżdżone stosy, z wykrywaniem cykli.
bool rstack_empty(rstack_t *rs) {
    if (rs == NULL) return true;
    reset_all_marks();
    return rstack_empty_recursive(rs);
}

// Wewnętrzna funkcja rekurencyjna do odczytywania frontu stosu, z wykrywaniem cykli.
result_t rstack_front_recursive(rstack_t *rs)
{
    if (rs == NULL) return (result_t){ .flag = false, .value = 0 };
    if (rs->marked) return (result_t){ .flag = false, .value = 0 }; // Znaleźliśmy cykl, przerywamy gałąź
    rs->marked = true; // Oznaczamy ten stos jako odwiedzony, żeby wykrywać cykle

    rstack_elem_t* elem = rs->top_element;
    while (elem != NULL)
    {
        if (elem->type == TYPE_UINT64)
        {
            return (result_t){ .flag = true, .value = elem->data.number };
        }
        else if (elem->type == TYPE_STACK)
        {
            result_t res = rstack_front_recursive(elem->data.stack);
            if (res.flag == true) return res; 
        }
        elem = elem->prev;
    }
    
    return (result_t){ .flag = false, .value = 0 }; // Stos nie ma żadnych liczb
}

// Funkcja do odczytywania frontu stosu. Wywołuje funkcję rekurencyjną, która sprawdza wszystkie zagnieżdżone stosy, z wykrywaniem cykli.
result_t rstack_front(rstack_t *rs)
{
    if (rs == NULL) return (result_t){ .flag = false, .value = 0 };
    reset_all_marks(); 
    return rstack_front_recursive(rs);
}

// Stany odczytu z pliku, które mogą wystąpić podczas próby odczytania liczby uint64_t z pliku tekstowego.
typedef enum 
{
    READ_OK,     // Udało się odczytać poprawną liczbę.
    READ_EOF,    // Koniec pliku (nie ma więcej liczb).
    READ_ERROR   // Błąd (zły znak, za duża liczba itp.).
} 
read_status_t;

// Funkcja do odczytywania kolejnej liczby uint64_t z pliku tekstowego. Ignoruje białe znaki i odczytuje kolejne "słowo" jako liczbę. Zwraca status odczytu, a odczytaną wartość zapisuje w out_value.
read_status_t read_next_uint64(FILE *file, uint64_t *out_value) 
{
    int c;

    // Ignorujemy wszystkie białe znaki na początku
    do 
    {
        c = fgetc(file);
    } 
    while (c != EOF && isspace(c));

    // Jeśli po ominięciu spacji/enterów trafiliśmy na koniec pliku, co oznacza koniec pliku.
    if (c == EOF) return READ_EOF;

    // Zbieramy znaki do bufora, aż trafisz na kolejny biały znak lub koniec pliku.
    char buffer[64];
    size_t idx = 0;

    while (c != EOF && !isspace(c)) 
    {
        if (idx < sizeof(buffer) - 1) 
        {
            buffer[idx++] = (char)c;
        } 
        else {
            // Zabezpieczenie: "słowo" w pliku jest za długie.
            errno = EINVAL;
            return READ_ERROR;
        }
        c = fgetc(file);
    }

    buffer[idx] = '\0'; 

    // Odrzucanie ujemnych liczb.
    if (buffer[0] == '-') 
    {
        errno = EINVAL;
        return READ_ERROR;
    }

    // Konwersja bezpieczną funkcją strtoull
    char *endptr;
    errno = 0; // Zawsze zerujemy errno przed wywołaniem funkcji z <stdlib.h>.
    unsigned long long val = strtoull(buffer, &endptr, 10);

    // Dokładna weryfikacja poprawności.
    if (buffer == endptr) {
        // Nie było żadnych cyfr.
        errno = EINVAL;
        return READ_ERROR;
    }
    if (*endptr != '\0') {
        // Po cyfrach były inne znaki.
        errno = EINVAL;
        return READ_ERROR;
    }
    if (errno == ERANGE) {
        // Liczba składała się z samych cyfr, ale była za duża na 64 bity.
        errno = EINVAL;
        return READ_ERROR;
    }

    *out_value = (uint64_t)val;
    return READ_OK;
}

// Funkcja do odczytywania stosu z pliku tekstowego. Odczytuje kolejne liczby uint64_t i dodaje je na stos, aż do końca pliku. Zwraca NULL w przypadku błędu.
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

    if (status == READ_ERROR) {
        rstack_delete(rs);  
        fclose(file);
        return NULL;    
    }

    fclose(file);
    return rs;
}

// Pomocnicza funkcja do zapisu stosu rekurencyjnie
int write_stack_recursive(FILE *file, rstack_t *stack)
{
    if (stack == NULL) return 0;
    if (stack->marked) return 1; 
    stack->marked = true; // Oznaczamy ten stos jako odwiedzony, żeby wykrywać cykle

    rstack_elem_t* elem = stack->bottom_element;
    while (elem != NULL)
    {
        if (elem->type == TYPE_STACK)
        {
            int status = write_stack_recursive(file, elem->data.stack);
            if (status != 0) return status;
        }
        else if (elem->type == TYPE_UINT64)
        {
            if (fprintf(file, "%" PRIu64 "\n", elem->data.number) < 0)  return -1; 
        }
        elem = elem->next; 
    }
    stack->marked = false; // Po zakończeniu zapisu tego stosu, resetujemy flagę marked, bo teraz jest już bezpieczny do ponownego odwiedzenia w innych gałęziach
    return 0; 
}

// Funkcja do zapisywania stosu do pliku tekstowego. Zapisuje wszystkie liczby uint64_t z całej struktury stosu, w kolejności od dołu do góry, każdą liczbę w nowej linii. 
// Zwraca 0 w przypadku sukcesu, -1 w przypadku błędu. Jeśli wykryje cykl, przerywa zapis.
int rstack_write(char const *path, rstack_t *rs)
{
    reset_all_marks(); // Resetujemy flagę marked dla wszystkich elementów.

    if (path == NULL || rs == NULL) 
    {
        errno = EINVAL; // Ustawiamy errno na EINVAL, jeśli któryś z argumentów jest NULL.
        return -1;
    }

    FILE *file = fopen(path, "wb");
    if (file == NULL) return -1;
    
    int result = write_stack_recursive(file, rs);
    int saved_errno = errno; // Zapisujemy errno, bo fclose może je nadpisać.
    
    fclose(file);
    
    errno = saved_errno; // Przywracamy errno do stanu sprzed fclose.
    if (result == 1) result = 0; // Jeśli wykryliśmy cykl, to nie jest to błąd, tylko po prostu pomijamy go w zapisie, więc zwracamy.
    
    return result; // Zwracamy wynik operacji zapisu.
}