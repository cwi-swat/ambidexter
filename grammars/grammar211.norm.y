%token Ta
%token Tb
%token Tc

%%

START : S
;

S : Tc A Tc
;

A : Ta C Tb
|
B
;

B : Ta C Tb
;

C : Tc
;
