%token Ta
%token Tb

%%

START : S
;

S : A
;

A : Ta Tb
|
B
;

B : Ta Tb
;
