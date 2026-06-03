default rel

; ==============================================================================
; Definicje stałych i wywołań systemowych
; ==============================================================================
%define SYS_READ      0
%define SYS_WRITE     1
%define SYS_MMAP      9
%define SYS_MUNMAP    11
%define SYS_MREMAP    25
%define SYS_EXIT      60

%define PROT_READ     1
%define PROT_WRITE    2
%define MAP_PRIVATE   2
%define MAP_ANONYMOUS 0x20
%define MREMAP_MAYMOVE 1

%define IO_BUF_SIZE   65536
%define INIT_CAPACITY 4096

; ==============================================================================
; Sekcja zmiennych niezainicjowanych (BSS)
; ==============================================================================
section .bss
    ; Bufory i metadane do zarządzania pamięcią
    axiom_buf       resq 1      ; Wskaźnik na bufor z obecnym napisem
    axiom_cap       resq 1      ; Zarezerwowana pojemność (w bajtach)
    axiom_len       resq 1      ; Długość obecnego napisu

    rule_buf        resq 1      ; Wskaźnik na bufor przechowujący ciągi zamienników
    rule_cap        resq 1      ; Pojemność bufora reguł
    rule_len        resq 1      ; Zajęte miejsce w buforze reguł

    ; Tablice dla reguł poszczególnych znaków (256 kodów ASCII)
    rule_offsets    resq 256    ; Przesunięcia wewnątrz rule_buf dla danego znaku
    rule_lens       resq 256    ; Długości ciągów zamieniających dla danego znaku
    rule_has        resb 256    ; Flagi oznaczające zdefiniowanie reguły (0 lub 1)

    ; Bufory dla "nowej generacji" reguł podczas ich potęgowania
    next_rule_buf       resq 1
    next_rule_cap       resq 1
    next_rule_len       resq 1
    next_rule_offsets   resq 256
    next_rule_lens      resq 256

    ; Bufor i wskaźniki dla czytania ze standardowego wejścia
    io_buf          resb IO_BUF_SIZE
    io_buf_ptr      resq 1
    io_buf_len      resq 1

; ==============================================================================
; Sekcja danych zainicjowanych
; ==============================================================================
section .data
    newline_char    db 10       ; Znak nowej linii do wypisania na końcu

; ==============================================================================
; Sekcja kodu
; ==============================================================================
section .text
    global _start

_start:
    ; 1. Weryfikacja liczby argumentów programu (argc)
    mov     rdi, [rsp]                  ; argc
    cmp     rdi, 2                      ; Oczekujemy dokładnie 2 argumentów (nazwa + parametr n)
    jne     error_exit

    ; 2. Parsowanie argumentu n (liczba iteracji z argv[1])
    mov     rdi, [rsp + 16]             ; argv[1]
    xor     r12, r12                    ; r12 = liczba iteracji (n)
    xor     rcx, rcx                    ; rcx = offset w napisie
.parse_loop:
    movzx   eax, byte [rdi + rcx]       ; Pobranie znaku
    test    eax, eax                    ; Sprawdzenie końca napisu (NULL)
    jz      .parse_done
    cmp     eax, '0'                    ; Walidacja cyfr (tylko 0-9)
    jl      error_exit
    cmp     eax, '9'
    jg      error_exit
    sub     eax, '0'                    ; Konwersja znaku ASCII na wartość
    imul    r12, 10                     ; r12 = r12 * 10
    add     r12, rax                    ; r12 = r12 + cyfra
    mov     r15, 0xFFFFFFFF             ; Maksymalna wartość to 2^32 - 1
    cmp     r12, r15
    ja      error_exit                  ; Błąd, jeśli przekroczono zakres
    inc     rcx
    jmp     .parse_loop
.parse_done:
    test    rcx, rcx                    ; Błąd, jeśli argument to pusty string
    jz      error_exit

    ; 3. Inicjalizacja buforów w pamięci
    mov     rdi, INIT_CAPACITY
    call    mmap_alloc
    mov     [rel axiom_buf], rax
    mov     qword [rel axiom_cap], INIT_CAPACITY
    mov     qword [rel axiom_len], 0

    mov     rdi, INIT_CAPACITY
    call    mmap_alloc
    mov     [rel rule_buf], rax
    mov     qword [rel rule_cap], INIT_CAPACITY
    mov     qword [rel rule_len], 0

    mov     rdi, INIT_CAPACITY
    call    mmap_alloc
    mov     [rel next_rule_buf], rax
    mov     qword [rel next_rule_cap], INIT_CAPACITY
    mov     qword [rel next_rule_len], 0

    ; 4. Wczytanie pierwszej linii (aksjomat - napis początkowy)
