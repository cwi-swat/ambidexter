%token CC97
%token CC98

%%

START : S 
;

S : A B 
;

B : CC98 B
|
CC98 CC98 
;

A : CC97 A CC98
|
CC97 
;
