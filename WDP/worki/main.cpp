#include "worki.h"
#include <assert.h>
#include <vector>

using namespace std;

int main(){
    int n = 4;
    vector<przedmiot*> przedmioty;
    vector<worek*> worki;

    for (int i = 0; i < n; ++i){
        przedmioty.push_back(nowy_przedmiot());
        worki.push_back(nowy_worek());
        wloz(przedmioty[i], worki[i]);
        if (i > 0)
            wloz(worki[i - 1], worki[i]);
    }

    for (int i = 0; i < n; ++i){
        assert(w_ktorym_worku(przedmioty[i]) == i);
        assert(w_ktorym_worku(worki[i]) == (i < n - 1 ? i + 1 : -1));
        assert(ile_przedmiotow(worki[i]) == i + 1);
    }

    for (int i = n - 1; i >= 0; --i){
        if (i < n - 1)
            wyjmij(worki[i]);
        wyjmij(przedmioty[i]);
    }

    for (int i = 0; i < n; ++i){
        assert(w_ktorym_worku(przedmioty[i]) == -1);
        assert(w_ktorym_worku(worki[i]) == -1);
        assert(ile_przedmiotow(worki[i]) == 0);
    }

    na_odwrot(worki[0]);

    assert(w_ktorym_worku(przedmioty[0]) == 0);
    assert(w_ktorym_worku(worki[0]) == -1);
    assert(ile_przedmiotow(worki[0]) == n);
    for (int i = 1; i < n; ++i){
        assert(w_ktorym_worku(przedmioty[i]) == 0);
        assert(w_ktorym_worku(worki[i]) == 0);
        assert(ile_przedmiotow(worki[i]) == 0);
    }

    gotowe();

    return 0;
}
