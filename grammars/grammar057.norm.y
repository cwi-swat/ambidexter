%token CC97

%%

START : S 
;

S : CC97
|
CC97 S CC97 
;
