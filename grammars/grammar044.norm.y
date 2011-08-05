%token CC97
%token CC98

%%

START : S 
;

S : CC98
|
A S B
|
S A B 
;

A : B CC97
|
CC97 CC98 
;

B : CC98 B
|
CC98 
;
