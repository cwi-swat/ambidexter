%token CC97
%token CC98

%%

START : S 
;

S : CC97
|
A S B
|
A B S B 
;

A : CC97
|
CC97 A 
;

B : CC98 B
|
CC98
|
A B
|
CC97 CC98 
;
