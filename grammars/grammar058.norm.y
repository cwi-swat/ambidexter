
// SLR1 not LR0

%token CC97
%token CC98

%%

START : S 
;

S : A B 
;

A : 
|
CC97 A 
;

B : 
|
CC98 B 
;