.read_axiom_loop:
    call    get_char
    jc      error_exit                  ; EOF przed znakiem \n jest błędem (nawet na początku)
    cmp     al, 10                      ; Koniec linii
    je      .axiom_done
    cmp     al, 33                      ; Sprawdzenie, czy to dozwolony symbol (ASCII 33-126)
    jl      error_exit
    cmp     al, 126
    jg      error_exit
    
    ; Zapis znaku z realokacją w razie potrzeby
    mov     rdi, [rel axiom_buf]
    mov     rsi, [rel axiom_len]
    mov     rdx, [rel axiom_cap]
    cmp     rsi, rdx
    jl      .no_axiom_realloc
    shl     rdx, 1                      ; Podwojenie pojemności bufora
    call    do_mremap
    mov     [rel axiom_buf], rax
    mov     [rel axiom_cap], rdx
    mov     rdi, rax
.no_axiom_realloc:
    mov     byte [rdi + rsi], al        ; Dodanie znaku na końcu bufora
    inc     qword [rel axiom_len]
    jmp     .read_axiom_loop
.axiom_done:

    ; 5. Wczytywanie kolejnych linii (reguły zastępowania)
.read_rules_loop:
    call    get_char
    jc      .rules_eof                  ; EOF w miejscu oczekiwanego nowego symbolu to poprawny koniec
    cmp     al, 10
    je      error_exit                  ; Pusta linia lub nadmiarowy enter to błąd
    cmp     al, 33                      ; Sprawdzenie poprawności symbolu docelowego
    jl      error_exit
    cmp     al, 126
    jg      error_exit

    movzx   rbx, al                     ; rbx = kod ASCII symbolu zastępowanego
    lea     rdx, [rel rule_has]
    cmp     byte [rdx + rbx], 1         ; Sprawdzenie czy symbol nie posiada już reguły
    je      error_exit
    mov     byte [rdx + rbx], 1         ; Zaznaczenie istnienia reguły

    lea     rdx, [rel rule_offsets]
    mov     rcx, [rel rule_len]         ; Aktualny koniec bufora reguł to początek nowej
    mov     [rdx + rbx * 8], rcx

    lea     rdx, [rel rule_lens]
    mov     qword [rdx + rbx * 8], 0    ; Inicjalizacja długości dla danej reguły na 0

.read_rule_body_loop:
    call    get_char
    jc      error_exit                  ; Nieoczekiwany EOF przed \n w trakcie wczytywania reguły
    cmp     al, 10                      ; Koniec linii
    je      .rule_done
    cmp     al, 33                      ; Walidacja znaków w regule
    jl      error_exit
    cmp     al, 126
    jg      error_exit

    ; Zapis znaku zastępującego do bufora z ewentualną realokacją
    mov     rdi, [rel rule_buf]
    mov     rsi, [rel rule_len]
    mov     rdx, [rel rule_cap]
    cmp     rsi, rdx
    jl      .no_rule_realloc
    push    rbx                         ; Zachowanie kodu znaku aktualnej reguły
    shl     rdx, 1                      ; Nowy rozmiar: rdx = stary * 2
    call    do_mremap
    mov     [rel rule_buf], rax
    mov     [rel rule_cap], rdx
    mov     rdi, rax
    pop     rbx
.no_rule_realloc:
    mov     byte [rdi + rsi], al        ; Dopisanie znaku
    inc     qword [rel rule_len]
    lea     rdx, [rel rule_lens]
    inc     qword [rdx + rbx * 8]       ; Inkrementacja długości ciągu dla danej reguły
    jmp     .read_rule_body_loop
.rule_done:
    jmp     .read_rules_loop
.rules_eof:
    ; 6. Uzupełnienie reguł domyślnych dla znaków bez określonej reguły (znak przechodzi na samego siebie)
    xor     rbx, rbx                    ; Pętla po ASCII 0-255
