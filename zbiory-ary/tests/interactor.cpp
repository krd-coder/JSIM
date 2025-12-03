#include <bits/stdc++.h>
#include "zbior_ary.h"

using namespace std;

int main()
{
    // ios_base::sync_with_stdio(false);
    // cin.tie(0);

    int t;
    bool p;
    cin >> t >> p;

    vector<zbior_ary> v;

    for (int i = 0; i < t; ++i)
    {
        int type;
        cin >> type;

        if (type == 0)
        {
            int a, q, b;
            cin >> a >> q >> b;
            v.push_back(ciag_arytmetyczny(a, q, b));
            if (p) print(v.back());
        }
        else if (type == 1)
        {
            int a;
            cin >> a;
            v.push_back(singleton(a));
            if (p) print(v.back());
        }
        else if (type == 2)
        {
            int a, b;
            cin >> a >> b;
            v.push_back(suma(v[a], v[b]));
            if (p) print(v.back());
        }
        else if (type == 3)
        {
            int a, b;
            cin >> a >> b;
            v.push_back(iloczyn(v[a], v[b]));
            if (p) print(v.back());
        }
        else if (type == 4)
        {
            int a, b;
            cin >> a >> b;

            v.push_back(roznica(v[a], v[b]));
            if (p) print(v.back());
        }
        else if (type == 5)
        {
            int idx, a;
            cin >> idx >> a;
            cout << nalezy(v[idx], a) << '\n';
        }
        else if (type == 6)
        {
            int a;
            cin >> a;
            cout << moc(v[a]) << '\n';
        }
        else if (type == 7)
        {
            int a;
            cin >> a;
            cout << ary(v[a]) << '\n';
        }
    }
}