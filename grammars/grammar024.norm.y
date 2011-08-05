%token CC97
%token CC98
%token CC99
%token CC100

%%

START : T 
;

T : A B D
|
A B C 
;

A : CC97
|
A C
|
A D 
;

B : CC98
|
A B 
;

C : CC99 
;

D : CC100 
;
