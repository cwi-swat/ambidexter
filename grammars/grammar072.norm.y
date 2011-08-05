%token CC46
%token CC40
%token CC41

%%

START : S 
;

S : L
|
L S 
;

L : CC46
|
CC40 F CC41 
;

F : L S
|
CC40 F CC41 
;
