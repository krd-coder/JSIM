// ==============================================================================
// machine_42.cpp - niezalezny (od wersji asm) port maszyny szyfrujacej Model 42.
// Implementacja pisana wprost ze specyfikacji (nie z odczytu machine_42.asm /
// machine_42_function.asm) - sluzy jako referencja do weryfikacji krzyzowej.
// Kompilacja: g++ -std=c++17 -O2 machine_42.cpp -o machine_42
// ==============================================================================

#include <cstddef>
#include <cstring>
#include <unistd.h>   // read/write - wymagane surowe wywolania POSIX, bez cin/cstdio

// ------------------------------------------------------------------------------
// Stale alfabetu Sigma i rozmiaru bufora I/O (zgodnie ze specyfikacja).
// ------------------------------------------------------------------------------
static constexpr int SIGMA_MIN  = 49;   // '1' - najmniejszy kod znaku Sigma
static constexpr int SIGMA_MAX  = 90;   // 'Z' - najwiekszy kod znaku Sigma
static constexpr int SIGMA_SIZE = 42;   // |Sigma|

static constexpr int NOTCH_L = 'L';     // 76 - pozycja zaczepienia obracajaca wirnik L
static constexpr int NOTCH_R = 'R';     // 82
static constexpr int NOTCH_T = 'T';     // 84

static constexpr int KEY_LEN     = 2;
static constexpr size_t IO_BUF_SIZE = 4096;

// ------------------------------------------------------------------------------
// key_t - stan wirnikow (pozycje L i R jako kody ASCII z Sigma).
// ------------------------------------------------------------------------------
struct key_t {
    char lambda;
    char rho;
};

// ------------------------------------------------------------------------------
// Q_alfa(x) i Q_alfa^-1(x) - przesuniecie cykliczne wzgledem pozycji wirnika alfa
// (alfa podane jako kod ASCII z [49,90], x jako indeks z [0,41]).
// ------------------------------------------------------------------------------
static inline int Q(int x, char alfa) {
    int shift = static_cast<unsigned char>(alfa) - SIGMA_MIN;
    return (x + shift) % SIGMA_SIZE;
}

static inline int Q_inv(int x, char alfa) {
    int shift = static_cast<unsigned char>(alfa) - SIGMA_MIN;
    return (x - shift + SIGMA_SIZE) % SIGMA_SIZE;
}

// ------------------------------------------------------------------------------
// key_t machine_42_function(...) - sygnatura DOKLADNIE jak w specyfikacji, zeby
// dalo sie porownac linia-po-linii z wersja asemblerowa.
// Przetwarza text[0..size) in-place, znak po znaku; stan wirnikow ewoluuje
// per-wywolanie zaczynajac od `key` i jest zwracany na koniec.
// ------------------------------------------------------------------------------
key_t machine_42_function(char const *L, char const *L_1,
                           char const *R, char const *R_1,
                           char const *T, key_t key,
                           char *text, size_t size) {
    char lambda = key.lambda;
    char rho = key.rho;

    for (size_t i = 0; i < size; ++i) {
        // ---- 1. Obrot wirnikow: R obraca sie przy kazdym znaku ----
        rho = static_cast<char>(((rho - SIGMA_MIN + 1) % SIGMA_SIZE) + SIGMA_MIN);

        // Sprawdzana jest NOWA (po obrocie) pozycja rho - to celowe.
        if (rho == NOTCH_L || rho == NOTCH_R || rho == NOTCH_T) {
            lambda = static_cast<char>(((lambda - SIGMA_MIN + 1) % SIGMA_SIZE) + SIGMA_MIN);
        }

        // ---- 2. Sciezka sygnalu ----
        int x = static_cast<unsigned char>(text[i]) - SIGMA_MIN;

        x = Q(x, rho);
        x = static_cast<unsigned char>(R[x]) - SIGMA_MIN;
        x = Q_inv(x, rho);

        x = Q(x, lambda);
        x = static_cast<unsigned char>(L[x]) - SIGMA_MIN;
        x = Q_inv(x, lambda);

        x = static_cast<unsigned char>(T[x]) - SIGMA_MIN;

        x = Q(x, lambda);
        x = static_cast<unsigned char>(L_1[x]) - SIGMA_MIN;
        x = Q_inv(x, lambda);

        x = Q(x, rho);
        x = static_cast<unsigned char>(R_1[x]) - SIGMA_MIN;
        x = Q_inv(x, rho);

        // ---- 3. Zapis ----
        text[i] = static_cast<char>(x + SIGMA_MIN);
    }

    return key_t{lambda, rho};
}

// ------------------------------------------------------------------------------
// validate_string - dlugosc musi byc DOKLADNIE `len`, kazdy znak z Sigma.
// (odpowiednik validate_string z machine_42.asm)
// ------------------------------------------------------------------------------
static bool validate_string(char const *s, size_t len) {
    size_t n = std::strlen(s);
    if (n != len) return false;
    for (size_t i = 0; i < len; ++i) {
        unsigned char c = static_cast<unsigned char>(s[i]);
        if (c < SIGMA_MIN || c > SIGMA_MAX) return false;
    }
    return true;
}

// ------------------------------------------------------------------------------
// main - odpowiednik _start z machine_42.asm: CLI "machine_42 L R T key",
// petla I/O czyta/pisze surowymi wywolaniami POSIX w porcjach po IO_BUF_SIZE,
// zeby zachowanie (w tym MOMENT wykrycia bledu wzgledem juz wypisanego wyniku)
// bylo bit-w-bit zgodne z wersja asemblerowa.
// ------------------------------------------------------------------------------
int main(int argc, char **argv) {
    if (argc != 5) return 1;

    char const *L = argv[1];
    char const *R = argv[2];
    char const *T = argv[3];
    char const *key_arg = argv[4];

    if (!validate_string(L, SIGMA_SIZE)) return 1;
    if (!validate_string(R, SIGMA_SIZE)) return 1;
    if (!validate_string(T, SIGMA_SIZE)) return 1;
    if (!validate_string(key_arg, KEY_LEN)) return 1;

    key_t key{key_arg[0], key_arg[1]};

    // ---- generowanie L_1 i R_1 przez odwrocenie L i R ----
    char L_1[SIGMA_SIZE];
    char R_1[SIGMA_SIZE];
    for (int i = 0; i < SIGMA_SIZE; ++i) {
        unsigned char li = static_cast<unsigned char>(L[i]) - SIGMA_MIN;
        L_1[li] = static_cast<char>(i + SIGMA_MIN);
        unsigned char ri = static_cast<unsigned char>(R[i]) - SIGMA_MIN;
        R_1[ri] = static_cast<char>(i + SIGMA_MIN);
    }

    static char io_buf[IO_BUF_SIZE];

    for (;;) {
        ssize_t n = read(0, io_buf, IO_BUF_SIZE);
        if (n == 0) return 0;      // EOF -> sukces
        if (n < 0) return 1;       // blad odczytu

        // ---- walidacja calej porcji PRZED szyfrowaniem/wypisaniem ----
        for (ssize_t i = 0; i < n; ++i) {
            unsigned char c = static_cast<unsigned char>(io_buf[i]);
            if (c < SIGMA_MIN || c > SIGMA_MAX) return 1;   // '\n' tez tu wpada
        }

        key = machine_42_function(L, L_1, R, R_1, T, key, io_buf, static_cast<size_t>(n));

        // ---- zapis z obsluga czesciowego write ----
        ssize_t written = 0;
        while (written < n) {
            ssize_t w = write(1, io_buf + written, static_cast<size_t>(n - written));
            if (w < 0) return 1;
            written += w;
        }
    }
}
