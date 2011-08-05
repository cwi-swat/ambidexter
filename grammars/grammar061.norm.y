%token CC98
%token CC99

%%

START : S 
;

S : A 
;

A : B C
|
B 
;

B : CC98 
;

C : 
|
CC99 
;
