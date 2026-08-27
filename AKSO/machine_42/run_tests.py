#!/usr/bin/env python3
"""
run_tests.py -- porownuje dwie implementacje maszyny szyfrujacej Model 42:
    - wersje asemblerowa (machine_42.asm + machine_42_function.asm, budowane
      przez nasm + ld),
    - wersje C++ (machine_42.cpp, budowana przez g++),
na podstawie plikow testowych z katalogu tests/.

UWAGA -- WYMAGANY LINUKS:
    Ten skrypt MUSI byc uruchamiany na Linuksie (np. wewnatrz WSL, jesli
    pracujesz na Windows). Wersja asemblerowa (machine_42) korzysta z
    surowych syscalli Linux (sys_read / sys_write / sys_exit), wiec zbudowana
    binarka nie zadziala na Windows ani na macOS. Uruchomienie tego skryptu
    poza Linuksem zakonczy sie bledem juz na etapie budowania (ld wyprodukuje
    plik ELF, ktorego nie da sie wykonac) lub uruchamiania binarki.

Uzycie:
    python3 run_tests.py

Zaklada sie, ze skrypt jest uruchamiany z katalogu, w ktorym lezy (czyli
katalogu zawierajacego machine_42.asm, machine_42_function.asm, machine_42.cpp
oraz podkatalog tests/).
"""

import subprocess
import sys
from pathlib import Path

# Format pliku testowego (test_NNNN.in) wedlug specyfikacji zawiera TYLKO
# permutacje L, R, T (po 42 znaki, kazda zakonczona 0x0A) oraz surowy tekst
# wejsciowy -- celowo NIE zawiera klucza startowego wirnikow. Klucz trzeba
# wiec ustalic z gory i uzyc tego samego dla wszystkich testow, zeby obie
# implementacje byly porownywane w tych samych warunkach.
FIXED_KEY = "11"

# Katalog roboczy = katalog, w ktorym lezy ten skrypt.
BASE_DIR = Path(__file__).resolve().parent
TESTS_DIR = BASE_DIR / "tests"

ASM_MAIN = BASE_DIR / "machine_42.asm"
ASM_FUNC = BASE_DIR / "machine_42_function.asm"
CPP_SRC = BASE_DIR / "machine_42.cpp"

OBJ_MAIN = BASE_DIR / "machine_42.o"
OBJ_FUNC = BASE_DIR / "machine_42_function.o"
BIN_ASM = BASE_DIR / "machine_42"
BIN_CPP = BASE_DIR / "machine_42_cpp"

SIGMA_MIN = 49  # '1'
SIGMA_MAX = 90  # 'Z'
PERM_LEN = 42


def run_build_step(cmd, description):
    """Uruchamia pojedynczy krok budowania; przerywa caly skrypt w razie bledu."""
    print(f"[build] {description}: {' '.join(cmd)}")
    try:
        result = subprocess.run(
            cmd, cwd=str(BASE_DIR), capture_output=True, text=True
        )
    except FileNotFoundError as exc:
        print(f"BLAD: nie mozna uruchomic '{cmd[0]}': {exc}", file=sys.stderr)
        print(
            "Upewnij sie, ze skrypt jest uruchamiany na Linuksie (np. w WSL) "
            "i ze nasm/ld/g++ sa zainstalowane.",
            file=sys.stderr,
        )
        sys.exit(1)

    if result.returncode != 0:
        print(f"BLAD BUDOWANIA podczas kroku: {description}", file=sys.stderr)
        print(f"Polecenie: {' '.join(cmd)}", file=sys.stderr)
        print(f"Kod wyjscia: {result.returncode}", file=sys.stderr)
        if result.stdout:
            print("--- stdout ---", file=sys.stderr)
            print(result.stdout, file=sys.stderr)
        if result.stderr:
            print("--- stderr ---", file=sys.stderr)
            print(result.stderr, file=sys.stderr)
        sys.exit(1)


def build_all():
    """Buduje obie implementacje (asemblerowa i C++). Przerywa caly skrypt
    z czytelnym komunikatem i niezerowym kodem wyjscia, jesli ktorykolwiek
    krok budowania zawiedzie."""
    if not ASM_MAIN.exists() or not ASM_FUNC.exists():
        print(
            f"BLAD: brak plikow zrodlowych asemblera "
            f"({ASM_MAIN.name} / {ASM_FUNC.name}) w {BASE_DIR}",
            file=sys.stderr,
        )
        sys.exit(1)
    if not CPP_SRC.exists():
        print(f"BLAD: brak pliku zrodlowego C++ ({CPP_SRC.name}) w {BASE_DIR}", file=sys.stderr)
        sys.exit(1)

    run_build_step(
        ["nasm", "-f", "elf64", str(ASM_MAIN), "-o", str(OBJ_MAIN)],
        "asemblacja machine_42.asm",
    )
    run_build_step(
        ["nasm", "-f", "elf64", str(ASM_FUNC), "-o", str(OBJ_FUNC)],
        "asemblacja machine_42_function.asm",
    )
    run_build_step(
        ["ld", str(OBJ_MAIN), str(OBJ_FUNC), "-o", str(BIN_ASM)],
        "linkowanie machine_42",
    )
    run_build_step(
        ["g++", "-O2", "-std=c++17", "-Wall", "-Wextra", str(CPP_SRC), "-o", str(BIN_CPP)],
        "kompilacja machine_42.cpp",
    )
    print("[build] Obie implementacje zbudowane pomyslnie.\n")


