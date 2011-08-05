%token Ta
%token Tb

%%

START : S
;

S : A
;

A : Ta
|
B Ta
;

B : Tb
|
A Tb
;

