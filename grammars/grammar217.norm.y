%token Ta
%token Tb
%token Tc
%token Td
%token Te

%%

START : S
;

S : Tb A Tc
| D
;

D : Td A Te
;

A : Ta Ta
| B
;

B : Ta C
;

C : Ta
;
