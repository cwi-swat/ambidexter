%token CC97
%token CC98
%token CC99
%token CC100

%%

START : S 
;

S : A B C D 
;

A : CC97
|
C S 
;

B : CC98
|
CC98 D 
;

D : CC100
|
S B 
;

C : CC99 
;
