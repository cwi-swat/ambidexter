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
CC40 P CC41 
;

L : CC46 L
|
CC40 P CC41 
;

R : 
|
R CC46 
;

P : CC40 N CC41
|
CC40 P CC41 
;

N : L S
|
R CC46
|
CC46 L 
;
