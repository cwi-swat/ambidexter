%token CC112
%token CC114
%token CC113

%%

START : P 
;

P : CC112
|
P R Q 
;

R : CC114 R
|
CC114
|
CC112 CC114 
;

Q : CC114 CC113
|
CC113 Q
|
CC113 
;
