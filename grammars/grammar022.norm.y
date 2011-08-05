%token CC98
%token CC97

%%

START : S 
;

S : A A 
;

A : CC97
|
A CC98
|
CC98 A
|
A A A 
;
