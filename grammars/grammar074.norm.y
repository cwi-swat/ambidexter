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

T : T CC40 P CC41
|
CC40 P CC41
|
T CC46 
;

P : CC40 N CC41
|
CC40 P CC41 
;

N : T CC40 P CC41
|
T CC46
|
CC46 S 
;
