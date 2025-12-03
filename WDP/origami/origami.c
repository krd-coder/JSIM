#include<stdlib.h>
#include<stdio.h>
#include<limits.h>

typedef long double ld;
ld epsilon = 0.000001;

typedef struct point
{
    ld x;
    ld y;
} point;

typedef struct line
{
    point a;
    point b;
} line;

typedef struct page page;
typedef struct page
{
    page* prev;
    line fold;
    int folds_amount;
    point a;//if rectangle - bottom left corner, if circle - center
    point b;//if rectangle - top right corner, if circle - X is the radius, Y is 0
    int if_rectangle; // 1 - rectangle, 0 - circle
} page;

int if_right(line l, point a)//1 if point is to the left, 0 if its on and -1 if its to the right of the line
{
    point perp;
    perp.x = l.b.y - l.a.y;
    perp.y = l.a.x - l.b.x;
    a.x -= l.a.x;
    a.y -= l.a.y;
    ld dot_product = a.x * perp.x + a.y * perp.y;
    if(dot_product > epsilon)return -1;
    if(dot_product < -epsilon)return 1;
    return 0;
}

point flip(point a, line l)
{
    point result = a;
    l.b.x -= l.a.x;
    l.b.y -= l.a.y;
    a.x -= l.a.x;
    a.y -= l.a.y;
    ld t = (ld)(a.x * l.b.x + a.y * l.b.y) / (ld)(l.b.x * l.b.x + l.b.y * l.b.y);
    point m;
    m.x = l.a.x + t * l.b.x;
    m.y = l.a.y + t * l.b.y;
    result.x = 2 * m.x - result.x;
    result.y = 2 * m.y - result.y;
    return result;    
}

int num_layers(page p, int folds, point pin)
{
    if(!folds)
    {
        if(p.if_rectangle)
        {
            if( p.a.x <= pin.x + epsilon && p.b.x >= pin.x - epsilon &&
                p.a.y <= pin.y + epsilon && p.b.y >= pin.y - epsilon )return 1;
            return 0;
        }
        ld dist = (pin.x - p.a.x) * (pin.x - p.a.x) + (pin.y - p.a.y) * (pin.y - p.a.y);
        ld radius = p.b.x * p.b.x;
        if(dist <= radius + epsilon)return 1;
        return 0;
    }
    switch (if_right(p.fold, pin))
    {
    case -1:
        return 0;
        break;
    case 0:
        return num_layers(*(p.prev), folds - 1, pin);
        break;
    case 1:
        return num_layers(*(p.prev), folds - 1, pin) + num_layers(*(p.prev), folds - 1, flip(pin, p.fold));
        break;
    }
    return 0;
}

int main()
{
    int n;
    int q;
    scanf("%d %d",&n, &q);
    page* pages = (struct page*)malloc(sizeof(struct page)*(unsigned long long)n);
    for (int i = 0; i < n; i++)
    {
        char type;
        scanf("\n%c ",&type);
        page p;
        point a;
        point b;
            int k = 0;
        switch (type)
        {
        case 'P':
            scanf("%Lf %Lf %Lf %Lf",&a.x, &a.y,&b.x, &b.y);
            p.a = a;
            p.b = b;
            p.if_rectangle = 1;
            p.folds_amount = 0;
            p.prev=NULL;
            break;
        case 'K':
            scanf("%Lf %Lf %Lf",&a.x, &a.y,&b.x);
            b.y = 0;
            p.a = a;
            p.b = b;
            p.if_rectangle = 0;
            p.folds_amount = 0;
            p.prev=NULL;
            break;
        case 'Z':
            scanf("%d %Lf %Lf %Lf %Lf",&k, &a.x, &a.y,&b.x, &b.y);
            line l;
            l.a=a;
            l.b=b;
            p = pages[k-1];
            p.folds_amount++;
            p.fold=l;
            p.prev = &pages[k-1];
            break;
        }
        pages[i]=p;
    }
    for (int i = 0; i < q; i++)
    {
        int k;
        point a;
        scanf("\n%d %Lf %Lf", &k ,&a.x, &a.y);
        k--;
        printf("%d\n", num_layers(pages[k], pages[k].folds_amount, a));
    }
    free(pages);
}