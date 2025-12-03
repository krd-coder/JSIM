#include<stdlib.h>
#include<stdio.h>
#include<limits.h>

int min(int a, int b)
{
    return (a<b) ? a : b;
}

int max(int a, int b)
{
    return (a>b) ? a : b;
}

struct motel
{
    int index;
    int pos;
};

int main()
{
    int n;
    scanf("%d",&n);
    struct motel* motels = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mn_l1 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mn_l2 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mn_r1 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mn_r2 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mx_l1 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mx_l2 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mx_r1 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mx_r2 = (struct motel*)malloc(sizeof(struct motel)*(unsigned int)n);
    struct motel* mn_buffer = (struct motel*)malloc(sizeof(struct motel)*4);
    struct motel* mx_buffer = (struct motel*)malloc(sizeof(struct motel)*4);
    for (int i = 0; i < 4; i++)
    {
        mx_buffer[i].index=-1;
        mx_buffer[i].pos=INT_MAX/2;
        mn_buffer[i].index=-1;
        mn_buffer[i].pos=-INT_MAX/2;
    }
    int mx_buffer_size = 0;
    for (int i = 0; i < n; i++)
    {
        scanf("%d",&motels[i].index);
        scanf("%d",&motels[i].pos);

        int ii=3;
        if(mn_buffer[ii].index==motels[i].index)ii--;
        mn_l1[i]=mn_buffer[ii];
        ii--;
        if(mn_buffer[ii].index==motels[i].index)ii--;
        mn_l2[i]=mn_buffer[ii];
        if(motels[i].index!=mn_buffer[3].index)
        {
            mn_buffer[0]=mn_buffer[1];
            mn_buffer[1]=mn_buffer[2];
            mn_buffer[2]=mn_buffer[3];
        }
        mn_buffer[3]=motels[i];

        ii=0;
        if(mx_buffer_size<4 && (!mx_buffer_size || mx_buffer[mx_buffer_size-1].index!=motels[i].index))
        {
            mx_buffer[mx_buffer_size]=motels[i];
            mx_buffer_size+=1;
        }
        if(mx_buffer[ii].index==motels[i].index)ii++;
        mx_l1[i]=mx_buffer[ii];
        ii++;
        if(mx_buffer[ii].index==motels[i].index)ii++;
        mx_l2[i]=mx_buffer[ii];
    }   
    for (int i = 0; i < 4; i++)
    {
        mx_buffer[i].index=-1;
        mx_buffer[i].pos=-INT_MAX/2;
        mn_buffer[i].index=-1;
        mn_buffer[i].pos=INT_MAX/2;
    }
    mx_buffer_size=0;
    for (int i = n-1; i >=0; i--)
    {
        int ii=3;
        if(mn_buffer[ii].index==motels[i].index)ii--;
        mn_r1[i]=mn_buffer[ii];
        ii--;
        if(mn_buffer[ii].index==motels[i].index)ii--;
        mn_r2[i]=mn_buffer[ii];
        if(motels[i].index!=mn_buffer[3].index)
        {
            mn_buffer[0]=mn_buffer[1];
            mn_buffer[1]=mn_buffer[2];
            mn_buffer[2]=mn_buffer[3];
        }
        mn_buffer[3]=motels[i];

        ii=0;
        if(mx_buffer_size<4 && (!mx_buffer_size || mx_buffer[mx_buffer_size-1].index!=motels[i].index))
        {
            mx_buffer[mx_buffer_size]=motels[i];
            mx_buffer_size++;
        }
        if(mx_buffer[ii].index==motels[i].index)ii++;
        mx_r1[i]=mx_buffer[ii];
        ii++;
        if(mx_buffer[ii].index==motels[i].index)ii++;
        mx_r2[i]=mx_buffer[ii];
    }   
    int result_min = INT_MAX/2;
    int result_max=-INT_MAX/2;
    for (int i = 0; i < n; i++)
    {
        int pos = motels[i].pos;
        int tmp_min=0;
        if(mn_l1[i].index == mn_r1[i].index)
        {
            int tmp_min1 = (mn_l1[i].index!=-1 && mn_r2[i].index!=-1 && mn_l1[i].index!=mn_r2[i].index) ? max(pos - mn_l1[i].pos, mn_r2[i].pos-pos) : INT_MAX/2;
            int tmp_min2 = (mn_l2[i].index!=-1 && mn_r1[i].index!=-1 && mn_l2[i].index!=mn_r1[i].index) ? max(pos - mn_l2[i].pos, mn_r1[i].pos-pos) : INT_MAX/2;
            tmp_min = min(tmp_min1, tmp_min2);
        }
        else tmp_min = (mn_l1[i].index!=-1 && mn_r1[i].index !=-1 && mn_l1[i].index!=mn_r1[i].index) ? max(pos - mn_l1[i].pos, mn_r1[i].pos-pos) : INT_MAX/2;
        result_min = min(tmp_min, result_min);

        int tmp_max=0;
        if(mx_l1[i].index == mx_r1[i].index)
        {
            int tmp_max1 = (mx_l1[i].index!=-1 && mx_r2[i].index!=-1 && mx_l1[i].index!=mx_r2[i].index) ? min(pos - mx_l1[i].pos, mx_r2[i].pos-pos) : -INT_MAX/2;
            int tmp_max2 = (mx_l2[i].index!=-1 && mx_r1[i].index!=-1 && mx_l2[i].index!=mx_r1[i].index) ? min(pos - mx_l2[i].pos, mx_r1[i].pos-pos) : -INT_MAX/2;
            tmp_max = max(tmp_max1, tmp_max2);
        }
        else tmp_max = (mx_l1[i].index!=-1 && mx_r1[i].index !=-1 && mx_l1[i].index!=mx_r1[i].index) ? min(pos - mx_l1[i].pos, mx_r1[i].pos-pos) : -INT_MAX/2;
        result_max = max(tmp_max, result_max);
    }
    if(result_min==INT_MAX/2)result_min=0;
    if(result_max==-INT_MAX/2)result_max=0;
    printf("%d %d", result_min, result_max);
}