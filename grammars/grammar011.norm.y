%token CC97
%token CC98
%token CC99
%token CC100

%%

START : S 
;

S : B C
|
A B 
;

A : CC97
|
B A D 
;

B : CC98 D
|
C C 
;

C : CC99
|
A B 
;

D : CC100 
;
