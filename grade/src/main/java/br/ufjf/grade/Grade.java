package br.ufjf.grade;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

public class Grade {
    
    public static List<String> professores = new ArrayList<>();
    public static String[] turmas = {"6A","6B","6C", "6D","7A","7B","7C", "7D","8A","8B","8C", "8D","9A","9B","9C", "9D","9E"};
    public static String[] dias = {"SEG", "TER", "QUA", "QUI", "SEX"};
    public static String[] horas = {"13:00", "13:50", "14:40", "15:45", "16:35"};

    public static ArrayList<ArrayList<Integer>> cargaHoraria = new ArrayList<>();
    public static ArrayList<ArrayList<ArrayList<Integer>>> disponibilidade = new ArrayList<>();

    public static void leituraCSVs(String pathProfs, String pathCH, String pathDisp) throws IOException{
        
        //Le o nome dos professores para posteriormente imprimir na grade
        Reader reader = Files.newBufferedReader(Paths.get(pathProfs));
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        List<String[]> listaProfessores = csvReader.readAll();
        for (int i=0; i < listaProfessores.size(); i++){
            professores.add(listaProfessores.get(i)[0]);     
        }
        
        //Le o arquivo com as cargas horárias de cada professor para cada turma
        reader = Files.newBufferedReader(Paths.get(pathCH));
        csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        List<String[]> listaCH = csvReader.readAll();
        for(int i=0; i < listaCH.size(); i++){
            List<String> list = Arrays.asList(listaCH.get(i));
            cargaHoraria.add(i, new ArrayList<>(list.stream().map(s -> Integer.valueOf(s)).collect(Collectors.toList())));
        }
        
        //Le o arquivo com a disponibilidade do professor para cada dia
        reader = Files.newBufferedReader(Paths.get(pathDisp));
        csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        List<String[]> listaDisponibilidades = csvReader.readAll();
        for(int i=0; i < listaDisponibilidades.size(); i++){
            ArrayList<ArrayList<Integer>> diasDisponiveis = new ArrayList<>();
            for(int j = 0; j < 5; j++){
                ArrayList<Integer> horasDisponiveis = new ArrayList<>();
                //O professor se está disponivel no dia, está disponivel em todos os 5 horarios
                horasDisponiveis.add(Integer.valueOf(listaDisponibilidades.get(i)[j]));
                horasDisponiveis.add(Integer.valueOf(listaDisponibilidades.get(i)[j]));
                horasDisponiveis.add(Integer.valueOf(listaDisponibilidades.get(i)[j]));
                horasDisponiveis.add(Integer.valueOf(listaDisponibilidades.get(i)[j]));
                horasDisponiveis.add(Integer.valueOf(listaDisponibilidades.get(i)[j]));
                diasDisponiveis.add(horasDisponiveis);
            } 
            disponibilidade.add(diasDisponiveis);
        }

    }
    
    public static int glp_find_col(int variaveis, glp_prob lp, String name){
        for(int i=1; i <= variaveis; i++){
            if(GLPK.glp_get_col_name(lp,i).equals(name)){
                return i;
            }
        }
        return 0;
    }
    
