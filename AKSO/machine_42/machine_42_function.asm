default rel

; ==============================================================================
; Stale (alfabet Sigma i punkty zaczepienia wirnikow)
; ==============================================================================
%define SIGMA_MIN     49          ; '1' - najmniejszy kod znaku alfabetu Sigma
%define SIGMA_MAX     90          ; 'Z' - najwiekszy kod znaku alfabetu Sigma
%define SIGMA_SIZE    42          ; |Sigma| = dlugosc kazdej permutacji L, R, T

%define NOTCH_L       76          ; 'L' - pozycja zaczepienia obracajaca wirnik L
%define NOTCH_R       82          ; 'R'
%define NOTCH_T       84          ; 'T'

; ==============================================================================
; Arytmetyka modulo 42 bez `div` - jeden operand wejsciowy zawsze w [0,41],
; wiec suma/roznica miesci sie w jednym warunkowym dodaniu/odjeciu SIGMA_SIZE.
; ==============================================================================
%macro MOD_ADD42 2                 ; %1: akumulator 8-bit, %2: skladnik (rej. lub imm.)
    add   %1, %2
    cmp   %1, SIGMA_SIZE
    jl    %%skip
    sub   %1, SIGMA_SIZE
%%skip:
%endmacro

%macro MOD_SUB42 2                 ; %1: akumulator 8-bit, %2: odjemnik (rej. lub imm.)
    sub   %1, %2
    jns   %%skip
    add   %1, SIGMA_SIZE
%%skip:
%endmacro

section .text
    global machine_42_function

; ------------------------------------------------------------------------------
; key_t machine_42_function(char const *L, char const *L_1,
;                            char const *R, char const *R_1,
;                            char const *T, key_t key,
;                            char *text, size_t size);
;
; System V AMD64 ABI (funkcja wolana z C):
;   rdi = L, rsi = L_1, rdx = R, rcx = R_1, r8 = T, r9w = key
;     (key_t to struktura {char lambda; char rho;} - klasa INTEGER, 2 bajty,
;      wiec trafia w calosci do r9w: r9b = lambda, [r9w >> 8] = rho)
;   [rsp+8] = text (char*), [rsp+16] = size (size_t)   (przed prologiem)
;   zwraca key_t w rax: al = lambda, ah = rho, reszta rax wyzerowana
; ------------------------------------------------------------------------------
machine_42_function:
    push  rbx                          ; zachowanie rejestrow nieulotnych (ABI)
    push  rbp
    push  r12
    push  r13
    push  r14
    push  r15
                                        ; po 6x push argumenty ze stosu przesuniete o 48B
    mov   r12, rdi                     ; L
    mov   r13, rsi                     ; L_1
    mov   r14, rdx                     ; R
    mov   r15, rcx                     ; R_1
    mov   rbp, r8                      ; T
    mov   rbx, [rsp + 56]              ; text  (oryginalne [rsp+8] + 48B pushy)
    mov   r10, [rsp + 64]              ; size  (oryginalne [rsp+16] + 48B pushy)

    movzx eax, r9w                     ; eax = 0..0 | rho | lambda (bez wzgledu na
                                        ; smieci w gornych bitach r9)
    mov   dl, al                       ; dl = lambda
    shr   eax, 8
    mov   dh, al                       ; dh = rho

    xor   r11, r11                     ; i = 0
.loop_chars:
    cmp   r11, r10
    jge   .loop_done

    ; ---- 1. Obrot wirnikow: R obraca sie przy kazdym znaku ----
    mov   al, dh
    sub   al, SIGMA_MIN
    MOD_ADD42 al, 1
    add   al, SIGMA_MIN
    mov   dh, al                       ; nowe rho

    cmp   dh, NOTCH_L                  ; sprawdzana jest NOWA pozycja rho
    je    .step_lambda
    cmp   dh, NOTCH_R
    je    .step_lambda
    cmp   dh, NOTCH_T
    jne   .after_step
.step_lambda:
    mov   al, dl
    sub   al, SIGMA_MIN
    MOD_ADD42 al, 1
    add   al, SIGMA_MIN
    mov   dl, al                       ; nowe lambda
.after_step:

    ; ---- 2. Sciezka sygnalu ----
    movzx eax, byte [rbx + r11]        ; c = text[i]
    sub   al, SIGMA_MIN                 ; x = indeks

    mov   cl, dh
    sub   cl, SIGMA_MIN
    MOD_ADD42 al, cl                    ; x = Q_rho(x)

    movzx eax, al
    mov   al, [r14 + rax]                ; x = R[x]
    sub   al, SIGMA_MIN

    mov   cl, dh
    sub   cl, SIGMA_MIN
    MOD_SUB42 al, cl                    ; x = Q_rho^-1(x)

    mov   cl, dl
    sub   cl, SIGMA_MIN
    MOD_ADD42 al, cl                    ; x = Q_lambda(x)

    movzx eax, al
    mov   al, [r12 + rax]                ; x = L[x]
    sub   al, SIGMA_MIN

    mov   cl, dl
    sub   cl, SIGMA_MIN
    MOD_SUB42 al, cl                    ; x = Q_lambda^-1(x)

    movzx eax, al
    mov   al, [rbp + rax]                ; x = T[x] (reflektor, bez przesuniecia)
    sub   al, SIGMA_MIN

    mov   cl, dl
    sub   cl, SIGMA_MIN
    MOD_ADD42 al, cl                    ; x = Q_lambda(x)

    movzx eax, al
    mov   al, [r13 + rax]                ; x = L_1[x]
    sub   al, SIGMA_MIN

    mov   cl, dl
    sub   cl, SIGMA_MIN
    MOD_SUB42 al, cl                    ; x = Q_lambda^-1(x)

    mov   cl, dh
    sub   cl, SIGMA_MIN
    MOD_ADD42 al, cl                    ; x = Q_rho(x)

    movzx eax, al
    mov   al, [r15 + rax]                ; x = R_1[x]
    sub   al, SIGMA_MIN

    mov   cl, dh
    sub   cl, SIGMA_MIN
    MOD_SUB42 al, cl                    ; x = Q_rho^-1(x)

    ; ---- 3. Zapis ----
    add   al, SIGMA_MIN                 ; c = x + 49
    mov   [rbx + r11], al

    inc   r11
    jmp   .loop_chars
.loop_done:
    xor   eax, eax                       ; wyzerowanie gornych bitow rax (wymog ABI)
    mov   al, dl                         ; al = lambda
    mov   ah, dh                         ; ah = rho

    pop   r15
    pop   r14
    pop   r13
    pop   r12
    pop   rbp
    pop   rbx
    ret

section .note.GNU-stack noalloc noexec nowrite progbits
