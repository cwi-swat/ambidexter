%token CC97
%token CC98

%%

S : CC97
|
A S B
;

A : CC97
|
CC97 A 
;

B : A B
|
CC97 CC98
;
