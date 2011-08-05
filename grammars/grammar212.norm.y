%token Td
%token Te

%%

START : S
;

S : A
| C
;

A : B
| C
| E
;

B : S Td
;

C : D
;

D : Td
;

E : Te
;

