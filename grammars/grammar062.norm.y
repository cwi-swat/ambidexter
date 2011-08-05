%token CC97
%token CC32
%token CC98

%%

START : S 
;

S : CC97 L A L CC97 
;

L : 
|
CC32 L 
;

A : 
|
A CC98 
;
