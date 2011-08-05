%token CC97
%token CC98
%token CC99

%%

START : S 
;

S : CC99
|
CC98 S CC98
|
CC97 S CC97 
;
