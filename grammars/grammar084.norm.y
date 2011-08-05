%token CC120
%token CC97
%token CC121

%%

START : S 
;

S : A B 
;

A : CC120
|
CC120 CC97 
;

B : CC121
|
CC97 CC121 
;
