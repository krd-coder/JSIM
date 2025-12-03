#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <limits.h>
#include <stdbool.h>
#include <string.h>

typedef long double ld;

typedef struct{
    ld x;
    ld y;
} complex;

typedef struct{
    complex p_1;
    complex p_2;
} rectangle_t;

typedef rectangle_t line_t;

typedef struct{
    complex p_s;
    ld r;
} circle_t;

typedef enum {
    RECTANGLE,
    CIRCLE,
    FOLDED
} shape_type;

typedef struct Figure Figure;
typedef Figure* figure_ptr;

typedef struct Figure {
    shape_type type;
    union {
        rectangle_t rect;
        circle_t circ;
        line_t line;
    } fig;
    figure_ptr prev_ptr;
} Figure;

static inline complex mul(complex a, complex b){
    complex res;
    res.x = a.x * b.x - a.y * b.y;
    res.y = a.x * b.y + b.x * a.y;
    return res;
}
static inline complex conjugate(complex a){
    return (complex){a.x, -a.y};
}

const ld eps = 1e-6;

int cross_product(line_t l, complex p){
    ld res = (p.x - l.p_1.x) * (l.p_2.y - l.p_1.y)
     - (l.p_2.x-l.p_1.x) * (p.y - l.p_1.y);
    if(res < eps && res > -eps) 
        return 0;
    else return (res > 0) ? 1 : -1;
}

static inline complex reflection(complex p, line_t l) {
    complex a = l.p_1;
    complex v = (complex){l.p_2.x - l.p_1.x, l.p_2.y - l.p_1.y};
    complex z = (complex){p.x - a.x, p.y - a.y};

    complex z_conj_v = mul(z, conjugate(v));
    ld __denominator = v.x*v.x + v.y*v.y;

    complex tmp = mul(v, (complex){ z_conj_v.x/__denominator, -z_conj_v.y/__denominator});

    return (complex){ tmp.x + a.x, tmp.y + a.y };
}
bool is_in_figure(complex p, figure_ptr f){
    if(f->type == RECTANGLE){
        if(p.x < f->fig.rect.p_1.x - eps || p.x > f->fig.rect.p_2.x + eps){
            return false;
        }
        if(p.y < f->fig.rect.p_1.y - eps || p.y > f->fig.rect.p_2.y + eps){
            return false;
        }
        return true;
    } else if(f->type == CIRCLE){
        ld dist = (p.x-f->fig.circ.p_s.x) * (p.x-f->fig.circ.p_s.x) + (p.y-f->fig.circ.p_s.y) * (p.y-f->fig.circ.p_s.y);
        if(dist <= f->fig.circ.r * f->fig.circ.r + eps)
            return true;
        else return false;
    }
    return true;
}

int unfold(complex p, figure_ptr f){
    if(f->type == FOLDED){
        int prod = cross_product(f->fig.line, p);
        //case kiedy kopiujemy punkt
        if(prod < 0){
            complex p_reflected = reflection(p, f->fig.line);
            int ans_1 = unfold(p_reflected, f->prev_ptr);
            int ans_2 = unfold(p, f->prev_ptr);
            return ans_1 + ans_2;
        } else if(prod == 0){
            int ans_1 = unfold(p, f->prev_ptr);
            return ans_1;
        } else return 0;
    } else{
        bool in_figure = is_in_figure(p, f);
        return in_figure;
    }
}
int n,q;
int main(){
    scanf("%d %d", &n, &q);
    Figure* fig_arr = (Figure*)malloc(sizeof(Figure) * (size_t)(n+1));
    for(int i=0;i<n;i++){
        char c;
        scanf(" %c", &c);
        if(c == 'P'){
            ld x_1,y_1,x_2,y_2;
            scanf("%Lf %Lf %Lf %Lf", &x_1, &y_1, &x_2, &y_2);
            fig_arr[i].type = RECTANGLE;
            fig_arr[i].fig.rect = (rectangle_t){
                .p_1 = (complex){.x = x_1, .y = y_1},
                .p_2 = (complex){.x = x_2, .y = y_2}
            };
            fig_arr[i].prev_ptr = NULL;
        } else if(c == 'K'){
            ld x_s,y_s,r;
            scanf("%Lf %Lf %Lf", &x_s, &y_s, &r);
            fig_arr[i].type = CIRCLE;
            fig_arr[i].fig.circ = (circle_t){
                .p_s = (complex){.x = x_s, .y = y_s},
                .r = r
            };
            fig_arr[i].prev_ptr = NULL;
        } else if(c == 'Z'){
            int k;
            ld x_1,y_1,x_2,y_2;
            scanf("%d %Lf %Lf %Lf %Lf", &k, &x_1, &y_1, &x_2, &y_2);
            fig_arr[i].type = FOLDED; 
            fig_arr[i].fig.line= (rectangle_t){
                .p_1 = (complex){.x = x_1, .y = y_1},
                .p_2 = (complex){.x = x_2, .y = y_2}
            };
            fig_arr[i].prev_ptr = fig_arr + k - 1; 
        }
    }
    for(int i=0;i<q;i++){
        int k;
        ld x, y;
        scanf("%d %Lf %Lf", &k, &x, &y);
        complex p = (complex){.x = x, .y = y};
        int res = unfold(p, &fig_arr[k-1]);
        printf("%d\n", res);
    }
    free(fig_arr);
    return 0;
}