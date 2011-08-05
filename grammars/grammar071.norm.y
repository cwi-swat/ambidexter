%token CC46
%token CC40
%token CC41

%%

START : S 
;

S : 
|
CC40 S CC41 S
|
CC46 S 
;
