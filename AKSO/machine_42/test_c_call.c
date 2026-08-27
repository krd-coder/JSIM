#include <stdio.h>
#include <string.h>

typedef struct {
    char lambda;
    char rho;
} key_t;

extern key_t machine_42_function(char const *L, char const *L_1,
                                  char const *R, char const *R_1,
                                  char const *T, key_t key,
                                  char *text, size_t size);

static void invert(char const *p, char *inv) {
    for (int i = 0; i < 42; i++) inv[p[i] - 49] = i + 49;
}

int main(void) {
    char SIGMA[42];
    for (int i = 0; i < 42; i++) SIGMA[i] = 49 + i;

    char L[42], R[42], T[42], L_1[42], R_1[42];
    for (int i = 0; i < 42; i++) L[i] = SIGMA[(i + 3) % 42];
    for (int i = 0; i < 42; i++) R[i] = SIGMA[(i + 5) % 42];
    for (int i = 0; i < 21; i++) { T[i] = SIGMA[41 - i]; T[41 - i] = SIGMA[i]; }
    invert(L, L_1);
    invert(R, R_1);

    char text[6] = "123ZA";
    size_t size = 5;
    key_t key = { '1', '1' };

    key_t result = machine_42_function(L, L_1, R, R_1, T, key, text, size);

    printf("ciphertext: %s\n", text);
    printf("final key: lambda=%c rho=%c\n", result.lambda, result.rho);

    if (strcmp(text, "JIH") == 0) {
        printf("BUG: text truncated unexpectedly\n");
    }
    return 0;
}
