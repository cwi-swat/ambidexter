%token CC40
%token CC41
%token CC46

%%

START : S 
;

S : L S
|
R CC46
|
CC46 L
|
CC40 S CC41 
;

L : CC46 L
|
CC40 S CC41 
;

R : 
|
R CC46 
;
