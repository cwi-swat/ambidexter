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
|
D
;

C : Ta Ta Tb
;

D : Tb Ta Tb
;
