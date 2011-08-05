%token CC97
%token CC98

%%

START : S 
;

S : B
|
A 
;

A : 
|
CC97 A 
;

B : 
|
CC98 B 
;
