%token CC99
%token CC103
%token CC101
%token CC100
%token CC98
%token CC97

%%

START : S 
;

S : A B
|
B C 
;

A : C F 
;

C : CC99 
;

F : CC103 CC101 CC100 
;

B : CC97 C
|
CC97 CC98 
;
