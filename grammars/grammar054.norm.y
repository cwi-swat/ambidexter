%token CC97
%token CC98

%%

START : A 
;

A : B C
|
C
|
B 
;

B : CC97 B
|
CC97 
;

C : CC98 C
|
CC98 
;
