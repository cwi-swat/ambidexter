%token CC46
%token CC40
%token CC41

%%

START : S 
;

S : 
|
T
|
CC46 S 
;

T : T CC40 S CC41
|
CC40 S CC41
|
T CC46 
;
