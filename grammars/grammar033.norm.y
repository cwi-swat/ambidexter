%token CC97
%token CC98

%%

START : S 
;

S : CC97
|
T 
;

T : CC98
|
A 
;

A : CC97 CC97 CC98
|
CC97 CC98 
;