.def_loop:
    lea     rdx, [rel rule_has]
    cmp     byte [rdx + rbx], 0
    jne     .def_next                   ; Pomijamy, jeśli ma już regułę
    
    ; Wstaw domyślny znak na koniec bufora reguł
    mov     rdi, [rel rule_buf]
    mov     rsi, [rel rule_len]
    mov     rdx, [rel rule_cap]
    cmp     rsi, rdx
    jl      .def_no_realloc
    push    rbx
    shl     rdx, 1
    call    do_mremap
    mov     [rel rule_buf], rax
    mov     [rel rule_cap], rdx
    mov     rdi, rax
    pop     rbx
.def_no_realloc:
    mov     byte [rdi + rsi], bl        ; Zapisanie samego znaku
    lea     rdx, [rel rule_offsets]
    mov     [rdx + rbx * 8], rsi        ; Przesunięcie do znaku
    lea     rdx, [rel rule_lens]
    mov     qword [rdx + rbx * 8], 1    ; Długość reguły to 1
    inc     qword [rel rule_len]
.def_next:
    inc     rbx
    cmp     rbx, 256
    jl      .def_loop








    ; 7. Generowanie zadanego stopnia L-systemu (iteracje)
.iter_loop:
    shr     r12, 1                      ; Dekrementacja licznika iteracji
    jnc     .build_next_generation

    mov     r8, [rel axiom_len]
    test    r8, r8
    jz      .output                     ; Pusty string już nie urośnie - optymalizacja

    ; 7a. Wyliczenie niezbędnego rozmiaru pamięci na nowy napis
    xor     r9, r9                      ; r9 = przewidywana nowa długość
    xor     rcx, rcx                    ; i = 0
    mov     r10, [rel axiom_buf]
.calc_loop:
    cmp     rcx, r8
    jge     .calc_done
    movzx   eax, byte [r10 + rcx]       ; Pobranie oryginalnego znaku
    lea     rdx, [rel rule_lens]
    add     r9, [rdx + rax * 8]         ; Dodanie długości jego następnika
    jc      error_exit                  ; Overflow pamięci operacyjnej - awaria
    inc     rcx
    jmp     .calc_loop
.calc_done:

    ; 7b. Alokacja idealnie dopasowanego bufora dla nowej iteracji (new_buf)
    test    r9, r9
    jz      .empty_new_buf
    mov     rdi, r9                     ; Nowy rozmiar (w rdi)
    call    mmap_alloc
    mov     r11, rax                    ; r11 = wskaźnik na nowy bufor
    jmp     .do_copy
.empty_new_buf:
    xor     r11, r11

    ; 7c. Przepisanie przekształconych znaków do new_buf
.do_copy:
    xor     rcx, rcx                    ; Index w starej pamięci
    xor     r13, r13                    ; Dst_offset (wskaźnik wpisujący) w nowej pamięci
.copy_loop:
    cmp     rcx, r8
    jge     .copy_done
    movzx   eax, byte [r10 + rcx]
    lea     rdx, [rel rule_lens]
    mov     r14, [rdx + rax * 8]        ; r14 = długość do wstawienia
    test    r14, r14
    jz      .copy_next                  ; Zastąpienie zerowym ciągiem -> idziemy dalej

    ; Szybki memcpy (rep movsb)
    lea     rdi, [r11 + r13]            ; dst = new_buf + offset
    mov     rsi, [rel rule_buf]
    lea     rdx, [rel rule_offsets]
    add     rsi, [rdx + rax * 8]        ; src = rule_buf + target_offset
    push    rcx                         ; Zachowaj zmienne pętli (rcx nadpisane przez rep)
    mov     rcx, r14                    ; Zliczanie bajtów
    rep     movsb                       ; Kopiuj rsi -> rdi
    pop     rcx

    add     r13, r14                    ; Aktualizacja offsetu wpisywania
.copy_next:
    inc     rcx
    jmp     .copy_loop
.copy_done:

    ; 7d. Czyszczenie starego bufora iteracyjnego i zastępowanie wskaźników
    mov     rdi, [rel axiom_buf]
    mov     rsi, [rel axiom_cap]
    call    do_munmap
    
    mov     [rel axiom_buf], r11
    mov     [rel axiom_len], r9
    mov     [rel axiom_cap], r9         ; Ustawienie nowej zdolności jako r9





