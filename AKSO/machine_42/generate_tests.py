#!/usr/bin/env python3
"""
generate_tests.py

Generator plikow testowych do porownania dwoch implementacji (asemblerowej
oraz C++) maszyny szyfrujacej Model 42.

Kazdy wygenerowany plik tests/test_NNNN.in ma format:

    linia 1: permutacja L (42 znaki z alfabetu Sigma) + '\n' (0x0A)
    linia 2: permutacja R (42 znaki z alfabetu Sigma) + '\n' (0x0A)
    linia 3: permutacja T (42 znaki z alfabetu Sigma) + '\n' (0x0A)
    reszta pliku: surowy tekst wejsciowy (0 lub wiecej bajtow, bez zadnej
                  dalszej modyfikacji, niekoniecznie zakonczony '\n')

Sigma = kody ASCII 49 ('1') .. 90 ('Z') wlacznie: 9 cyfr '1'-'9', potem
znaki ':', ';', '<', '=', '>', '?', '@', potem litery 'A'-'Z'. Razem 42 znaki.

WAZNA DECYZJA PROJEKTOWA: klucz startowy (para znakow lambda, rho) NIE jest
czescia formatu pliku testowego. Robimy tak celowo, zeby format pliku byl
niezalezny od klucza: plik opisuje wylacznie "maszyne" (permutacje L, R, T)
i "material" do przetworzenia (tekst wejsciowy). Klucz startowy jest za to
parametrem SPOSOBU URUCHOMIENIA testu, a nie danych testowych - oba badane
programy (implementacja asemblerowa i C++) przyjmuja klucz jako osobny
argument wywolania. Dzieki temu osobny skrypt run_tests.py moze uruchomic
KAZDY test z tym samym, ustalonym z gory kluczem startowym (ten sam punkt
odniesienia dla wszystkich przypadkow), bez potrzeby przechowywania klucza
w kazdym pliku *.in z osobna, i bez ryzyka niespojnosci miedzy plikiem
testowym a kluczem uzytym do jego wygenerowania.

Uzycie:
    python generate_tests.py [--seed 20260827] [--count 20] [--tests-dir tests]
"""

import argparse
import random
from pathlib import Path

# Sigma: ASCII 49 ('1') .. 90 ('Z') wlacznie -> 42 znaki.
SIGMA = [chr(c) for c in range(49, 91)]
assert len(SIGMA) == 42

# Kilka bajtow spoza Sigma, uzywanych do konstruowania przypadkow bledow.
# Wybrane tak, zeby pokryc rozne "rodzaje" niepoprawnosci: znak tuz ponizej
# zakresu, znak tuz powyzej zakresu, biala spacja, mala litera, bajt zerowy.
INVALID_BYTES = [
    ord("0"),   # 48, jeden ponizej dolnej granicy Sigma ('1' = 49)
    ord("["),   # 91, jeden powyzej gornej granicy Sigma ('Z' = 90)
    ord(" "),   # 32, spacja
    ord("a"),   # 97, mala litera
    0x00,       # bajt zerowy
    0x7F,       # DEL
]


def random_permutation(rng: random.Random) -> str:
    """Zwraca prawdziwa losowa permutacje (bijekcje) alfabetu Sigma."""
    perm = SIGMA.copy()
    rng.shuffle(perm)
    return "".join(perm)


def random_sigma_text(rng: random.Random, length: int) -> bytes:
    """Losowy tekst dlugosci `length` zlozony wylacznie ze znakow Sigma."""
    return "".join(rng.choices(SIGMA, k=length)).encode("ascii")


class TestWriter:
    """Nadaje kolejne numery testom i zapisuje je na dysk w formacie *.in."""

    def __init__(self, tests_dir: Path):
        self.tests_dir = tests_dir
        self.counter = 0

    def write(self, L: str, R: str, T: str, text: bytes, label: str = "") -> Path:
        self.counter += 1
        path = self.tests_dir / f"test_{self.counter:04d}.in"
        with open(path, "wb") as f:
            f.write((L + "\n").encode("ascii"))
            f.write((R + "\n").encode("ascii"))
            f.write((T + "\n").encode("ascii"))
            f.write(text)
        return path


