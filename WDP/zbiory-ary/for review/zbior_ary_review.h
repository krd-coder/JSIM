#ifndef _ZBIOR_ARY_H_
#define _ZBIOR_ARY_H_

#include <stdbool.h>
#include <stdint.h>

typedef struct
{
  // All elements of [begin, end) that are x mod q. Because of the closed-open
  // implementation of Segment, int64_t are used to avoid int overflow.
  int64_t begin, end;
} Segment;

typedef struct
{
  // Works like std::vector<Segment>. Stores a sorted array of segments that
  // are remainder mod q. Once a SegmentVector is created, its values are never
  // changed so that new zbior_ary can point to it.
  uint64_t allocated_size;
  Segment* begin;
  Segment* end;
  int64_t remainder;
} SegmentVector;

typedef struct {
  // Works like std::vector<SegmentVector>. Stores an array of SegmentVectors
  // sorted by their remainder. By storing the beginning and past-the-end
  // pointers, zbior_ary can be iterated over more easily.
  uint64_t allocated_size;
  SegmentVector* begin;
  SegmentVector* end;
} zbior_ary;

void print(zbior_ary A);

/* Najlepiej nie modyfikowac nic ponizej. */

// Funkcja daje w wyniku zbior reprezentujacy ciag arytmetyczny o elemencie poczatkowym a, końcowym b i roznicy q>0, tj. {a,a+q,...,b}.
// Mozesz zalozyc, ze we wszystkich zapytaniach w danym tescie wartosc q bedzie taka sama.
// Mozesz zalozyc, ze ta funkcja zostanie wywolana jako pierwsza.
zbior_ary ciag_arytmetyczny(int a, int q, int b);

// Daje w wyniku zbior zlozony z samego elementu a, tj. {a}.
zbior_ary singleton(int a);

// Daje w wyniku zbior reprezentujacy teoriomnogosciowa sume zbiorow A i B.
zbior_ary suma(zbior_ary A, zbior_ary B);

// Daje w wyniku zbior reprezentujacy czesc wspolna zbiorow A i B.
zbior_ary iloczyn(zbior_ary A, zbior_ary B);

// Daje w wyniku zbior A \ B.
zbior_ary roznica(zbior_ary A, zbior_ary B);

// Daje w wyniku true wtw. gdy liczba b nalezy do zbioru A.
bool nalezy(zbior_ary A, int b);

// Wynikiem funkcji jest liczba elementow w zbiorze A.
unsigned moc(zbior_ary A);

// Wynikiem funkcji jest Ary_q(A), czyli minimalna liczba parami rozlacznych ciagow arytmetycznych o roznicy q, ktorych suma jest zbior A.
unsigned ary(zbior_ary A);

#endif
