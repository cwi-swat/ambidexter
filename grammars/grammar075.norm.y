%token CC120
%token CC121

%%

START : S 
;

S : A A 
;

A : CC121
|
CC120 A CC120 
;
