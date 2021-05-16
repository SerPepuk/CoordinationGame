import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import org.apache.commons.collections15.Factory;

import java.util.function.Function;

class CoordinationGame {
    private static Factory<Node> vertexFactory = new Factory<Node>() {  // фабрика для создания вершин
        int i = 1;

        public Node create() {
            return new Node(i++);
        }
    };
    private static Factory<Object> edgeFactory = Object::new;

    private Graph<Node, Object> graph;
    private int colDopNodes;
    private int colGenNodes;

    private int minColNodes;
    private String minConf;

    private Graph<Node, Object> getGraphSeed() {
        Graph<Node, Object> graph = new UndirectedSparseGraph<>();
        for (int i = 0; i < colGenNodes; i++) {
            graph.addVertex(vertexFactory.create());
        }
        Object[] mass = graph.getVertices().toArray();    //добавляем ребра между добавленными узлами
        for (int i = 0; i < mass.length - 1; i++)
            for (int j = i + 1; j < mass.length; j++)
                if (i != j) {
                    graph.addEdge(edgeFactory.create(), (Node) mass[i], (Node) mass[j]);
                }
        return graph;
    }

    private Graph<Node, Object> generateSystem() {
        Function<Integer, Double> pa = (k) -> 1. * k;
        double[] r = new double[]{0.0, 0.0, 1};
        GenNPPS<Node, Object> generator = new GenNPPS<>(vertexFactory, edgeFactory, r, pa);
        graph = generator.evolve(colDopNodes, getGraphSeed());
        return graph;
    }

    private String formingString(String confStr) {
        StringBuilder stringBuilder = new StringBuilder(confStr);
        for (int i = stringBuilder.length(); i < colDopNodes + colGenNodes; i++) {
            stringBuilder.insert(0, "0");
        }
        confStr = stringBuilder.toString();
        return confStr;
    }

    private void setNewConfig(String string) {
        int i = 0;
        for (Node node : graph.getVertices()) {
            if (String.valueOf(string.charAt(i)).equals("1")) {
                node.setStatus(true);
            } else {
                node.setStatus(false);
            }
            i++;
            node.fixNewStatus();
        }
    }

    private void updateStatus() {
        for (int i = 0; i < colDopNodes; i++) {
//            Проходим по свем узлам
            for (Node node : graph.getVertices()) {
//                Проверяем состояние узла
                if (!node.isActivated()) {
                    int act_n = 0;
                    int pas_n = 0;
//                Проверяем соседние узлы
                    for (Node node_neighbor : graph.getNeighbors(node)) {
                        if (node_neighbor.isActivated()) {
                            act_n++;
                        } else {
                            pas_n++;
                        }
                    }
//                    Проверяем количество соседних узлов приняших решение
                    if (act_n > pas_n) {
                        node.setStatus(true);
                    }
                }
            }
        }
    }

    private void fixNewStatus() {
        for (Node node : graph.getVertices()) {
            node.fixNewStatus();
        }
    }

    private boolean systemHasChanged() {
        for (Node node : graph.getVertices()) {
            if (!node.statusHasNotChanged()) {
                return true;
            }
        }
        return false;
    }

    private boolean allNodesIsActivated() {
        for (Node node : graph.getVertices()) {
            if (!node.isActivated()) {
                return false;
            }
        }
        return true;
    }

    private boolean checkSolution(int colActNodes, int conf) {
        if (colActNodes < minColNodes) {
            while (true) {
                updateStatus();
                if (systemHasChanged()) {
                    fixNewStatus();
                    if (allNodesIsActivated()) {
                        minConf = formingString(Integer.toBinaryString(conf));
                        minColNodes = colActNodes;
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    void modeling(int colDop, int colGen) {
        colDopNodes = colDop;
        colGenNodes = colGen;

        minColNodes = colDop + colGenNodes;
        graph = generateSystem();
        enumerationMethod();
        System.out.println("Решение путём перебора :\n");
        showSolution();

        minColNodes = colDop + colGenNodes;
        greedMethod();
        System.out.println("\nРешение при помощи жадного алгоритма :\n");
        showSolution();
    }

    private void greedMethod() {
        int max = 1;
        for (int newConf = 1; newConf < Math.pow(2, colDopNodes + colGenNodes); newConf *= 2) {
            findNextNodes(newConf, max);
        }
    }

    private void findNextNodes(int conf, int max) {
        String oldConfStr = formingString(Integer.toBinaryString(conf));
        int maxActNodes = max;
        int maxConf = conf;
        int actNodes;
        if (max < minColNodes) {
            for (int i = 1; i < Math.pow(2, colDopNodes + colGenNodes); i *= 2) {
                int index = formingString(Integer.toBinaryString(i)).indexOf("1");
                if ((oldConfStr.charAt(index)) == '1') {
                    continue;
                }
                int newConf = conf + i;
//            Установка текущей конфигурации
                String newConfStr = formingString(Integer.toBinaryString(newConf));
                setNewConfig(newConfStr);
                updateStatus();
                fixNewStatus();
//            Подсчёт активных узлов
                actNodes = 0;
                for (Node node : graph.getVertices()) {
                    if (node.isActivated()) {
                        actNodes++;
                    }
                }
//            Проверка числа активных узлов
                if (actNodes > maxActNodes) {
                    maxConf = newConf;
                    maxActNodes = actNodes;
                }
            }
            if (checkSolution(max + 1, maxConf)) {
                return;
            }
            findNextNodes(maxConf, max + 1);
        }
    }

    private void enumerationMethod() {
        for (int i = 1; i < Math.pow(2, colDopNodes + colGenNodes); i++) {
            String newConfStr = formingString(Integer.toBinaryString(i));
            setNewConfig(newConfStr);
            int colActNodes = newConfStr.length() - newConfStr.replace("1", "").length();
            checkSolution(colActNodes, i);
        }
    }

    private void showSolution() {
        setNewConfig(minConf);
        for (Node node : graph.getVertices()) {
            System.out.print(node.getId() + "-й узел (" + node.isActivated() + ") связан с : ");
            for (Node node_neighbor : graph.getNeighbors(node)) {
                System.out.print(node_neighbor.getId() + ", ");
            }
            System.out.println();
        }
        System.out.println("Минимальное количество узлов для решения : " + minColNodes + "\n Ответ : " + minConf);
    }
}
