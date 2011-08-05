%token Ta
%token Tb

%%

START : S
;

S : A
|
B
;

A : Ta Ta Tb
|
C
;

B : Tb Ta Tb
;

C : Ta Ta Tb
;