    public static void main(String[] args) throws IOException {
        
        String path = "src\\main\\java\\br\\ufjf\\dados\\";
        leituraCSVs(path + "Professores.csv", path + "CHporTurma.csv", path + "disponibilidade.csv");
               
        glp_prob lp;
        glp_smcp parm;
        SWIGTYPE_p_int ind;
        SWIGTYPE_p_double val;
        int ret;

        try {
            // Criação do problema
            lp = GLPK.glp_create_prob();
            GLPK.glp_set_prob_name(lp, "grade");

            // Definição das variaveis de decisão
            int variaveis = professores.size()*turmas.length*dias.length*horas.length + professores.size()*dias.length;
            GLPK.glp_add_cols(lp, variaveis);
            
            int col = 1;
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int t=1; t <= turmas.length; t++){
                        for(int h=1; h <= horas.length; h++){
                            GLPK.glp_set_col_name(lp, col, "x"+p+","+d+","+t+","+h);
                            GLPK.glp_set_col_kind(lp, col, GLPKConstants.GLP_BV);
                            col++;
                        }
                    }
                }
            }
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    GLPK.glp_set_col_name(lp, col, "y"+p+","+d);
                    GLPK.glp_set_col_kind(lp, col, GLPKConstants.GLP_BV);
                    col++;
                }
            }

            // Alocação de memória
            ind = GLPK.new_intArray(variaveis);
            val = GLPK.new_doubleArray(variaveis);
            
            // Criação das restrições
            GLPK.glp_add_rows(lp, 7064);

            
            int restricao = 1;
            
            
            //para todo p, d M * ypd - (somatorio t somatorio h xpdth) >= 0
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    GLPK.glp_set_row_name(lp, restricao, "a"+restricao);
                    GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 0, 4);
                    
                    int i = 0; //qtd de colunas que vao ser alteradas
                    
                    i++;
                    GLPK.intArray_setitem(ind, i, glp_find_col(variaveis,lp, "y"+p+","+d));
                    GLPK.doubleArray_setitem(val, i, 5);
                    
                    for(int t=1; t <= turmas.length; t++){
                        for(int h=1; h <= horas.length; h++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, -1);
                        }
                    }
                    GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                    restricao++;
                }
            }
            
            //para todo d, h DispPdh >= ypd
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int h=1; h <= horas.length; h++){
                        GLPK.glp_set_row_name(lp, restricao, "b"+restricao);
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, - disponibilidade.get(p-1).get(d-1).get(h-1), 0);

                        GLPK.intArray_setitem(ind, 1, glp_find_col(variaveis,lp, "y"+p+","+d));
                        GLPK.doubleArray_setitem(val, 1, -1);
                        
                        GLPK.glp_set_mat_row(lp, restricao, 1, ind, val);
                        restricao++;
                    }
                }
            }
            
            
            //para todo t, d , h       somatorio p xpdth = 1
            for(int d=1; d <= dias.length; d++){
                for(int t=1; t <= turmas.length; t++){
                    for(int h=1; h <= horas.length; h++){
                        GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 1, 1);
                        
                        for(int p=1; p <= professores.size(); p++){
                            GLPK.intArray_setitem(ind, p, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, p, 1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, professores.size(), ind, val);
                        restricao++;
                    }
                }
            }
            
            //para todo p, t, d somatorio h xptdh <= 2
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int t=1; t <= turmas.length; t++){
                        GLPK.glp_set_row_name(lp, restricao, "d"+restricao);
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, -2, 0);

                        for(int h=1; h <= horas.length; h++){
                            GLPK.intArray_setitem(ind, h, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, h, -1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, horas.length, ind, val);
                        restricao++;
                    }
                }
            }
            
            //para todo p, d, h somatorio t xpdth <= dispPdh
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int h=1; h <= horas.length; h++){
                        GLPK.glp_set_row_name(lp, restricao, "e"+restricao);
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, - disponibilidade.get(p-1).get(d-1).get(h-1), 0);

                        for(int t=1; t <= turmas.length; t++){
                            GLPK.intArray_setitem(ind, t, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, t, -1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, turmas.length, ind, val);
                        restricao++;
                    }
                }
            }
            
            //para todo p, t somatorio d somatorio h xpdth = CHpt
            for(int p=1; p <= professores.size(); p++){
                for(int t=1; t <= turmas.length; t++){
                    GLPK.glp_set_row_name(lp, restricao, "f"+restricao);
                    GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, cargaHoraria.get(p-1).get(t-1), cargaHoraria.get(p-1).get(t-1));

                    int i = 0; //qtd de colunas que vao ser alteradas
                    for(int d=1; d <= dias.length; d++){
                        for(int h=1; h <= horas.length; h++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, 1);
                        }
                    }
                    GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                    restricao++;
                }
            }
            
            //para todo p, d, h somatorio t xpdth = 1
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int h=1; h <= horas.length; h++){
                        GLPK.glp_set_row_name(lp, restricao, "g"+restricao);
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV,1,1);

                        for(int t=1; t <= turmas.length; t++){
                            GLPK.intArray_setitem(ind, t, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, t, 1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, turmas.length, ind, val);
                        restricao++;
                    }
                }
            }
            
            //para todo d, h somatorio p t xpdth <= 1
            // p de educação fisica
            for(int d=1; d <= dias.length; d++){
                for(int h=1; h <= horas.length; h++){
                    GLPK.glp_set_row_name(lp, restricao, "h"+restricao);
                    GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, -1, 0);

                    int i = 0; //qtd de colunas que vao ser alteradas
                    for(int p=1; p <= 3; p++){
                        for(int t=1; t <= turmas.length; t++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, -1);
                        }
                    }
                    GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                    restricao++;
                }
            }
            
            //para todo p somatorio h xpdth <= 16
            for(int p=1; p <= professores.size(); p++){
                GLPK.glp_set_row_name(lp, restricao, "i"+restricao);
                GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, -16, 0);

                int i = 0; //qtd de colunas que vao ser alteradas
                for(int d=1; d <= dias.length; d++){
                    for(int t=1; t <= turmas.length; t++){
                        for(int h=1; h <= horas.length; h++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(variaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, -1);
                        }
                    }
                }
                GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                restricao++;
            }
          
            // Liberação da memória
            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);

            // Função objetivo
            //min Z = somatorio p, d ypd
            GLPK.glp_set_obj_name(lp, "z");
            GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    GLPK.glp_set_obj_coef(lp, glp_find_col(variaveis,lp, "y"+p+","+d) , 1);
                }
            }

            // Escreve modelo em arquivo
            GLPK.glp_write_lp(lp, null, path + "lp.lp");

            // Resolve modelo
            parm = new glp_smcp();
            GLPK.glp_init_smcp(parm);
            ret = GLPK.glp_simplex(lp, parm);

            // Mostra solução
            if (ret == 0) {
                escreveSolucao(lp, path);
            } else {
                System.out.println("O problema não pôde ser resolvido");
            }

            // Liberação da memória
            GLPK.glp_delete_prob(lp);
        } catch (GlpkException ex) {
            System.out.println(ex);
        }
    }

    public static void escreveSolucao(glp_prob lp, String path) {
        
        BufferedWriter br;
        try {
            br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path+"saida.txt"), StandardCharsets.UTF_8));
            
            int i;
            int n;
            String name;
            double val;

            n = GLPK.glp_get_num_cols(lp);
            for (i = 1; i <= n; i++) {
                name = GLPK.glp_get_col_name(lp, i);
                val = GLPK.glp_get_col_prim(lp, i);
                if(val == 1 && name.startsWith("x")){
                    String array[] = name.split(",");
                    br.write(professores.get(Integer.parseInt(array[0].replace("x", ""))-1));
                    br.write(" - ");
                    br.write(dias[Integer.parseInt(array[1])-1]);
                    br.write(" - ");
                    br.write(turmas[Integer.parseInt(array[2])-1]);
                    br.write(" - ");
                    br.write(horas[Integer.parseInt(array[3])-1]);
                    br.newLine();
                }
            }
            
            br.close();
            
        } catch (IOException ex) {
            Logger.getLogger(Grade.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
