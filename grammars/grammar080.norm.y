%token CC97
%token CC99
%token CC98

%%

START : S 
;

S : B C CC98
|
A C 
;

A : CC97 
;

B : CC97 
;

C : CC99 CC98
|
CC99 C CC98 
;
