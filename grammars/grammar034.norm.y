%token CC101
%token CC104
%token CC116

%%

START : S 
;

S : H
|
CC101 H S
|
CC101 H
|
CC104 CC101
|
CC101
|
CC116 
;

H : CC104 CC116
|
CC104
|
CC104 H 
;
