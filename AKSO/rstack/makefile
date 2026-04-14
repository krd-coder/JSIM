# --- ZMIENNE (Konfiguracja) ---
CC = gcc

# Flagi kompilacji (zgodnie z poleceniem)
CFLAGS = -Wall -Wextra -Wno-implicit-fallthrough -std=gnu23 -fPIC -O2

# Flagi linkera - tworzenie biblioteki współdzielonej (shared) i podmienianie funkcji pamięci pod testy
LDFLAGS = -shared -Wl,--wrap=malloc -Wl,--wrap=calloc -Wl,--wrap=realloc -Wl,--wrap=reallocarray -Wl,--wrap=free -Wl,--wrap=strdup -Wl,--wrap=strndup

# --- CELE SPECJALNE ---
# .PHONY mówi make'owi, że "all" i "clean" to tylko nazwy akcji, a nie fizyczne pliki do stworzenia.
.PHONY: all clean

# Domyślny cel (wykona się, gdy wpiszesz samo "make").
# Chcemy, żeby stworzył naszą bibliotekę.
all: librstack.so

# --- GŁÓWNY CEL (Biblioteka) ---
# librstack.so zależy od plików obiektowych .o. Jeśli rstack.o lub memory_tests.o
# zostaną zmienione, biblioteka zostanie zbudowana na nowo (zlinkowana).
librstack.so: rstack.o memory_tests.o
	$(CC) $(LDFLAGS) -o $@ $^

# --- CELE POŚREDNIE (Kompilacja do plików obiektowych .o) ---
# Żeby skompilować rstack.o, potrzebujemy rstack.c, ale także nagłówków.
# Dodanie rstack.h jako zależności gwarantuje, że gdy zmienisz definicję struktury 
# w pliku .h, rstack.c skompiluje się na nowo!
rstack.o: rstack.c rstack.h memory_tests.h
	$(CC) $(CFLAGS) -c $< -o $@

# Analogicznie dla memory_tests.c
memory_tests.o: memory_tests.c memory_tests.h
	$(CC) $(CFLAGS) -c $< -o $@

# --- SPRZĄTANIE ---
# Cel "clean" pozwala łatwo usunąć wszystkie wygenerowane pliki poleceniem "make clean".
clean:
	rm -f *.o librstack.so