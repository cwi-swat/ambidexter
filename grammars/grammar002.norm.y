%token CC43
%token CC42
%token CC40
%token CC41
%token CC97

%%

START : E 
;

E : CC97
|
CC40 E CC41
|
E CC42 E
|
E CC43 E 
;
