%token CC98
%token CC99
%token CC97
%token CC100

%%

START : S 
;

S : P Q
|
M N 
;

M : CC98
|
M CC99
|
CC97 M 
;

N : CC99
|
CC98 N
|
N CC99 
;

P : CC100
|
CC99 P
|
P CC100 
;

Q : CC97 CC100 
;
