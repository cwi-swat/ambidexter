%token Ta
%token Tb

%%

START : S
;

S : A
;

A : Ta Tb
|
C B
;

B : Ta Tb
;

C :
;
