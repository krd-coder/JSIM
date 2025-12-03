#include<stdlib.h>
#include <stdio.h>
#include <stdlib.h> // Zawiera qsort
#include "zbior_ary.h"
#include<limits.h>

int diff = -1; // global variable storing the value q, -1 means it has not been set yet

// Function that returns the value modulo diff, also handling negative numbers
long long mod(long long x)
{
    return ((x % diff) + diff) % diff;
}

// Constructor for a set from an arithmetic progression
zbior_ary ciag_arytmetyczny(int a, int q, int b)
{
    zbior_ary result;
    result.mn = (long long*)malloc(sizeof(long long));
    result.mx = (long long*)malloc(sizeof(long long));
    result.size = 1;
    diff = q;
    result.mn[0] = a;
    result.mx[0] = b;
    return result;
}

// Constructor for a set containing a single element (singleton)
zbior_ary singleton(int a)
{
    zbior_ary result;
    result.mn = (long long*)malloc(sizeof(long long));
    result.mx = (long long*)malloc(sizeof(long long));
    result.size = 1;
    result.mn[0] = a;
    result.mx[0] = a;
    return result;
}

// Implementation of the sum (union) function
zbior_ary suma(zbior_ary A, zbior_ary B)
{
    zbior_ary result;
    result.mn = (long long*)malloc(sizeof(long long) * (size_t)(A.size + B.size));
    result.mx = (long long*)malloc(sizeof(long long) * (size_t)(A.size + B.size));
    result.size = 0;

    unsigned int A_index = 0;
    unsigned int B_index = 0;

    // we consider elements (progressions) in sets A and B in increasing order of their left endpoints (A.mn and B.mn)
    // While there are elements left to consider in both set A and set B
    while (A_index<A.size && B_index<B.size)
    {
        // While the currently considered elements in sets A and B have different values modulo q
        while(A_index<A.size && B_index<B.size && mod(A.mn[A_index]) != mod(B.mn[B_index]))
        {
            // Since the modulo is different, the current elements do not overlap, so we add the one with the smaller modulo to the result
            if (mod(A.mn[A_index]) < mod(B.mn[B_index]))
            {
                result.mn[result.size] = A.mn[A_index];
                result.mx[result.size] = A.mx[A_index];
                A_index++;
                result.size++;
            }
            else
            {
                result.mn[result.size] = B.mn[B_index];
                result.mx[result.size] = B.mx[B_index];
                B_index++;
                result.size++;
            }
        }

        if(!(A_index<A.size && B_index<B.size))break;//Sprawdzenie, czy nie skończyły się elementy
        long long current_diff = mod(A.mn[A_index]);//obecnie rozważane modulo
        long long mn = (A.mn[A_index] < B.mn[B_index]) ? A.mn[A_index] : B.mn[B_index];//początek obecnie rozważanego ciągu w wyniku (skoro jest suma, to kilka ciągów może się nakładać tworząc jeden, więc musimy zapisać jego początek - bierzemy mniejszy lewy kraniec)

        // Currently considered elements in sets A and B have the same modulo q value
        while (A_index<A.size && B_index<B.size && mod(A.mn[A_index]) == mod(B.mn[B_index]))
        {
            // when the currently considered intervals are disjoint
            if (A.mx[A_index]+diff < B.mn[B_index])
            {
                // intervals are disjoint, and A has the smaller left endpoint, so we add the current element A to the result
                result.mn[result.size] = mn;
                result.mx[result.size] = A.mx[A_index];
                A_index++;
                result.size++;
                // set the start of the new interval to the left end of the next element in A or B, depending on which is smaller
                mn = B.mn[B_index];
                if(A_index<A.size && mod(A.mn[A_index])==current_diff && B.mn[B_index] > A.mn[A_index]) mn = A.mn[A_index];//sprawdzenie, czy kolejny element w A ma to samo modulo i czy on wogule istnieje
            }
            else if (A.mn[A_index] > B.mx[B_index]+diff)
            {
                // intervals are disjoint, and B has the smaller left endpoint, so we add the current element B to the result
                result.mn[result.size] = mn;
                result.mx[result.size] = B.mx[B_index];
                B_index++;
                result.size++;
                // set the start of the new interval to the left end of the next element in A or B, depending on which is smaller
                mn = A.mn[A_index];
                if(B_index<B.size && mod(B.mn[B_index])==current_diff && A.mn[A_index] > B.mn[B_index]) mn = B.mn[B_index];//sprawdzenie, czy kolejny element w B ma to samo modulo i czy on wogule istnieje
            }
            // when the currently considered intervals overlap:
            else
            {
                // we update the right end of the current interval in the result - we move to the next element in A or B, depending on which has the larger right end
                if(A.mx[A_index]>B.mx[B_index])B_index++;
                else A_index++;
            }
        }
        // close the current interval in the result
        if(A_index<A.size && mod(A.mn[A_index]) == current_diff)
        {
            result.mn[result.size] = mn;
            result.mx[result.size] = A.mx[A_index];
            A_index++;
            result.size++;
        }
        else if(B_index<B.size && mod(B.mn[B_index]) == current_diff)
        {
            result.mn[result.size] = mn;
            result.mx[result.size] = B.mx[B_index];
            B_index++;
            result.size++;
        }
    }
    // if there are any elements left in A, but not in B, we add them
    while (A_index<A.size)
    {
        result.mn[result.size] = A.mn[A_index];
        result.mx[result.size] = A.mx[A_index];
        A_index++;
        result.size++;
    }
    // if there are any elements left in B, but not in A, we add them
    while (B_index<B.size)
    {
        result.mn[result.size] = B.mn[B_index];
        result.mx[result.size] = B.mx[B_index];
        B_index++;
        result.size++;
    }    
    return result;
}


