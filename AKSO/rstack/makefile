# --- ZMIENNE (Konfiguracja) ---
CC = gcc

# Flagi kompilacji (zgodnie z poleceniem)
CFLAGS = -Wall -Wextra -Wno-implicit-fallthrough -std=gnu2x -fPIC -O2

# Flagi linkera - tworzenie biblioteki współdzielonej (shared) i podmienianie funkcji pamięci pod testy
LDFLAGS = -shared -Wl,--wrap=malloc -Wl,--wrap=calloc -Wl,--wrap=realloc -Wl,--wrap=reallocarray -Wl,--wrap=free -Wl,--wrap=strdup -Wl,--wrap=strndup


# --- ZMIENNE DLA TESTÓW ---
# Nazwa Twojego pliku źródłowego z funkcją main() (zmień, jeśli nazywa się inaczej)
TEST_SRC = memory_tests.c
# Nazwa pliku wykonywalnego, który ma powstać
TEST_EXEC = tester

# --- CELE SPECJALNE ---
# .PHONY mówi make'owi, że "all" i "clean" to tylko nazwy akcji, a nie fizyczne pliki do stworzenia.
.PHONY: all clean test

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

# --- BUDOWANIE TESTERA ---
# Tworzy plik wykonywalny testera. Wymaga istnienia librstack.so.
$(TEST_EXEC): $(TEST_SRC) librstack.so
	$(CC) $(CFLAGS) $(TEST_SRC) -L. -lrstack -o -std=gnu23 -fPIC $@

# --- URUCHAMIANIE TESTÓW (Magia Make'a) ---
# Wpisanie "make test" zbuduje testera i od razu odpali go w Valgrindzie.
test: $(TEST_EXEC)
	@echo "--- Uruchamiam testy Valgrindem ---"
	env LD_LIBRARY_PATH=.:$$LD_LIBRARY_PATH valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./$(TEST_EXEC) 

# --- SPRZĄTANIE ---
# Cel "clean" pozwala łatwo usunąć wszystkie wygenerowane pliki poleceniem "make clean".
clean:
	rm -f *.o librstack.so