def parse_test_file(path):
    """Parsuje plik testowy: pierwsze 3 linie to permutacje L, R, T
    (kazda zakonczona 0x0A), reszta to surowy tekst wejsciowy bez zadnej
    modyfikacji."""
    data = path.read_bytes()

    lines = []
    offset = 0
    for _ in range(3):
        nl_index = data.find(b"\n", offset)
        if nl_index == -1:
            raise ValueError(
                f"plik {path.name}: nie znaleziono 3 linii naglowka (L, R, T) "
                f"zakonczonych 0x0A"
            )
        lines.append(data[offset:nl_index])
        offset = nl_index + 1

    L, R, T = (line.decode("ascii", errors="replace") for line in lines)
    text = data[offset:]  # surowy tekst wejsciowy, bez modyfikacji
    return L, R, T, text


def run_binary(binary, L, R, T, key, text_bytes):
    """Uruchamia dana binarke z podanymi argumentami i tekstem na stdin."""
    try:
        result = subprocess.run(
            [str(binary), L, R, T, key],
            input=text_bytes,
            capture_output=True,
        )
    except FileNotFoundError as exc:
        print(f"BLAD: nie mozna uruchomic {binary}: {exc}", file=sys.stderr)
        sys.exit(1)
    return result


def find_first_diff(a: bytes, b: bytes):
    """Zwraca indeks pierwszego bajtu, na ktorym a i b sie roznia, albo None
    jesli sa identyczne (jedno moze byc prefiksem drugiego -- wtedy zwraca
    indeks = dlugosc krotszego)."""
    min_len = min(len(a), len(b))
    for i in range(min_len):
        if a[i] != b[i]:
            return i
    if len(a) != len(b):
        return min_len
    return None


def format_context(data: bytes, index: int, radius: int = 8):
    """Zwraca czytelny fragment `data` wokol `index` (repr bajtow)."""
    start = max(0, index - radius)
    end = min(len(data), index + radius + 1)
    fragment = data[start:end]
    marker_pos = index - start
    return f"[{start}:{end}] = {fragment!r}  (pozycja roznicy w fragmencie: {marker_pos})"


def describe_byte(data: bytes, index: int):
    if index < len(data):
        b = data[index]
        return f"0x{b:02x} ({b!r})"
    return "<brak bajtu -- koniec strumienia>"


def compare_outputs(name, asm_result, cpp_result):
    """Porownuje wyniki obu implementacji dla jednego testu. Zwraca
    (passed: bool, details: list[str])."""
    details = []
    passed = True

    if asm_result.returncode != cpp_result.returncode:
        passed = False
        details.append(
            f"  kod wyjscia: asm={asm_result.returncode}  cpp={cpp_result.returncode}"
        )

    asm_out = asm_result.stdout
    cpp_out = cpp_result.stdout
    diff_index = find_first_diff(asm_out, cpp_out)

    if diff_index is not None:
        passed = False
        details.append(f"  dlugosc stdout: asm={len(asm_out)} bajtow  cpp={len(cpp_out)} bajtow")
        details.append(
            f"  pierwsza roznica na pozycji {diff_index}: "
            f"asm={describe_byte(asm_out, diff_index)}  "
            f"cpp={describe_byte(cpp_out, diff_index)}"
        )
        details.append(f"  kontekst asm: {format_context(asm_out, diff_index)}")
        details.append(f"  kontekst cpp: {format_context(cpp_out, diff_index)}")

    return passed, details


def main():
    build_all()

    if not TESTS_DIR.is_dir():
        print(f"BLAD: katalog testow nie istnieje: {TESTS_DIR}", file=sys.stderr)
        sys.exit(1)

    test_files = sorted(TESTS_DIR.glob("test_*.in"))
    if not test_files:
        print(f"BLAD: brak plikow test_*.in w {TESTS_DIR}", file=sys.stderr)
        sys.exit(1)

    print(f"Uzyty stalt klucz startowy dla wszystkich testow: FIXED_KEY={FIXED_KEY!r}")
    print(f"Znaleziono {len(test_files)} plikow testowych.\n")

    failed_names = []
    passed_count = 0

    for test_path in test_files:
        name = test_path.name
        try:
            L, R, T, text = parse_test_file(test_path)
        except ValueError as exc:
            print(f"FAIL {name}  (blad parsowania pliku testowego: {exc})")
            failed_names.append(name)
            continue

        if len(L) != PERM_LEN or len(R) != PERM_LEN or len(T) != PERM_LEN:
            print(
                f"FAIL {name}  (nieprawidlowa dlugosc permutacji: "
                f"L={len(L)} R={len(R)} T={len(T)}, oczekiwano {PERM_LEN})"
            )
            failed_names.append(name)
            continue

        asm_result = run_binary(BIN_ASM, L, R, T, FIXED_KEY, text)
        cpp_result = run_binary(BIN_CPP, L, R, T, FIXED_KEY, text)

        passed, details = compare_outputs(name, asm_result, cpp_result)

        if passed:
            print(f"PASS {name}")
            passed_count += 1
        else:
            print(f"FAIL {name}")
            for line in details:
                print(line)
            failed_names.append(name)

    total = len(test_files)
    print()
    print(f"Podsumowanie: {passed_count}/{total} testow przeszlo.")
    if failed_names:
        print("Testy ktore nie przeszly:")
        for name in failed_names:
            print(f"  - {name}")
        sys.exit(1)
    else:
        print("Wszystkie testy przeszly.")
        sys.exit(0)


if __name__ == "__main__":
    main()
