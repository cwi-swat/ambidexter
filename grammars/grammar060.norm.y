%token CC105
%token CC110
%token CC104
%token CC111
%token CC109
%token CC97
%token CC116
%token CC115
%token CC101
%token CC32

%%

START : S 
;

S : S CC32 P
|
N1 CC32 V 
;

N1 : N1 CC32 P
|
CC97 CC32 N2
|
N2 
;

N2 : CC104 CC111 CC109 CC101
|
CC109 CC97 CC110
|
CC105 
;

P : CC97 CC116 CC32 N1 
;

V : CC115 CC101 CC101 CC32 N1 
;