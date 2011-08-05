%token CC98
%token CC97

%%

START : S 
;

S : V
|
U 
;

U : T CC97 T
|
T CC97 U 
;

V : T CC98 T
|
T CC98 V 
;

T : 
|
CC98 T CC97 T
|
CC97 T CC98 T 
;
