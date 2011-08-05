%token CC97

%%

START : S 
;

S : B
|
A 
;

A : CC97
|
CC97 A 
;

B : CC97
|
B CC97 
;
