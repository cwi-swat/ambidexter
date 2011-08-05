%token CC43
%token CC42
%token CC97

%%

START : E 
;

E : CC97
|
E CC43 E
|
E CC42 E
;
