%token CC97
%token CC98

%%

START : S 
;

S : E F
|
C D
|
A B 
;

A : CC97 
;

B : CC98 
;

C : CC97 
;

D : CC98 
;

E : CC97 
;

F : CC98 
;
