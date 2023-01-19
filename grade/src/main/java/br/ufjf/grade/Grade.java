package br.ufjf.grade;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static void leDados(String pathProfs, String pathCH, String pathDisp) throws IOException{
        
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
    
    public static int glp_find_col(int qtdVariaveis, glp_prob lp, String name){
        for(int i=1; i <= qtdVariaveis; i++){
            if(GLPK.glp_get_col_name(lp,i).equals(name)){
                return i;
            }
        }
        return 0;
    }
    
    public static void main(String[] args) throws IOException {
        
        String path = "C:\\Users\\maral\\OneDrive\\Documents\\NetBeansProjects\\gradeescolar\\src\\main\\java\\br\\ufjf\\dados\\";
        leDados(path + "Professores.csv", path + "CHporTurma.csv", path + "disponibilidade.csv");
                
        glp_prob lp;
        glp_smcp parm;
        SWIGTYPE_p_int ind;
        SWIGTYPE_p_double val;
        int ret;

        try {
            // Create problem
            lp = GLPK.glp_create_prob();
            GLPK.glp_set_prob_name(lp, "grade");

            // Define columns
            int qtdVariaveis = professores.size()*turmas.length*dias.length*horas.length + professores.size()*dias.length;
            GLPK.glp_add_cols(lp, qtdVariaveis);
            
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
         
            // Create constraints

            // Allocate memory
            ind = GLPK.new_intArray(qtdVariaveis);
            val = GLPK.new_doubleArray(qtdVariaveis);
            
            // Create rows
            GLPK.glp_add_rows(lp, 5264);

            // Set row details
            
            int restricao = 1;
            
            
            //para todo p, d M * ypd - (somatorio t somatorio h xpdth) >= 0
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                    GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 0, 100);
                    
                    int i = 0; //qtd de colunas que vao ser alteradas
                    
                    i++;
                    GLPK.intArray_setitem(ind, i, glp_find_col(qtdVariaveis,lp, "y"+p+","+d));
                    GLPK.doubleArray_setitem(val, i, 5000);
                    
                    for(int t=1; t <= turmas.length; t++){
                        for(int h=1; h <= horas.length; h++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, -1);
                        }
                    }
                    GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                    restricao++;
                }
            }
            
            
            //para todo t, d , h       somatorio p xpdth = 1
            for(int d=1; d <= dias.length; d++){
                for(int t=1; t <= turmas.length; t++){
                    for(int h=1; h <= horas.length; h++){
                        GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                        restricao++;
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 1, 1);
                        
                        for(int p=1; p <= professores.size(); p++){
                            GLPK.intArray_setitem(ind, p, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, p, 1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, professores.size(), ind, val);
                    }
                }
            }
            
            //para todo p, t, d somatorio h xptdh <= 2
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int t=1; t <= turmas.length; t++){
                        GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                        restricao++;
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 0, 2);

                        for(int h=1; h <= horas.length; h++){
                            GLPK.intArray_setitem(ind, h, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, h, 1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, horas.length, ind, val);
                    }
                }
            }
            
            //para todo p, d, h somatorio xpdth <= dispPdh
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    for(int h=1; h <= horas.length; h++){
                        GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                        restricao++;
                        GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 0, disponibilidade.get(p-1).get(d-1).get(h-1));

                        for(int t=1; t <= turmas.length; t++){
                            GLPK.intArray_setitem(ind, t, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, t, 1);
                        }
                        GLPK.glp_set_mat_row(lp, restricao, turmas.length, ind, val);
                    }
                }
            }
            
            //para todo p, t somatorio d somatorio h xpdth = CHpt
            for(int p=1; p <= professores.size(); p++){
                for(int t=1; t <= turmas.length; t++){
                    GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                    restricao++;
                    GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, cargaHoraria.get(p-1).get(t-1), cargaHoraria.get(p-1).get(t-1));

                    int i = 0; //qtd de colunas que vao ser alteradas
                    for(int d=1; d <= dias.length; d++){
                        for(int h=1; h <= horas.length; h++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, 1);
                        }
                    }
                    GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                }
            }
            
            //para todo d, h somatorio p t xpdth <= 1
            // p de educação fisica
            for(int d=1; d <= dias.length; d++){
                for(int h=1; h <= horas.length; h++){
                    GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                    restricao++;
                    GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 0, 1);

                    int i = 0; //qtd de colunas que vao ser alteradas
                    for(int p=1; p <= 3; p++){
                        for(int t=1; t <= turmas.length; t++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, 1);
                        }
                    }
                    GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
                }
            }
            
            //para todo p somatorio h xpdth <= 16
            for(int p=1; p <= professores.size(); p++){
                GLPK.glp_set_row_name(lp, restricao, "c"+restricao);
                restricao++;
                GLPK.glp_set_row_bnds(lp, restricao, GLPKConstants.GLP_IV, 0, 16);

                int i = 0; //qtd de colunas que vao ser alteradas
                for(int d=1; d <= dias.length; d++){
                    for(int t=1; t <= turmas.length; t++){
                        for(int h=1; h <= horas.length; h++){
                            i++;
                            GLPK.intArray_setitem(ind, i, glp_find_col(qtdVariaveis,lp, "x"+p+","+d+","+t+","+h));
                            GLPK.doubleArray_setitem(val, i, 1);
                        }
                    }
                }
                GLPK.glp_set_mat_row(lp, restricao, i, ind, val);
            }
          
            // Free memory
            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);

            // Define objective
            //min Z = somatorio p, d ypd
            GLPK.glp_set_obj_name(lp, "z");
            GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);
            for(int p=1; p <= professores.size(); p++){
                for(int d=1; d <= dias.length; d++){
                    GLPK.glp_set_obj_coef(lp, glp_find_col(qtdVariaveis,lp, "y"+p+","+d) , 1);
                }
            }

            // Write model to file
            GLPK.glp_write_lp(lp, null, "lp.lp");

            // Solve model
            parm = new glp_smcp();
            GLPK.glp_init_smcp(parm);
            ret = GLPK.glp_simplex(lp, parm);

            // Retrieve solution
            if (ret == 0) {
                write_lp_solution(lp);
            } else {
                System.out.println("The problem could not be solved");
            }

            // Free memory
            GLPK.glp_delete_prob(lp);
        } catch (GlpkException ex) {
            System.out.println(ex);
        }
    }

    /**
     * write simplex solution
     * @param lp problem
     */
    public static void write_lp_solution(glp_prob lp) {
        int i;
        int n;
        String name;
        double val;

        name = GLPK.glp_get_obj_name(lp);
        val = GLPK.glp_get_obj_val(lp);
        System.out.print(name);
        System.out.print(" = ");
        System.out.println(val);
        n = GLPK.glp_get_num_cols(lp);
        for (i = 1; i <= n; i++) {
            name = GLPK.glp_get_col_name(lp, i);
            val = GLPK.glp_get_col_prim(lp, i);
            if(val == 1){
                System.out.print(name);
                System.out.print(" = ");
                System.out.println(val);
            }
        }
    }
}
