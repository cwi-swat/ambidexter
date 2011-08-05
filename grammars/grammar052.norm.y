%token CC97

%%

START : S 
;

S : B
|
A 
;

A : CC97 
;

B : CC97 
;
