%token CC97
%token CC98

%%

START : A 
;

A : CC97
|
B A
|
A CC98 
;

B : CC98
|
B A 
;
