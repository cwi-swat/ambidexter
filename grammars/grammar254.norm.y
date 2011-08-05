%token CC98
%token CC97

%%

START : S 
;

S : CC98
|
CC97
|
A S
|
S S 
;

A : CC97
;
