%token CC105
%token CC99
%token CC116
%token CC101
%token CC97

%%

START : S 
;

S : CC97
|
CC105 CC99 CC116 S CC101 S
|
CC105 CC99 CC116 S 
;