def build_tests(writer: TestWriter, rng: random.Random, extra_count: int) -> None:
    # --- a) kilkanascie testow z losowymi permutacjami i losowym tekstem ---
    # dlugosci umiarkowane, rozrzucone w przedziale [1, 500].
    for _ in range(15):
        L, R, T = (random_permutation(rng) for _ in range(3))
        length = rng.randint(1, 500)
        text = random_sigma_text(rng, length)
        writer.write(L, R, T, text, "losowy tekst umiarkowanej dlugosci")

    # --- b) pusty tekst (0 bajtow) ---
    L, R, T = (random_permutation(rng) for _ in range(3))
    writer.write(L, R, T, b"", "pusty tekst")

    # --- c) tekst dlugosci dokladnie 1 znak ---
    L, R, T = (random_permutation(rng) for _ in range(3))
    text = random_sigma_text(rng, 1)
    writer.write(L, R, T, text, "tekst 1-znakowy")

    # --- d) granica bufora I/O (4096 bajtow): 4095, 4096, 4097 ---
    for length in (4095, 4096, 4097):
        L, R, T = (random_permutation(rng) for _ in range(3))
        text = random_sigma_text(rng, length)
        writer.write(L, R, T, text, f"granica bufora I/O, dlugosc {length}")

    # --- e) tekst obejmujacy wiele porcji 4096-bajtowych (9000..12000) ---
    L, R, T = (random_permutation(rng) for _ in range(3))
    length = rng.randint(9000, 12000)
    text = random_sigma_text(rng, length)
    writer.write(L, R, T, text, f"wiele porcji 4096B, dlugosc {length}")

    # --- f) niepoprawny znak (spoza Sigma) na POCZATKU tekstu ---
    L, R, T = (random_permutation(rng) for _ in range(3))
    bad = bytes([rng.choice(INVALID_BYTES)])
    rest = random_sigma_text(rng, rng.randint(0, 300))
    writer.write(L, R, T, bad + rest, "niepoprawny znak na poczatku tekstu")

    # --- g) niepoprawny znak W SRODKU pierwszej porcji 4096 bajtow ---
    L, R, T = (random_permutation(rng) for _ in range(3))
    before = random_sigma_text(rng, 2048)
    bad = bytes([rng.choice(INVALID_BYTES)])
    after = random_sigma_text(rng, 4096 - 2048 - 1)
    writer.write(L, R, T, before + bad + after,
                 "niepoprawny znak w srodku 1. porcji 4096B")

    # --- h) niepoprawny znak TUZ PO granicy pierwszej porcji 4096 bajtow ---
    # pierwsze 4096 bajtow poprawnych (caly pierwszy blok I/O), blad dopiero
    # jako pierwszy bajt drugiej porcji -> sprawdza czesciowy poprawny
    # output zapisany przed wykryciem bledu.
    L, R, T = (random_permutation(rng) for _ in range(3))
    before = random_sigma_text(rng, 4096)
    bad = bytes([rng.choice(INVALID_BYTES)])
    after = random_sigma_text(rng, 50)
    writer.write(L, R, T, before + bad + after,
                 "niepoprawny znak tuz po granicy 4096B")

    # --- i) osadzony znak nowej linii w SRODKU tekstu (musi byc bledem) ---
    L, R, T = (random_permutation(rng) for _ in range(3))
    before = random_sigma_text(rng, 100)
    after = random_sigma_text(rng, 100)
    writer.write(L, R, T, before + b"\n" + after,
                 "znak nowej linii osadzony w srodku tekstu")

    # --- j) kilka dlugich testow do wielokrotnego "okrazenia" wirnika rho ---
    # (i wielokrotnego wywolania kroku wirnika lambda) - dlugi losowy tekst
    # z Sigma o dlugosci znacznie przekraczajacej 42*3.
    for _ in range(5):
        L, R, T = (random_permutation(rng) for _ in range(3))
        length = rng.randint(42 * 3, 42 * 30)
        text = random_sigma_text(rng, length)
        writer.write(L, R, T, text,
                     f"dlugi tekst, wielokrotny obrot wirnikow, dlugosc {length}")

    # --- dodatkowe czysto losowe testy (liczba sterowana --count) ---
    for _ in range(extra_count):
        L, R, T = (random_permutation(rng) for _ in range(3))
        length = rng.randint(0, 2000)
        text = random_sigma_text(rng, length)
        writer.write(L, R, T, text, "dodatkowy losowy test")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generator danych testowych dla maszyny szyfrujacej Model 42."
    )
    parser.add_argument("--seed", type=int, default=20260827,
                         help="ziarno generatora liczb losowych (domyslnie 20260827)")
    parser.add_argument("--count", type=int, default=20,
                         help="dodatkowa liczba czysto losowych testow "
                              "poza zestawem edge-case (domyslnie 20)")
    parser.add_argument("--tests-dir", type=str, default="tests",
                         help='katalog docelowy na pliki testowe (domyslnie "tests")')
    args = parser.parse_args()

    rng = random.Random(args.seed)

    tests_dir = Path(args.tests_dir)
    tests_dir.mkdir(parents=True, exist_ok=True)

    writer = TestWriter(tests_dir)
    build_tests(writer, rng, args.count)

    print(f"Wygenerowano {writer.counter} plikow testowych w katalogu: "
          f"{tests_dir.resolve()}")


if __name__ == "__main__":
    main()
