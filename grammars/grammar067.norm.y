%token CC40
%token CC41
%token CC46

%%

START : S 
;

S : 
|
S S
|
S CC46
|
CC46 S
|
CC40 S CC41 
;
