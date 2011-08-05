%token Tc
%token Td

%%

START : S ;

S : C C ;

C : Tc C | Td ;
