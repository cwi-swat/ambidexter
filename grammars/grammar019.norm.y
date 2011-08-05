%token CC97
%token CC98
%token CC100
%token CC99

%%

START : S 
;

S : A B C
|
A B D 
;

A : CC97
|
A E 
;

B : CC98
|
S E 
;

D : CC100 
;

C : CC99 
;

E : CC100 CC99 
;
