
// this one gives a very weird parse tree

%%

S : A B C B E ;

A : 'a' ;

B : | 'b' ;

C : | 'c' ;

D : | 'b' ;

E : 'e' ;

