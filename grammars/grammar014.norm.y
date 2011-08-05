%token CC98
%token CC97

%%

START : S 
;

S : B C
|
A B 
;

A : CC97
|
B A 
;

B : CC98
|
C C 
;

C : CC97
|
A B 
;
