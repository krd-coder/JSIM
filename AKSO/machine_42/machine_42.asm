default rel

; ==============================================================================
; Stale i numery wywolan systemowych
; ==============================================================================
%define SYS_READ      0
%define SYS_WRITE     1
%define SYS_EXIT      60

%define SIGMA_MIN     49          ; '1' - najmniejszy kod znaku alfabetu Sigma
%define SIGMA_MAX     90          ; 'Z' - najwiekszy kod znaku alfabetu Sigma
%define SIGMA_SIZE    42          ; |Sigma| = dlugosc kazdej permutacji L, R, T

%define KEY_LEN       2           ; klucz startowy z CLI: 1 znak lambda + 1 znak rho
%define IO_BUF_SIZE   4096        ; rozmiar bufora I/O (pojedynczy sys_read/sys_write)

; ==============================================================================
; Sekcja zmiennych niezainicjowanych
; ==============================================================================
section .bss
    L_1_arr     resb SIGMA_SIZE    ; permutacja odwrotna do L, generowana w locie
    R_1_arr     resb SIGMA_SIZE    ; permutacja odwrotna do R, generowana w locie
    io_buf      resb IO_BUF_SIZE   ; wspolny bufor odczytu/zapisu
    bytes_read  resq 1             ; liczba bajtow z ostatniego sys_read (potrzebna
                                    ; po powrocie z machine_42_function do sys_write)

; ==============================================================================
; Sekcja kodu
; ==============================================================================
section .text
    global _start
    extern machine_42_function

; ------------------------------------------------------------------------------
; Program glowny (CLI): machine_42 L R T key < wejscie > wyjscie
; ------------------------------------------------------------------------------
_start:
    mov   rax, [rsp]                 ; argc
    cmp   rax, 5                     ; program + L + R + T + key
    jne   error_exit

    mov   r12, [rsp + 16]            ; argv[1] = L
    mov   r14, [rsp + 24]            ; argv[2] = R
    mov   rbp, [rsp + 32]            ; argv[3] = T
    mov   r9,  [rsp + 40]            ; argv[4] = klucz startowy (string)

    mov   rdi, r12                   ; walidacja L: dlugosc SIGMA_SIZE, znaki z Sigma
    mov   rsi, SIGMA_SIZE
    call  validate_string

    mov   rdi, r14                   ; walidacja R
    mov   rsi, SIGMA_SIZE
    call  validate_string

    mov   rdi, rbp                   ; walidacja T
    mov   rsi, SIGMA_SIZE
    call  validate_string

    mov   rdi, r9                    ; walidacja klucza (dlugosc KEY_LEN)
    mov   rsi, KEY_LEN
    call  validate_string

    mov   bl, [r9]                   ; bl = lambda (pierwszy znak klucza)
    movzx eax, byte [r9 + 1]          ; rejestr wysokiego bajtu (bh) nie moze byc
    mov   bh, al                      ; adresowany razem z r9 (wymog REX) - posrednio

    ; ---- generowanie L_1 z L: dla kazdego i, L_1[L[i]-49] = i+49 ----
    lea   r8, [rel L_1_arr]
    xor   rcx, rcx
.build_L1:
    cmp   rcx, SIGMA_SIZE
    jge   .build_L1_done
    movzx eax, byte [r12 + rcx]      ; L[i]
    sub   al, SIGMA_MIN               ; wartosc = L[i] - 49
    movzx eax, al                     ; czysty indeks do adresowania [r8 + rax]
    mov   dl, cl
    add   dl, SIGMA_MIN                ; i + 49
    mov   [r8 + rax], dl
    inc   rcx
    jmp   .build_L1
.build_L1_done:

    ; ---- generowanie R_1 z R, analogicznie ----
    lea   r8, [rel R_1_arr]
    xor   rcx, rcx
.build_R1:
    cmp   rcx, SIGMA_SIZE
    jge   .build_R1_done
    movzx eax, byte [r14 + rcx]      ; R[i]
    sub   al, SIGMA_MIN
    movzx eax, al
    mov   dl, cl
    add   dl, SIGMA_MIN
    mov   [r8 + rax], dl
    inc   rcx
    jmp   .build_R1
.build_R1_done:

    lea   r13, [rel L_1_arr]          ; r13 = L_1, stabilne przez cala petle I/O
    lea   r15, [rel R_1_arr]          ; r15 = R_1

    ; rejestry stale dla petli I/O: r12=L r13=L_1 r14=R r15=R_1 rbp=T bx=key
