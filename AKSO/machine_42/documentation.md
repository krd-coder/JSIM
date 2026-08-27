# Dokumentacja Implementacyjna: Wirnikowa Maszyna Szyfrująca Model 42

Poniższy dokument zawiera wyłącznie techniczne szczegóły implementacji i opis działania kodu dla maszyny szyfrującej w asemblerze x86_64.

## 1. Arytmetyka i Mapowanie Znaków
Maszyna operuje na alfabecie $\Sigma$ składającym się z 42 znaków (kody ASCII od 49 `1` do 90 `Z`). 
Wszystkie operacje wewnętrzne (przesunięcia $Q$, indeksowanie tablic permutacji) muszą być wykonywane na indeksach z przedziału `[0, 41]`.

*   **Znak $ightarrow$ Indeks:** `indeks = kod_ascii - 49`
*   **Indeks $ightarrow$ Znak:** `kod_ascii = indeks + 49`

Operacja przesunięcia cyklicznego $Q_lpha(x)$ dla indeksu $x$ i pozycji wirnika $lpha$:
*   $Q_lpha(x) = (x + (lpha - 49)) \pmod{42}$
*   $Q_lpha^{-1}(x) = (x - (lpha - 49) + 42) \pmod{42}$

*Szczegół implementacyjny:* Unikaj instrukcji `div` do obliczania modulo. Użyj instrukcji warunkowych:
```assembly
; Dodawanie modulo 42
add al, cl
cmp al, 42
jl .skip_sub
sub al, 42
.skip_sub:

; Odejmowanie modulo 42
sub al, cl
jns .skip_add
add al, 42
.skip_add:
```

## 2. Implementacja funkcji `machine_42_function`
Funkcja biblioteczna napisana w asemblerze musi być zgodna z konwencją **System V AMD64 ABI**.

### 2.1. Odbiór parametrów
| Parametr | Typ | Rejestr / Stos | Opis |
| :--- | :--- | :--- | :--- |
| `L` | `char*` | `rdi` | Permutacja L |
| `L_1` | `char*` | `rsi` | Permutacja odwrotna do L |
| `R` | `char*` | `rdx` | Permutacja R |
| `R_1` | `char*` | `rcx` | Permutacja odwrotna do R |
| `T` | `char*` | `r8`  | Permutacja T |
| `key` | `key_t` (16-bit) | `r9w` | Klucz szyfrowania (`r9b` = $\lambda$, `r9w >> 8` = $ho$) |
| `text` | `char*` | `[rsp + 8]` | Bufor tekstu (wymaga pobrania ze stosu) |
| `size` | `size_t` | `[rsp + 16]` | Rozmiar tekstu (wymaga pobrania ze stosu) |

*Uwaga na Prolog:* Przed pobraniem argumentów ze stosu (`[rsp + 8]`), pamiętaj o uwzględnieniu ewentualnego przesunięcia `rsp` spowodowanego przez zachowywanie rejestrów (np. `push rbx`, `push rbp`).

### 2.2. Działanie pętli szyfrującej
Dla każdego znaku w buforze `text` należy wykonać:

1.  **Obrót wirników (Stepping):**
    *   Pobierz bieżącą pozycję wirnika R ($ho$). Zwiększ o 1 modulo 42: `rho = ((rho - 49 + 1) % 42) + 49`.
    *   Sprawdź, czy **stara** pozycja $ho$ (przed obrotem) lub **nowa** (zależnie od interpretacji "osiągnie") była jedną z pozycji obrotowych: `'L' (76)`, `'R' (82)`, `'T' (84)`.
    *   Jeśli tak, obróć również wirnik L ($\lambda$) o 1 modulo 42.

