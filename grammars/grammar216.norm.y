%token Ta
%token Tb

%%

S : A B
;

A : C D
| C 
;

B : D E
| E
;

C : Ta
;

D : '(' F ',' G ')'
;

E : Tb
;

F : Ta
| H
;

G : Tb
| I
;

H : Ta
;

I : Tb
;
