%token CC97
%token CC98
%token CC99

%%

START : S 
;

S : A C A 
;

A : C
|
B
|
CC97 A CC97 
;

B : CC98
|
CC98 B 
;

C : CC99
|
CC99 C 
;
