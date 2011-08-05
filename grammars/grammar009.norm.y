%token CC97
%token CC98

%%

START : S 
;

S : A S B
|
A B 
;

A : CC97 
;

B : CC98 
;