; ==============================================================================
; Pętla wyliczająca reguły na kolejną potęgę (2^(i+1))
; Wejście: rule_buf, rule_offsets, rule_lens (reguły dla 2^i)
; Wyjście: next_rule_buf, next_rule_offsets, next_rule_lens
; ==============================================================================

.build_next_generation:
    test    r12, r12
    jz      .output                     ; Brak iteracji - natychmiastowe wypisanie
    xor     rbx, rbx                    ; rbx = aktualnie rozpatrywany znak ASCII (0-255)
    mov     qword [rel next_rule_len], 0 ; Wyzerowanie długości nowego bufora reguł

.char_loop:
    cmp     rbx, 256
    jge     .char_loop_done

    ; 1. Zapisz przesunięcie początkowe dla tego znaku w nowej generacji
    lea     rdx, [rel next_rule_offsets]
    mov     rax, [rel next_rule_len]
    mov     [rdx + rbx * 8], rax

    ; 2. Wyzeruj licznik nowej długości dla tego znaku
    lea     rdx, [rel next_rule_lens]
    mov     qword [rdx + rbx * 8], 0

    ; 3. Pobierz informacje o STAREJ regule dla znaku (rbx)
    lea     rdx, [rel rule_offsets]
    mov     rsi, [rdx + rbx * 8]        ; rsi = offset w rule_buf
    lea     rdx, [rel rule_lens]
    mov     r8, [rdx + rbx * 8]         ; r8 = stara długość reguły

    xor     rcx, rcx                    ; rcx = licznik znaków w starej regule
.expand_loop:
    cmp     rcx, r8
    jge     .expand_done

    ; 4. Pobierz znak składowy ze starej reguły
    mov     rdi, [rel rule_buf]
    add     rdi, rsi                    ; Dodajemy offset bezpośrednio do bazy (rdi = rule_buf + offset)
    movzx   eax, byte [rdi + rcx]       ; Teraz w nawiasach są tylko 2 rejestry - wszystko gra!

    ; 5. Sprawdź, na co TEN znak przechodzi w obecnej generacji (Rule Composition)
    lea     rdx, [rel rule_lens]
    mov     r9, [rdx + rax * 8]         ; r9 = długość docelowa
    test    r9, r9
    jz      .skip_copy                  ; Jeśli zamienia się na puste, pomiń



; --- POCZĄTEK ZARZĄDZANIA POJEMNOŚCIĄ ---
    push    rax                         ; ZABEZPIECZ KOD ASCII!
    
    ; Sprawdzamy, czy nowa długość całkowita zmieści się w buforze
    mov     rax, [rel next_rule_len]
    add     rax, r9                     ; rax = przewidywana długość
    mov     rdi, [rel next_rule_cap]
    cmp     rax, rdi
    jle     .capacity_ok                ; Jeśli się mieści, pomiń realokację

.realloc_loop:
    shl     rdi, 1                      ; Podwajamy pojemność (rdi = rdi * 2)
    cmp     rdi, rax
    jl      .realloc_loop               ; Powtarzaj podwajanie, aż pomieści

    ; Zabezpieczamy resztę rejestrów pętli
    push    rcx
    push    rsi
    push    r9
    push    rdi                         ; push nowej pojemności

    ; Wywołanie systemowe powiększenia pamięci (mremap)
    mov     rdx, rdi                    ; rdx = nowa pojemność
    mov     rdi, [rel next_rule_buf]    ; rdi = stary wskaźnik
    mov     rsi, [rel next_rule_cap]    ; rsi = stara pojemność
    call    do_mremap                   ; RAX zwróci nowy wskaźnik

    ; Aktualizujemy wskaźniki i metadane po udanym rozszerzeniu
    mov     [rel next_rule_buf], rax
    pop     qword [rel next_rule_cap]   ; Ściągamy ze stosu bezpośrednio do zmiennej
    pop     r9
    pop     rsi
    pop     rcx

