%token CC97
%token CC99
%token CC98

%%

START : S 
;

S : CC97 C CC98
|
CC97 C 
;

C : CC99 CC98
|
CC99 C CC98 
;
