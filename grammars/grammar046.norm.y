%token CC100
%token CC111
%token CC115
%token CC105
%token CC110
%token CC103
%token CC101
%token CC97
%token CC116

%%

START : S 
;

S : N V N V CC105 CC110 CC103 N
|
N V N 
;

N : N V CC105 CC110 CC103 N
|
CC100 CC111 CC103 CC115 
;

V : CC101 CC97 CC116 
;