.capacity_ok:
    pop     rax                         ; PRZYWRÓĆ KOD ASCII DO RAX!
    ; --- KONIEC ZARZĄDZANIA POJEMNOŚCIĄ ---



    ; 6. Skopiuj rozwinięcie tego znaku do nowego bufora reguł (rep movsb)
    push    rsi                         ; Zabezpiecz stare wskaźniki
    push    rcx
    
    mov     rdi, [rel next_rule_buf]
    add     rdi, [rel next_rule_len]    ; rdi = docelowe miejsce zapisu
    
    mov     rsi, [rel rule_buf]
    lea     rdx, [rel rule_offsets]
    add     rsi, [rdx + rax * 8]        ; rsi = skąd kopiujemy rozwinięcie
    
    mov     rcx, r9                     ; rcx = ile bajtów kopiujemy
    rep     movsb
    
    pop     rcx                         ; Przywróć stare wskaźniki
    pop     rsi

    ; 7. Zaktualizuj liczniki
    add     [rel next_rule_len], r9     ; Aktualizuj globalne zużycie bufora
    lea     rdx, [rel next_rule_lens]
    add     [rdx + rbx * 8], r9         ; Aktualizuj długość reguły dla rozpatrywanego (rbx) znaku

.skip_copy:
    inc     rcx
    jmp     .expand_loop
.expand_done:

    inc     rbx
    jmp     .char_loop
.char_loop_done:

; ==============================================================================
; Sekcja zamiany generacji (Swapping & Metadata Copy)
; Przenosi dane z 'next_rule' do głównych struktur 'rule'
; ==============================================================================
.swap_generations:
    ; 1. Zamiana wskaźników buforów (rule_buf <-> next_rule_buf)
    ; Używamy rejestru RAX jako tymczasowego pośrednika
    mov     rax, [rel rule_buf]
    mov     rbx, [rel next_rule_buf]
    mov     [rel rule_buf], rbx
    mov     [rel next_rule_buf], rax

    ; 2. Zamiana pojemności buforów (rule_cap <-> next_rule_cap)
    ; Dzięki temu zarządca pamięci mremap będzie dokładnie wiedział, ile ma miejsca
    mov     rax, [rel rule_cap]
    mov     rbx, [rel next_rule_cap]
    mov     [rel rule_cap], rbx
    mov     [rel next_rule_cap], rax

    ; 3. Przepisanie nowej długości bufora reguł
    mov     rax, [rel next_rule_len]
    mov     [rel rule_len], rax

    ; 4. Szybkie kopiowanie tablicy offsetów (256 elementów * 8 bajtów = 2048 bajtów)
    ; Wykorzystujemy rep movsq (kopiowanie 64-bitowych Quadwordów)
    lea     rdi, [rel rule_offsets]         ; Cel: stara tablica
    lea     rsi, [rel next_rule_offsets]    ; Źródło: nowa tablica
    mov     rcx, 256                        ; Licznik: 256 elementów do skopiowania
    rep     movsq                           ; Kopiuj sprzętowo rsi -> rdi

    ; 5. Szybkie kopiowanie tablicy długości reguł (256 elementów * 8 bajtów)
    lea     rdi, [rel rule_lens]            ; Cel: stara tablica
    lea     rsi, [rel next_rule_lens]       ; Źródło: nowa tablica
    mov     rcx, 256                        ; Licznik: 256 elementów
    rep     movsq                           ; Kopiuj sprzętowo rsi -> rdi

    ; W tym momencie struktury 'rule_...' zawierają już w pełni zaktualizowane
    ; i złożone reguły dla kolejnej potęgi iteracji.





    jmp     .iter_loop

    ; 8. Wypisywanie wyniku i wyjście
.output:
    mov     rdi, [rel axiom_buf]
    mov     rsi, [rel axiom_len]
    call    write_all                   ; Zrzut na STDOUT bufora

    lea     rdi, [rel newline_char]
    mov     rsi, 1
    call    write_all                   ; Zrzut '\n' na końcu

    ; Standardowe czyszczenie przed opuszczeniem programu (0 exit_code)
    call    cleanup
    mov     rax, SYS_EXIT
    mov     rdi, 0
    syscall

; ==============================================================================
; Procedury i funkcje pomocnicze
; ==============================================================================

; error_exit - wyjście awaryjne (czyści używaną pamięć i kończy jako błąd z 1)
error_exit:
    call    cleanup
    mov     rax, SYS_EXIT
    mov     rdi, 1
    syscall

