%token CC98
%token CC99
%token CC97

%%

START : S 
;

S : T Q
;

T : CC97
|
CC97 T
;

Q : CC97 Q
|
CC98 CC99 Q
|
CC97
;
