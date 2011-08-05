%token CC46
%token CC40
%token CC41

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
CC40 P CC41 
;

P : S
|
CC40 P CC41 
;
