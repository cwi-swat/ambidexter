%token CC119
%token CC99
%token CC98
%token CC115

%%

START : S 
;

S : CC119 CC98 CC99 CC98 CC98 R CC98 
;

R : CC115 R
|
CC115 
;
