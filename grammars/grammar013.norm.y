%token CC97
%token CC98

%%

START : S 
;

S : CC97
|
A A 
;

A : CC98
|
S S 
;