; cleanup - Zwalnianie zalokowanej pamięci ujętej w wskaźnikach
cleanup:
    mov     rdi, [rel axiom_buf]
    mov     rsi, [rel axiom_cap]
    call    do_munmap
    mov     rdi, [rel rule_buf]
    mov     rsi, [rel rule_cap]
    call    do_munmap
    mov     rdi, [rel next_rule_buf]
    mov     rsi, [rel next_rule_cap]
    call    do_munmap
    ret

; get_char - Przetwarzanie i ładowanie IO wejścia standardowego bajt po bajcie.
; Korzysta z io_buf jako pamięci podręcznej. Ustawia CF=1 przy EOF. Zwraca bajt w AL.
get_char:
    mov     rax, [rel io_buf_ptr]
    cmp     rax, [rel io_buf_len]
    jl      .return_char

    mov     rax, SYS_READ               ; Zabezpieczenie na wyczytanie nowej paczki
    mov     rdi, 0                      ; STDIN
    lea     rsi, [rel io_buf]
    mov     rdx, IO_BUF_SIZE
    syscall
    cmp     rax, 0
    jl      error_exit                  ; Błąd wywołania systemowego IO
    je      .eof

    mov     [rel io_buf_len], rax
    mov     qword [rel io_buf_ptr], 0
    mov     rax, 0
.return_char:
    mov     rcx, [rel io_buf_ptr]
    lea     rdx, [rel io_buf]
    movzx   eax, byte [rdx + rcx]       ; Ekstrakcja do rejestru wyjściowego (AL)
    inc     rcx
    mov     [rel io_buf_ptr], rcx
    clc                                 ; CF=0 (odczyt powiódł się)
    ret
.eof:
    stc                                 ; CF=1 (odczyt trafił na EOF)
    ret

; write_all - Wypisanie długiego na RSI bajtów ciągu z adresu RDI na STDOUT.
write_all:
    test    rsi, rsi
    jz      .done
    mov     r8, rdi                     ; R8 użyty jako ruchomy wskaźnik bufora wpisywanego
    mov     r9, rsi                     ; R9 rezerwowany dla pozostałej długości
.wloop:
    mov     rax, SYS_WRITE
    mov     rdi, 1                      ; STDOUT
    mov     rsi, r8
    mov     rdx, r9
    syscall
    cmp     rax, 0
    jl      error_exit
    add     r8, rax                     ; Przesunięcie okienka
    sub     r9, rax
    jg      .wloop
.done:
    ret

; mmap_alloc - Funkcja rezerwująca pamięć z mmap (wielkość w rdi). Zwraca RAX.
mmap_alloc:
    push    rdi
    mov     rax, SYS_MMAP
    mov     rsi, rdi                    ; Length
    xor     rdi, rdi                    ; Domyślny adres null
    mov     rdx, PROT_READ | PROT_WRITE
    mov     r10, MAP_PRIVATE | MAP_ANONYMOUS
    mov     r8, -1                      ; Bez podpiętego deskryptora
    xor     r9, r9                      ; Zerowy offset
    syscall
    pop     rdi
    cmp     rax, -4096                  ; Jeśli z przedziału błędów, opuść
    ja      error_exit
    ret

; do_mremap - Wywołanie rozszerzające zarezerwowaną przestrzeń w pamięci
; Parametry wejściowe: rdi = stary adres, rsi = stary rozmiar, rdx = nowy rozmiar
do_mremap:
    mov     rax, SYS_MREMAP
    mov     r10, MREMAP_MAYMOVE         ; Opcja 1 wymuszająca pozwoleń realokacji na inny adres
    xor     r8, r8
    xor     r9, r9
    syscall
    cmp     rax, -4096
    ja      error_exit
    ret

; do_munmap - Bezpieczne dealokowanie pamięci w podanym wskaźniku na rozmiar RSI.
do_munmap:
    test    rdi, rdi                    ; Nie unmapujemy z wskaźników zerowych (0)
    jz      .done
    mov     rax, SYS_MUNMAP
    syscall
    cmp     rax, -4096                  ; Złapanie błędu i zakończenie statusem 1
.done:
    ret