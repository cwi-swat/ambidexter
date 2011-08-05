%token CC40
%token CC41
%token CC46

%%

START : P 
;

P : CC40 O CC41
|
CC40 P CC41 
;

O : H
|
S P S
|
P R
|
L P 
;

L : CC46
|
CC46 L 
;

R : CC46
|
CC46 R 
;

S : CC46
|
CC46 S 
;

H : CC46 CC46 CC46
|
CC46 H 
;
