%token CC97
%token CC104
%token CC101
%token CC102
%token CC112
%token CC114
%token CC118
%token CC98
%token CC103

%%

START : S 
;

S : N V N D J 
;

N : P N
|
P J N
|
CC104
|
CC97 
;

V : CC101
|
CC102 
;

P : CC112
|
CC102 
;

D : CC118
|
CC114 
;

J : P J
|
CC103
|
CC98 
;
