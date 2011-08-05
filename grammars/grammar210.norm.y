%token Ta
%token Tb

%%

START : S
;

S : A
;

A : Ta Tb
|
B C
;

B : Ta Tb
;

C : D
;

D : 
;
