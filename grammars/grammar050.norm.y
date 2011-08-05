%token CC97
%token CC98
%token CC99

%%

START : S 
;

S : B A
|
A C 
;

A : CC97
|
CC97 B
|
B CC97 
;

C : CC99
|
C B 
;

B : CC98 CC99
|
CC98
|
B CC99 
;
