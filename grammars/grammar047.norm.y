%token CC97
%token CC98

%%

START : A 
;

A : A S
|
S B 
;

S : CC97 CC98 
;

B : S B
|
CC98 B
|
CC98 
;
