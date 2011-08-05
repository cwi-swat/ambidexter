%token CC115
%token CC116
%token CC102
%token CC99
%token CC100
%token CC104
%token CC105
%token CC97
%token CC98

%%

START : S 
;

S : N2 V2
|
N1 V1 
;

N1 : D N3 S1 
;

N2 : D N4 
;

V1 : V3 N2 
;

V2 : CC115
|
V4 S1 
;

S1 : C S 
;

D : CC116 
;

N3 : CC102 
;

N4 : CC100
|
CC99 
;

C : CC104 
;

V4 : CC105 
;

V3 : CC98
|
CC97 
;
