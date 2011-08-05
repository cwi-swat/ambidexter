%token CC97
%token CC98

%%

START : S 
;

S : CC97 B
|
CC98 A 
;

A : CC97
|
CC97 S
|
CC98 A A 
;

B : S B
|
CC98
|
CC98 B B 
;
