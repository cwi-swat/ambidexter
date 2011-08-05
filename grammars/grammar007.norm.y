%token CC97
%token CC105
%token CC99
%token CC116
%token CC101

%%

START : S 
;

S : U
|
M 
;

M : CC97
|
CC105 CC99 CC116 M CC101 M 
;

U : CC105 CC99 CC116 M CC101 U
|
CC105 CC99 CC116 S 
;
