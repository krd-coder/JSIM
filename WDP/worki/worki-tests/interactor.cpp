#include <bits/stdc++.h>
#include "worki.h"

using namespace std;

int main()
{
    int q;
    cin >> q;

    vector<przedmiot*> przedmioty;
    vector<worek*> worki;

    for (int i = 0; i < q; ++i)
    {
        int t;
        cin >> t;

        if (t == 0)
        {
            przedmioty.push_back(nowy_przedmiot());
        }
        else if (t == 1)
        {
            worki.push_back(nowy_worek());
        }
        else if (t == 2)
        {
            int a, b;
            cin >> a >> b;

            wloz(przedmioty[a], worki[b]);
        }
        else if (t == 3)
        {
            int a, b;
            cin >> a >> b;
            
            wloz(worki[a], worki[b]);
        }
        else if (t == 4)
        {
            int a;
            cin >> a;

            wyjmij(przedmioty[a]);
        }
        else if (t == 5)
        {
            int a;
            cin >> a;

            wyjmij(worki[a]);
        }
        else if (t == 6)
        {
            int a;
            cin >> a;

            cout << w_ktorym_worku(przedmioty[a]) << '\n';
        }
        else if (t == 7)
        {
            int a;
            cin >> a;

            cout << w_ktorym_worku(worki[a]) << '\n';
        }
        else if (t == 8)
        {
            int a;
            cin >> a;

            cout << ile_przedmiotow(worki[a]) << '\n';
        }
        else if (t == 9)
        {
            int a;
            cin >> a;

            na_odwrot(worki[a]);
        }
    }
    gotowe();
}