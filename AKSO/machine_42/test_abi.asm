default rel

section .bss
    Lp   resb 42
    L1p  resb 42
    Rp   resb 42
    R1p  resb 42
    Tp   resb 42
    txt  resb 4

section .text
    global _start
    extern machine_42_function

_start:
    lea   rdi, [rel Lp]
    call  fill_identity
    lea   rdi, [rel L1p]
    call  fill_identity
    lea   rdi, [rel Rp]
    call  fill_identity
    lea   rdi, [rel R1p]
    call  fill_identity
    lea   rdi, [rel Tp]
    call  fill_identity

    mov   byte [rel txt], 49

    mov   rbx, 0x1111111111111111
    mov   rbp, 0x2222222222222222
    mov   r12, 0x3333333333333333
    mov   r13, 0x4444444444444444
    mov   r14, 0x5555555555555555
    mov   r15, 0x6666666666666666

    lea   rdi, [rel Lp]
    lea   rsi, [rel L1p]
    lea   rdx, [rel Rp]
    lea   rcx, [rel R1p]
    lea   r8, [rel Tp]
    mov   r9w, 0x3131

    lea   rax, [rel txt]
    push  qword 1
    push  rax
    call  machine_42_function
    add   rsp, 16

    cmp   rbx, 0x1111111111111111
    jne   .fail
    cmp   rbp, 0x2222222222222222
    jne   .fail
    cmp   r12, 0x3333333333333333
    jne   .fail
    cmp   r13, 0x4444444444444444
    jne   .fail
    cmp   r14, 0x5555555555555555
    jne   .fail
    cmp   r15, 0x6666666666666666
    jne   .fail

    mov   rax, 60
    xor   rdi, rdi
    syscall
.fail:
    mov   rax, 60
    mov   rdi, 1
    syscall

fill_identity:
    xor   rcx, rcx
.fi_loop:
    cmp   rcx, 42
    jge   .fi_done
    mov   al, cl
    add   al, 49
    mov   [rdi + rcx], al
    inc   rcx
    jmp   .fi_loop
.fi_done:
    ret