.read_loop:
    mov   rax, SYS_READ
    xor   rdi, rdi                    ; fd = 0 (stdin)
    lea   rsi, [rel io_buf]
    mov   rdx, IO_BUF_SIZE
    syscall

    test  rax, rax
    jz    .exit_ok                    ; 0 bajtow = EOF -> sys_exit(0)
    js    error_exit                  ; ujemny wynik = blad sys_read

    mov   [rel bytes_read], rax
    mov   r9, rax                     ; lokalna kopia licznika do walidacji ponizej

    lea   r10, [rel io_buf]           ; baza bufora, uzywana tez jako arg. "text"
    xor   rcx, rcx
.validate_input_loop:
    cmp   rcx, r9
    jge   .validate_input_done
    movzx eax, byte [r10 + rcx]
    cmp   eax, SIGMA_MIN               ; znak spoza Sigma (w tym '\n') -> blad
    jl    error_exit
    cmp   eax, SIGMA_MAX
    jg    error_exit
    inc   rcx
    jmp   .validate_input_loop
.validate_input_done:

    mov   rdi, r12                     ; L
    mov   rsi, r13                     ; L_1
    mov   rdx, r14                     ; R
    mov   rcx, r15                     ; R_1
    mov   r8,  rbp                     ; T
    movzx r9d, bx                      ; key (r9w), gorne bity r9 wyzerowane

    push  qword [rel bytes_read]       ; 8. argument (size) -> [rsp+16] w callee
    push  r10                          ; 7. argument (text) -> [rsp+8] w callee
    call  machine_42_function
    add   rsp, 16                      ; sprzatanie argumentow przekazanych na stosie

    mov   bx, ax                        ; nowy stan klucza na kolejny cykl

    ; ---- sys_write moze zapisac mniej bajtow niz zadano (zapis czesciowy) ----
    ; ---- lub zwrocic blad - obie sytuacje trzeba jawnie obsluzyc.           ----
    ; ---- UWAGA: instrukcja `syscall` zawsze niszczy rcx i r11 (sprzetowo   ----
    ; ---- zapisuje tam adres powrotu i flagi na potrzeby sysret), wiec      ----
    ; ---- licznik pozostalych bajtow NIE moze byc trzymany w r11/rcx.       ----
    lea   r10, [rel io_buf]             ; wskaznik do zapisu, przesuwany przy zapisie czesciowym
    mov   r9, [rel bytes_read]          ; pozostalo do zapisania
.write_loop:
    test  r9, r9
    jz    .read_loop                    ; wszystko zapisane -> kolejny cykl odczytu
    mov   rax, SYS_WRITE
    mov   rdi, 1                        ; fd = 1 (stdout)
    mov   rsi, r10
    mov   rdx, r9
    syscall
    test  rax, rax
    jle   error_exit                    ; rax<=0 (blad lub brak postepu) -> blad zapisu
    add   r10, rax
    sub   r9, rax
    jmp   .write_loop

.exit_ok:
    mov   rax, SYS_EXIT
    xor   rdi, rdi
    syscall

error_exit:
    mov   rax, SYS_EXIT
    mov   rdi, 1
    syscall

; ------------------------------------------------------------------------------
; validate_string(rdi = wskaznik, rsi = oczekiwana dlugosc)
; Wychodzi przez error_exit, jesli dlugosc lub zakres znakow sa niepoprawne.
; Rejestry nieulotne (rbx, rbp, r12-r15) sa tu tylko odczytywane pod adresami
; przekazanymi w rdi, wiec nie wymagaja zachowania.
; ------------------------------------------------------------------------------
validate_string:
    xor   rcx, rcx
.vs_loop:
    cmp   rcx, rsi
    jl    .vs_check_char
    cmp   byte [rdi + rcx], 0          ; na pozycji oczekiwanej dlugosci musi byc NUL
    jne   error_exit                   ; string dluzszy niz wymagany
    ret
.vs_check_char:
    movzx eax, byte [rdi + rcx]
    test  eax, eax
    jz    error_exit                   ; string krotszy niz wymagany (przedwczesny NUL)
    cmp   eax, SIGMA_MIN
    jl    error_exit
    cmp   eax, SIGMA_MAX
    jg    error_exit
    inc   rcx
    jmp   .vs_loop

section .note.GNU-stack noalloc noexec nowrite progbits
