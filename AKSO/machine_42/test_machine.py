import subprocess, sys, random

SIGMA = [chr(c) for c in range(49, 91)]  # 42 znakow: '1'..'9', ':',';','<','=','>','?','@', 'A'..'Z'
assert len(SIGMA) == 42
IDX = {c: i for i, c in enumerate(SIGMA)}
BIN = "./machine_42"

def perm_shift(k):
    return "".join(SIGMA[(i + k) % 42] for i in range(42))

def perm_involution():
    # T[i] <-> T[41-i], involucja bez punktow stalych (42 jest parzyste)
    arr = [None] * 42
    for i in range(21):
        arr[i] = SIGMA[41 - i]
        arr[41 - i] = SIGMA[i]
    return "".join(arr)

def invert(p):
    inv = [None] * 42
    for i in range(42):
        inv[IDX[p[i]]] = SIGMA[i]
    return "".join(inv)

def ref_encrypt(L, R, T, lam0, rho0, text):
    L1, R1 = invert(L), invert(R)
    lam, rho = lam0, rho0
    out = []
    for ch in text:
        rho = SIGMA[(IDX[rho] + 1) % 42]
        if rho in ('L', 'R', 'T'):
            lam = SIGMA[(IDX[lam] + 1) % 42]
        x = IDX[ch]
        x = (x + IDX[rho]) % 42
        x = IDX[R[x]]
        x = (x - IDX[rho]) % 42
        x = (x + IDX[lam]) % 42
        x = IDX[L[x]]
        x = (x - IDX[lam]) % 42
        x = IDX[T[x]]
        x = (x + IDX[lam]) % 42
        x = IDX[L1[x]]
        x = (x - IDX[lam]) % 42
        x = (x + IDX[rho]) % 42
        x = IDX[R1[x]]
        x = (x - IDX[rho]) % 42
        out.append(SIGMA[x])
    return "".join(out), lam, rho

def run(args, stdin_bytes, timeout=5):
    p = subprocess.run([BIN] + args, input=stdin_bytes, stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE, timeout=timeout)
    return p.returncode, p.stdout, p.stderr

random.seed(42)
L = perm_shift(3)
R = perm_shift(5)
T = perm_involution()
key = "11"  # lambda = '1' (idx0), rho = '1' (idx0)

failures = []

def check(name, cond):
    status = "OK" if cond else "FAIL"
    print(f"[{status}] {name}")
    if not cond:
        failures.append(name)

# --- 1. Walidacja argc ---
rc, out, err = run([L, R], b"")
check("zly argc -> exit 1", rc == 1)

# --- 2. Zla dlugosc permutacji ---
rc, out, err = run([L[:-1], R, T, key], b"")
check("L za krotkie -> exit 1", rc == 1)

# --- 3. Zly znak w permutacji (np. spacja) ---
badL = " " + L[1:]
rc, out, err = run([badL, R, T, key], b"")
check("znak spoza Sigma w L -> exit 1", rc == 1)

# --- 4. Zla dlugosc klucza ---
rc, out, err = run([L, R, T, "1"], b"")
check("za krotki klucz -> exit 1", rc == 1)

# --- 5. Pusty strumien (natychmiastowy EOF) -> exit 0, brak wyjscia ---
rc, out, err = run([L, R, T, key], b"")
check("pusty strumien -> exit 0", rc == 0 and out == b"")

# --- 6. Znak nowej linii na wejsciu jest bledem ---
rc, out, err = run([L, R, T, key], b"1\n")
check("nowa linia na wejsciu -> exit 1", rc == 1)

# --- 7. Poprawne szyfrowanie, porownanie z implementacja referencyjna w Pythonie ---
plaintext = "".join(random.choice(SIGMA) for _ in range(500))
ref_cipher, lam_end, rho_end = ref_encrypt(L, R, T, "1", "1", plaintext)
rc, out, err = run([L, R, T, key], plaintext.encode("ascii"))
check("exit 0 dla poprawnego strumienia", rc == 0)
check("dlugosc wyjscia == dlugosc wejscia", len(out) == len(plaintext))
match = out.decode("ascii", errors="replace") == ref_cipher
check("szyfrogram zgodny z implementacja referencyjna (500 znakow)", match)
if not match:
    for i, (a, b) in enumerate(zip(out.decode("ascii", errors="replace"), ref_cipher)):
        if a != b:
            print(f"  pierwsza roznica na pozycji {i}: asm={a!r} ref={b!r}")
            break

print()
if failures:
    print(f"NIEPOWODZENIA: {failures}")
    sys.exit(1)
else:
    print("WSZYSTKIE TESTY PRZESZLY")
