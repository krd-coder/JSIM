#include <queue>
#include <iostream>
#include <vector>
#include <cmath>
#include <algorithm>
#include <unordered_map>

using namespace std;

struct VectorHasher {
    size_t operator()(const std::vector<int>& v) const {
        size_t hash = 0;
        for (int i : v) {
            hash ^= std::hash<int>{}(i) + 0x9e3779b9 + (hash << 6) + (hash >> 2);
        }
        return hash;
    }
};

int count_gcd(int a, int b) {
    while (b) {
        a %= b;
        swap(a, b);
    }
    return a;
}

int BFS(vector<int> cap, vector<int> goal, int n)
{
    unordered_map<vector<int>,bool, VectorHasher> m;
    int ans = 2e9;
    queue<vector<int>> q1;
    queue<int> q2;
    q1.push(vector<int>(n,0));
    q2.push(0);
    while(ans == (int)2e9 && !q1.empty())
    {
        if (m.find(q1.front()) != m.end()) 
        {
            q1.pop();
            q2.pop();
            continue;
        }
        if (q1.front() == goal) 
        {
            ans=q2.front();
            break;
        }
        m[q1.front()] = 1; //mark as visited
        for (int i = 0; i < n; i++)
        {
            if(q1.front()[i] != cap[i])
            {
                vector<int> new_state = q1.front();
                new_state[i] = cap[i];
                q1.push(new_state);
                q2.push(q2.front() + 1);
            }
        }
        for (int i = 0; i < n; i++)
        {
            if(q1.front()[i])
            {
                vector<int> new_state = q1.front();
                new_state[i] = 0;
                q1.push(new_state);
                q2.push(q2.front() + 1);
            }
        }
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if(i == j)continue;
                if(q1.front()[i] && q1.front()[j]<cap[j])
                {
                    vector<int> new_state = q1.front();
                    int transfer = min(q1.front()[i], cap[j] - q1.front()[j]);
                    new_state[i] -= transfer;
                    new_state[j] += transfer;
                    q1.push(new_state);
                    q2.push(q2.front() + 1);
                }
            }
            
        }
        q1.pop();
        q2.pop();
    }
    if(ans == (int)2e9)ans =- 1;
    return ans;
}

int main()
{
    std::ios_base::sync_with_stdio(false);
    std::cin.tie(NULL);
    std::cout.tie(NULL);

    int n; cin >> n;
    vector<int> cap, goal;
    bool if_zero = true, if_full = true;
    int gcd = 1;
    for (int i = 0; i < n; i++)
    {
        int x, y;
        cin >> x >> y;
        if(x)
        {
            if(y)if_zero = false;
            if(x != y)if_full = false;
            cap.push_back(x);
            goal.push_back(y);
            if(i)gcd = count_gcd(gcd, x);
            else gcd = x;
        }
    }
    n = (int)goal.size();
    if(!n)
    {
        cout << 0 << endl;
        return 0;
    }
    for (int i = 0; i < n; i++)
    {
        if((goal[i] % gcd))
        {
            cout << -1 << endl;
            return 0;
        }
    }
    if(if_zero)
    {
        cout << 0 << endl;
        return 0;
    }
    if(if_full)
    {
        cout << n << endl;
        return 0;
    }
    cout<<BFS(cap, goal, n)<<endl;
    return 0;
}