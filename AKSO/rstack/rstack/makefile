CC = gcc

CFLAGS = -Wall -Wextra -Wno-implicit-fallthrough -std=gnu23 -fPIC -O2

LDFLAGS = -shared -Wl,--wrap=malloc -Wl,--wrap=calloc -Wl,--wrap=realloc -Wl,--wrap=reallocarray -Wl,--wrap=free -Wl,--wrap=strdup -Wl,--wrap=strndup

.PHONY: all clean test

all: librstack.so

librstack.so: rstack.o memory_tests.o
	$(CC) $(LDFLAGS) -o $@ $^

rstack.o: rstack.c rstack.h memory_tests.h
	$(CC) $(CFLAGS) -c $< -o $@

memory_tests.o: memory_tests.c memory_tests.h
	$(CC) $(CFLAGS) -c $< -o $@

# Cele do testowania. 
TEST_BATCH ?= test
test_%.o: ./tests_$(TEST_BATCH)/%.c macros.h
	gcc -c $< -o $@ $(CFLAGS)
test_%_executable: test_%.o librstack.so
	gcc $^ -o $@ -L . -lrstack

clean: # Czyści również pliki wynikowe testów, które mogą być generowane podczas testowania.
	rm -f *.o librstack.so test_*.fout test_*_executable test_*.o test.fout test.diff test.stdout test.valgrind test.make