%token CC97
%token CC98

%%

START : S 
;

S : C A
|
A B 
;

A : CC97 
;

B : CC98
|
A B
|
B C 
;

C : CC98
|
CC97 B 
;