2.  **Ścieżka sygnału:**
    Dla znaku $c$ (przekonwertowanego na indeks $x = c - 49$), wykonuj kolejno (zapisując wynik na $x$):
    *   `x = Q_rho(x)`
    *   `x = R[x] - 49`  (Zwróć uwagę, że R zawiera kody ASCII, więc po odczycie należy odjąć 49)
    *   `x = Q_rho_inv(x)`
    *   `x = Q_lambda(x)`
    *   `x = L[x] - 49`
    *   `x = Q_lambda_inv(x)`
    *   `x = T[x] - 49`
    *   `x = Q_lambda(x)`
    *   `x = L_1[x] - 49`
    *   `x = Q_lambda_inv(x)`
    *   `x = Q_rho(x)`
    *   `x = R_1[x] - 49`
    *   `x = Q_rho_inv(x)`
    
3.  **Zapis:**
    *   Po przejściu przez całą ścieżkę, przekonwertuj indeks z powrotem na znak (`c = x + 49`).
    *   Nadpisz znak w buforze: `mov byte [text + index], cl`.

### 2.3. Zwracanie wyniku
Zaktualizowany klucz (pozycje wirników) należy spakować do struktury `key_t` i zwrócić w rejestrze `rax`. Z racji, że `key_t` ma 2 bajty, umieść $\lambda$ w `al`, a $ho$ w `ah`, i wyzeruj wyższe bity `rax`.

## 3. Implementacja Programu Głównego `machine_42.asm`
Program ten pełni rolę CLI (Command Line Interface).

### 3.1. Przetwarzanie i walidacja CLI
*   Pobierz `argc` ze stosu (`[rsp]`). Musi wynosić 5 (program + 4 argumenty).
*   Każda przekazana permutacja (L, R, T) z argumentów `argv[1]`, `argv[2]`, `argv[3]` musi mieć długość 42.
*   Klucz początkowy (`argv[4]`) musi mieć długość 2.
*   Walidacja znaków: Zawsze sprawdzaj czy każdy znak mieści się w przedziale `'1'` (49) do `'Z'` (90). Błędne wejście -> `sys_exit(1)`.

### 3.2. Generowanie Permutacji Odwrotnych w locie
Funkcja szyfrująca wymaga wskaźników na tablice `L_1` i `R_1`. Program musi je wygenerować i umieścić w `.bss` lub na stosie przed wywołaniem szyfrowania.
```c
// Algorytm odwracania permutacji P i zapisywania do P_1
for(int i = 0; i < 42; i++) {
    int wartosc = P[i] - 49;
    P_1[wartosc] = i + 49;
}
```

### 3.3. Pętla wejścia/wyjścia (I/O)
*   **Buforowanie:** Zaalokuj globalny bufor w sekcji `.bss` (np. rozmiar 4096 bajtów).
*   **`sys_read` (syscall 0):** Czytaj dane ze standardowego wejścia (fd=0) do bufora. Zakończ program (`sys_exit(0)`), jeśli zwróci 0 (EOF).
*   **Walidacja wejścia:** Program musi sprawdzić przeczytany strumień. Wszelkie znaki spoza $\Sigma$ (z wyjątkiem ewentualnego zignorowania/obsłużenia nowej linii według specyfikacji) muszą powodować błąd (`sys_exit(1)`).
*   **Wywołanie funkcji:** Przekaż wymagane wskaźniki w rejestrach (zgodnie z 2.1). Wywołaj `call machine_42_function`.
*   **Zapisanie stanu klucza:** Wynik działania funkcji z `rax` musi zostać użyty jako `key` w następnym cyklu `sys_read` i wywołaniu funkcji.
*   **`sys_write` (syscall 1):** Wypisz zmodyfikowany bufor na standardowe wyjście (fd=1). Zawsze pisz tyle bajtów, ile zostało wczytane.
*   Wróć do początku pętli `sys_read`.

## 4. Zasady Formatowania Kodu (Wymogi ABI i Oceny)
1.  **Callee-saved registers:** Rejestry `rbx`, `rbp`, `r12`, `r13`, `r14`, `r15` po zakończeniu funkcji *muszą* mieć taką samą wartość, jaką miały na początku. Użyj `push` na początku i `pop` na końcu.
2.  **Struktura Kodu:**
    *   Kolumna 1: Etykiety
    *   Kolumna 2: Mnemoniki
    *   Kolumna 3: Operandy
    *   Kolumna 4: Komentarze opisujące *dlaczego*, a nie *co* robi dana linia.
