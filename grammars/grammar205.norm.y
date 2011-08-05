%token Ta
%token Tb
%token Tc

%%

START : S
;

S : Tc A Tc
;

A : Ta Tb
|
B
;

B : Ta Tb
;
