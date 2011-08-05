%token CC97
%token CC98

%%

START : S 
;

S : 
|
CC98
|
CC97
|
CC98 S CC98
|
CC97 S CC97 
;
