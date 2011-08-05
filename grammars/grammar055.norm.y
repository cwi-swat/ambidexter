%token CC97
%token CC98
%token CC99
%token CC100

%%

START : S 
;

S : CC100
|
CC98 S CC99
|
CC97 S CC98
|
CC97 S CC98 S CC99 
;
