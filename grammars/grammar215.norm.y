%token Ta
%token Tb

%%

S : A B
;

A : Ta
| C 
;

B : Tb
| D
;

C : Ta
;

D : Tb
;

