#include <iostream>
#include <vector>
#include<cmath>

using namespace std;

struct node
{
    node* prev;
    node* next;
    node* min;
    node* max;
    int index;
    double val;
};

bool comp(node* a, node* b)
{
    if(abs(a->val-b->val)<0.000001)return a->index > b->index;
    return a->val<b->val;
}

class p_queue
{
    public:
    int size = 0;
    p_queue()
    {
        MID = new node();
        FRONT=MID;
        BACK=MID;
    }       
    void push(int _index, double _val)
    {
        size++;
        node *Node = new node;
        Node->index = _index;
        Node->val = _val;
        Node->prev = FRONT;
        Node->next = nullptr;
        FRONT->next=Node;
        if(FRONT!=MID)
        {
                if(comp(Node, FRONT->min))
                Node->min = Node;
            else
                Node->min = FRONT->min;

            if(comp(FRONT->max, Node))
                Node->max = Node;
            else
                Node->max = FRONT->max;
        }
        else 
        {
            Node->max=Node;
            Node->min=Node;
        }
        FRONT=Node;
    }
    void pop()
    {
        if(!size)return;
        if(BACK == MID)
        {
            for (int i = 0; i < size; i++)
            {
                node *tmp = FRONT->prev;
                FRONT->next = BACK;
                FRONT->prev = nullptr;
                BACK->prev = FRONT;   
                BACK = FRONT;
                FRONT = tmp;
                tmp->next=nullptr;
                if(i)
                {
                    if(comp(BACK, BACK->next->min))
                        BACK->min = BACK;
                    else
                        BACK->min = BACK->next->min;
                    if(comp(BACK->next->max, BACK))
                        BACK->max = BACK;
                    else
                        BACK->max = BACK->next->max;
                }
                else
                {
                    BACK->min=BACK;
                    BACK->max=BACK;
                }
            }            
        }
        size--;
        node* tmp = BACK;
        BACK = BACK->next;
        BACK->prev = nullptr;
        delete tmp;
    }
    pair<int,int> min_element()
    {
        if(!size)return pr(nullptr);
        if(BACK==MID)return pr(FRONT->min);
        if(FRONT==MID)return pr(BACK->min);
        return comp(FRONT->min, BACK->min) ? pr(FRONT->min) : pr(BACK->min);
    }
    pair<int,int> max_element()
    {
        if(!size)return pr(nullptr);
        if(BACK==MID)return pr(FRONT->max);
        if(FRONT==MID)return pr(BACK->max);
        return comp(BACK->max, FRONT->max) ? pr(FRONT->max) : pr(BACK->max);
    }
    pair<int,int> front()
    {
        if(!size)return pr(nullptr);
        if(FRONT==MID)return pr(MID->prev);
        return pr(FRONT);
    }
    pair<int,int> back()
    {
        if(!size)return pr(nullptr);
        if(BACK==MID)return pr(MID->next);
        return pr(BACK);
    }
    int length()
    {
        return size;
    }
    void clear()
    {
        while (size)pop();        
    }
    private:
    node *FRONT, *BACK, *MID;
    pair<int,int> pr(node* Node)
    {
        if(Node==nullptr)return {-1,-1};
        return {Node->index, (int)Node->val};
    }
};

int main()
{
    int n, U;
    cin>>n>>U;
    p_queue Q;
    vector<int> max_seg(n,0);
    vector<int> result(n);
    vector<pair<int,int>> points;
    for (int i = 0; i < n; i++)
    {
        int x,y;
        cin>>x>>y;
        points.push_back({x,y});
        Q.push(i,y);
        while (Q.max_element().second - Q.min_element().second > U)
            Q.pop();
        max_seg[i]=Q.length();
    }
    
    Q.clear();

    for (int i = n-1; i >=0;)
    {
        double val = ((double)points[i-max_seg[i]+1].first-(double)points[i].first);
        val*=val;
        val/=max_seg[i];
        Q.push(i, val);

        do
        {
            while (Q.back().first-max_seg[Q.back().first]+1>i)Q.pop();
            result[i]=Q.max_element().first;
            i--;
        }
        while(i>=0 && max_seg[i]+1==max_seg[i+1]);
    }

    for (int i = 0; i < n; i++)cout<< result[i]-max_seg[result[i]]+2<<" "<< result[i]+1<<endl;
    return 0;
}