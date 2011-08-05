
// pico statements excerpt

%%

Program : 'b' Stats 'e' ;

Stats : Statsn | ;

Statsn : Stat ';' Statsn | Stat ;

// 'i' Id 't' Stats 'e' Stats 'f' 

Stat : 'w' Exp 'd' Stats 'o'
     | Id '=' Id
;

Id : Id NChar | Char ;

Char : 'w' | 'd' | 'o' ;

NChar : Char | '1' ;

Exp : '1' | Id ;


