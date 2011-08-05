%token CC61
%token CC105
%token CC102
%token CC116
%token CC104
%token CC101
%token CC110

%%

START : S 
;

S : A
|
I 
;

I : CC105 CC102 E CC116 CC104 CC101 CC110 S 
;

A : D CC61 E 
;

E : D CC61 D 
;

D : CC116 CC104 CC101 CC110
|
CC105 CC102 
;
