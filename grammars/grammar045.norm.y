%token CC98
%token CC97

%%

START : L 
;

L : G A
|
A N D 
;

A : CC97 CC98
|
CC97 A
|
CC97 
;

N : CC97 CC98 
;

D : D CC97
|
CC98 CC97 
;

G : CC98 CC97
|
CC98 CC97 G
|
CC98 G 
;
