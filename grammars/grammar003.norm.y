%token CC42
%token CC40
%token CC41
%token CC97

%%

START : E 
;

E : T
|
E CC42 T 
;

T : F
|
T CC42 F 
;

F : CC97
|
CC40 E CC41 
;
