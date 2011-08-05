%token CC98
%token CC97

%%

START : A 
;

A : CC97
|
CC98 A
|
A S 
;

S : B B
|
A B 
;

B : CC98 
;
