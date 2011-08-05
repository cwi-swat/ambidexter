%token CC97
%token CC98

%%

START : A 
;

A : A CC98
|
CC97 A
|
CC98
|
CC97 
;