zbior_ary roznica(zbior_ary A, zbior_ary B)
{
    zbior_ary result;
    result.mn = (long long*)malloc(sizeof(long long) * (size_t)(A.size + B.size));
    result.mx = (long long*)malloc(sizeof(long long) * (size_t)(A.size + B.size));
    result.size = 0;

    unsigned int A_index = 0;
    unsigned int B_index = 0;

    // we consider elements (progressions) in sets A and B in increasing order of their left endpoints (A.mn and B.mn)
    // While there are elements left to consider in set A and set B
    while (A_index<A.size && B_index<B.size)
    {
        // While the currently considered elements in sets A and B have different values modulo q
        while(A_index<A.size && B_index<B.size && mod(A.mn[A_index]) != mod(B.mn[B_index]))
        {
            if (mod(A.mn[A_index]) < mod(B.mn[B_index]))
            {
                result.mn[result.size] = A.mn[A_index];
                result.mx[result.size] = A.mx[A_index];
                A_index++;
                result.size++;
            }
            else
            {
                B_index++;
            }
        }


        if(!(A_index<A.size && B_index<B.size))break;// Check if we haven't run out of elements
        long long current_diff = mod(A.mn[A_index]);

        long long mn = A.mn[A_index];// start of the currently considered progression in A
        // While the currently considered elements in sets A and B have the same modulo value.
        // We can treat the differences of elements with the same modulo as set differences of intervals on a line
        while (A_index<A.size && B_index<B.size && mod(A.mn[A_index]) == mod(B.mn[B_index]))
        {
            // when the currently considered intervals are disjoint
            if (A.mx[A_index] < B.mn[B_index])// when A.mx is smaller than B.mn, the intervals are disjoint, so we add the current element A to the result
            {
                result.mn[result.size] = mn;
                result.mx[result.size] = A.mx[A_index];
                A_index++;
                if(A_index<A.size)mn = A.mn[A_index];
                result.size++;
            }
            else if (A.mn[A_index] > B.mx[B_index])// when A.mn is larger than B.mx, the intervals are disjoint, so we skip the current element from B
            {
                B_index++;
            }
            // when the currently considered intervals overlap
            else if(mn>=B.mn[B_index])
            {
                if(A.mx[A_index]>B.mx[B_index])
                {
                    // element from B covers the beginning (left endpoint) of the element from A, so we "cut" it to B.mx+diff:
                    mn = B.mx[B_index]+diff; 
                    B_index++;
                }
                else
                {
                    A_index++; // element from B covers the element from A entirely
                    if(A_index<A.size)mn = A.mn[A_index];
                }
            }
            else
            {
                // element from B does not cover the beginning (left endpoint) of the element from A, so we add the interval [A.mn - B.mn-diff):
                result.mn[result.size] = mn; 
                result.mx[result.size] = B.mn[B_index]-diff; 
                result.size++;
                if(A.mx[A_index]>B.mx[B_index])
                {
                    mn = B.mx[B_index]+diff;// element from B does not cover the right end of the element from A, so it is contained within it - so we cut the element from A to B.mx
                    B_index++;
                }
                else
                {
                    A_index++; // element from B covers the right end, so we have processed the entire element from A
                    if(A_index<A.size)mn = A.mn[A_index];
                }
            }
            
        }
        // the loop above ends when elements with modulo current_diff run out in either A or B, so we must consider the remaining elements in the other set
        // we only consider the case where elements remain in A, because this is a difference operation, so elements from B can be skipped
        while(A_index<A.size && mod(A.mn[A_index]) == current_diff)
        {
            result.mn[result.size] = mn;
            result.mx[result.size] = A.mx[A_index];
            A_index++;
            if(A_index<A.size)mn = A.mn[A_index];
            result.size++;
        }
    }

    // if there are any elements left in A, but not in B, we add them
    while (A_index<A.size)
    {
        result.mn[result.size] = A.mn[A_index];
        result.mx[result.size] = A.mx[A_index];
        A_index++;
        result.size++;
    }
    // if there are any elements left in B, but not in A, skip them
    return result;
}

