%token CC61
%token CC42
%token CC97

%%

START : S 
;

S : R
|
L CC61 R 
;

L : CC97
|
CC42 R 
;

R : L 
;
