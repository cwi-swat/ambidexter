%token Ta
%token Tb

%%

START : S
;

S : A
|
B
;

A : Ta Tb
;

B : Ta Tb
;
