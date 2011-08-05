%token CC97
%token CC100
%token CC99
%token CC101

%%

START : A 
;

A : B D E 
;

B : CC97
|
CC99
|
CC99 A 
;

D : CC97 B
|
CC100
|
CC99 D 
;

E : CC99 CC101
|
CC100 CC101
|
CC101 
;
