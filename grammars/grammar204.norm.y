%token Ta
%token Tb
%token Tc

%%

START : S
;

S : A
;

A : Ta Tb
|
B
|
C
;

B : Ta Tb
;

C : Tc
;
