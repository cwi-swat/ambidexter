%token CC98
%token CC97

%%

START : S 
;

S : CC98 A
|
CC97 B 
;

A : CC98 CC98 A A CC97
|
CC98 CC97 A
|
CC97 S
|
CC97 
;

B : CC97 B B
|
CC98 S
|
CC98 
;
