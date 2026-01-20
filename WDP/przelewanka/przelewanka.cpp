#include <iostream>
#include <vector>
#include<cmath>
#include<algorithm>
#include<unordered_map>

using namespace std;

struct VectorHasher {
    size_t operator()(const std::vector<int>& v) const {
        size_t hash = 0;
        for (int i : v) {
            // A common "hash combiner" trick to reduce collisions
            hash ^= std::hash<int>{}(i) + 0x9e3779b9 + (hash << 6) + (hash >> 2);
        }
        return hash;
    }
};

int BT(unordered_map<vector<int>,int, VectorHasher> &m, vector<int> state, vector<int> cap, vector<int> goal, int n, int moves, int &ans, bool &if_used_ans)
{
    if(moves>=ans)
    {
        if_used_ans=true;
        return m[state] = 2e9;
    }
    if (m.find(state) != m.end()) 
    {
        ans=min(ans,m[state]+moves);
        return m[state];
    }
    if (state == goal) 
    {
        ans=min(ans,moves);
        return m[state] = 0;
    }
    int res = 2e9;
    m[state] = 2e9; //mark as visited
    for (int i = 0; i < n; i++)
    {
        if(state[i]!=cap[i])
        {
            vector<int> new_state = state;
            new_state[i] = cap[i];
            res = min(BT(m, new_state, cap, goal, n, moves+1, ans, if_used_ans), res);
        }
    }
    for (int i = 0; i < n; i++)
    {
        if(state[i])
        {
            vector<int> new_state = state;
            new_state[i] = 0;
            res = min(BT(m, new_state, cap, goal, n, moves+1, ans, if_used_ans), res);
        }
    }
    for (int i = 0; i < n; i++)
    {
        for (int j = 0; j < n; j++)
        {
            if(i==j)continue;
            if(state[i] && state[j]<cap[j])
            {
                vector<int> new_state = state;
                int transfer = min(state[i], cap[j]-state[j]);
                new_state[i] -= transfer;
                new_state[j] += transfer;
                res = min(BT(m, new_state, cap, goal, n, moves+1, ans, if_used_ans), res);
            }
        }
        
    }
    

    if(res!=2e9)res++;
    return m[state] = res;
}

int solve(vector<int> cap, vector<int> goal, int n)
{
    int res = 2e9;
    for (int i = 0; i < 1000000 && res==2e9; i++)
    {
        int ans = i;
        vector<int> start(n,0);
        unordered_map<vector<int>,int, VectorHasher> m;
        bool if_used_ans=false;
        res=min(res,BT(m, start, cap, goal, n, 0, ans, if_used_ans));
        if(!if_used_ans)break;
    }

    return res;
}

int main()
{
    int n; cin>>n;
    vector<int> cap(n), start(n,0), goal(n);
    for (int i = 0; i < n; i++)
    {
        int x,y;
        cin>>x>>y;
        cap[i]=x;
        goal[i]=y;
    }
    int ans = solve(cap, goal, n);
    cout<<ans<<endl;
    return 0;
}