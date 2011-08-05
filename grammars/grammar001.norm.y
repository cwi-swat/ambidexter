%token CC98
%token CC97

%%

START : S 
;

S : CC97
|
A B 
;

A : CC98
|
S B 
;

B : CC97
|
B A 
;
