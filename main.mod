/* Total de váriaveis x */
param m, integer, > 0;

/* Define o valor de i */
set I := 1..m;

/* Custos das váriaveis - função z */
param c{i in I}, >= 0;

/* Váriavel x */
var x{i in I}, >= 0;

/* Custos das váriaveis - função de restrição 1 */
param a{i in I}, >= 0;

/* Custos das váriaveis - função de restrição 2 */
param b{i in I}, >= 0;

/* Define as restrições */
s.t. f1: sum{i in I} a[i] * x[i] <= 18;
s.t. f2: sum{i in I} b[i] * x[i] <= 12;

/* Função objetivo */
maximize obj: sum{i in I} c[i] * x[i];

solve;

printf "\n";
printf{i in I} "x_%d = %d\n", i, x[i];
printf "Z = %10g\n", sum{i in I} c[i] * x[i];

data;

param m := 2;

param c :=
      1 4
      2 1 ;

param a :=
      1 9
      2 1 ;

param b :=
      1 3
      2 1 ;

