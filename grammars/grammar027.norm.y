%token CC97
%token CC100
%token CC98
%token CC99

%%

START : A 
;

A : C A
|
B
|
CC97 
;

B : CC98
|
CC98 D 
;

D : CC97 CC100
|
CC100 D
|
CC100 
;

C : C C
|
CC99
|
CC98 CC99 
;
