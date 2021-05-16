import edu.uci.ics.jung.graph.Graph;
import org.apache.commons.collections15.Factory;

import java.util.*;
import java.util.function.Function;

class GenNPPS<V, E> {
    private Map<Integer, List<V>> map = new HashMap<>(); // Структура для хранения слоев вершин
    private Function<Integer, Double> attachRule;              // Правило предпочтительного связывания
    private Factory<V> vertexFactory;     // Способ создания узла
    private Factory<E> edgeFactory;         // Способ создания ребра
    private double[] numEdgesToAttach;     // Вероятности задающие распределение для приращения
    private Random mRand = new Random();

    GenNPPS(Factory<V> vertexFactory, Factory<E> edgeFactory, double[] probEdgesToAttach, Function<Integer, Double> attachRule) {    // Инициация параметров
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.numEdgesToAttach = probEdgesToAttach;
        this.attachRule = attachRule;
    }

    Graph<V, E> evolve(int step, Graph<V, E> graph) { // функция генерации
        for (V v : graph.getVertices()) {// Заполняем слои
            addToLayer(v, graph.degree(v));
        }
        // итерационный процесс добавления приращений
        for (int i = 0; i < step; i++) {
            V new_n = vertexFactory.create();// создаем узел приращения
            Set<V> list = new HashSet<>();  // вспомогательный список для хранения выбранных вершин
            // разыгрываю случайную число ребер приращения по распределению numEdgesToAttach
            double s = 0.;
            double r = mRand.nextDouble();
            int addEd = 0;
            for (int j = 0; j < numEdgesToAttach.length; j++) {
                s = s + numEdgesToAttach[j];
                if (s > r) {
                    addEd = j;
                    break;
                }
            }
            do {
                int k = getLayer();    // разыгрываю слой, после чего выбираю вершину
                V n = map.get(k).get(mRand.nextInt(map.get(k).size())); // из слоя - равновероятно
                list.add(n); // добавляю в вспомогательный список
            } while (list.size() != addEd);    // пока не выбрано added вершин для присоединения
            graph.addVertex(new_n);                // добавление новой вершины к графу
            for (V n : list) {// добавляю ребра и корректирую список слоев
                int tec = graph.degree(n);
                graph.addEdge(edgeFactory.create(), new_n, n);
                map.get(tec).remove(n);
                addToLayer(n, tec + 1);
            }
            addToLayer(new_n, addEd);
        }
        return graph;
    }

    private void addToLayer(V n, int i) {  // добавляю вершины в список слоев
        List<V> list = map.computeIfAbsent(i, k -> new LinkedList<V>());
        if (!list.contains(n)) list.add(n);
    }

    private int getLayer() {
        {// разыгрываю номер слоя
            int k = 0;
            double rand = mRand.nextDouble();
            double tr = 0;
            double sum = 0.0;
            for (int op : map.keySet())
                sum = sum + attachRule.apply(op) * map.get(op).size();
            for (int l : map.keySet()) {
                int A = map.get(l).size();
                tr = tr + ((double) A * attachRule.apply(l)) / sum;
                if (rand < tr) {
                    k = l;
                    break;
                }
            }
            return k;
        }
    }
}