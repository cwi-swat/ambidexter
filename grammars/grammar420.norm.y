%token Ts
%token Ti
%token Te

%%

START : S ;

S : L Te R 
| R ;

L : Ts R
| Ti ;

R : L ;