zbior_ary iloczyn(zbior_ary A, zbior_ary B)
{
    //A ∩ B = A \ (A \ B)
    return (roznica(A, roznica(A, B)));
}

bool nalezy(zbior_ary A, int b)
{
    if(A.size==0) return false;
    long long lo=0;
    long long hi=(long long)A.size-1;//binary searsh
    while (hi-lo>1)
    {
        long long mid = (lo+hi)/2;
        // first look for elements with equal modulo q
        if(mod(A.mn[mid]) < mod(b))
        {
            lo=mid;
            continue;
        }
        if(mod(A.mn[mid]) > mod(b))
        {
            hi=mid;
            continue;
        }
        // if A[mid] = b mod q, then we look for the largest left endpoint of a progression in A, which is less than/equal to b
        if(A.mn[mid]<b) 
        {
            lo=mid;
            continue;
        }
        else if(A.mn[mid]>b)
        {
            hi=mid;
            continue;
        }
        else return true;// if A.mn[mid]=b
    }
    // check if either of the progressions hi or lo contains b
    return ((mod(A.mn[lo]) == mod(b) && A.mn[lo]<=b && A.mx[lo]>=b)||(mod(A.mn[hi]) == mod(b) && A.mn[hi]<=b && A.mx[hi]>=b));//sprawdzamy, czy któryś z ciągów hi lub lo zawiera b
}

unsigned moc(zbior_ary A)
{
    unsigned result=0;
    for (unsigned int i = 0; i < A.size; i++)
    {
        // add the number of elements in the i-th arithmetic progression
        result+=(unsigned int)((A.mx[i]-A.mn[i])/diff+1);
    }
    return result;
}

unsigned ary(zbior_ary A)
{
    // number of arithmetic progressions in set A - this is simply the size of the mn or mx array, because we always keep the set in an optimal form regarding the number of arithmetic progressions
    return (unsigned int)A.size;
}

int cmpfunc (const void * a, const void * b)
{
    if(*(int*)a > *(int*)b) return 1;
    if(*(int*)a < *(int*)b) return -1;
    return 0;
}

void print(zbior_ary A)
{
    int* tmp = (int*)malloc(sizeof(int) * moc(A));
    unsigned int index = 0;
    for(unsigned int i=0;i<A.size;i++)
    {
        for(int val=(int)A.mn[i];val<=A.mx[i];val+=(int)diff)
        {
            tmp[index++] = (int)val;
        }
    }
    qsort(tmp, index, sizeof(int), cmpfunc);
    for(unsigned int i=0;i<index;i++)
    {
        printf("%d ", tmp[i]);
    }
    free(tmp);
    printf("\n");
}