%token T_end_br T_end_img T_row T_end_row T_html T_end_html
%token T_end_table T_end_cell T_end_font T_string
%token T_BR T_br
%token T_IMG T_img
%token T_table
%token T_cell
%token T_font

%%

html  : T_html fonttext T_end_html 
      | T_html fonttable T_end_html 
      ;

fonttext : text 
      ;

text : text textitem  
     | textitem 
     ;

textitem : string 
         | br 
         | sfont text nfont
         ;

sfont : T_font 
      ;

nfont : T_end_font 
      ;

br     : T_br T_end_br 
       | T_BR 
       ;

string : T_string
       ;

table : opt_space T_table 
        rows T_end_table opt_space 
      ;

fonttable : table 
          | sfont table nfont 
          ;

opt_space : string 
          | 
          ;

rows : row
     | rows row
     ;

row : T_row  cells T_end_row
      ;

cells : cell
      | cells cell
      ;

cell : T_cell fonttable  T_end_cell
     | T_cell fonttext  T_end_cell
     | T_cell image  T_end_cell
     | T_cell  T_end_cell
     ;

image  : T_img T_end_img 
       | T_IMG 
       ;
