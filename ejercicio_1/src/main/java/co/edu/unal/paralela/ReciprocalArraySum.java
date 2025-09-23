package co.edu.unal.paralela;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Clase que contiene los métodos para implementar la suma de los recíprocos de un arreglo usando paralelismo.
 */
public final class ReciprocalArraySum {

    /**
     * Constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Calcula secuencialmente la suma de valores recíprocos para un arreglo.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Calcula la suma de los recíprocos de los elementos del arreglo
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * calcula el tamaño de cada trozo o sección, de acuerdo con el número de secciones para crear
     * a través de un número dado de elementos.
     *
     * @param nChunks El número de secciones (chunks) para crear
     * @param nElements El número de elementos para dividir
     * @return El tamaño por defecto de la sección (chunk)
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Función techo entera
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Calcula el índice del elemento inclusivo donde la sección/trozo (chunk) inicia,
     * dado que hay cierto número de secciones/trozos (chunks).
     *
     * @param chunk la sección/trozo (chunk) para cacular la posición de inicio
     * @param nChunks Cantidad de secciones/trozos (chunks) creados
     * @param nElements La cantidad de elementos de la sección/trozo que deben atravesarse
     * @return El índice inclusivo donde esta sección/trozo (chunk) inicia en el conjunto de 
     *         nElements
     */
    private static int getChunkStartInclusive(final int chunk,
            final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Calcula el índice del elemento exclusivo que es proporcionado al final de la sección/trozo (chunk),
     * dado que hay cierto número de secciones/trozos (chunks).
     *
     * @param chunk La sección para calcular donde termina
     * @param nChunks Cantidad de secciones/trozos (chunks) creados
     * @param nElements La cantidad de elementos de la sección/trozo que deben atravesarse
     * @return El índice de terminación exclusivo para esta sección/trozo (chunk)
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * Este pedazo de clase puede ser completada para para implementar el cuerpo de cada tarea creada
     * para realizar la suma de los recíprocos del arreglo en paralelo.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        /**
         * Iniciar el índice para el recorrido transversal hecho por esta tarea.
         */
        private final int startIndexInclusive;
        /**
         * Concluir el índice para el recorrido transversal hecho por esta tarea.
         */
        private final int endIndexExclusive;
        /**
         * Arreglo de entrada para la suma de recíprocos.
         */
        private final double[] input;
        /**
         * Valor intermedio producido por esta tarea.
         */
        private double value;

        /**
         * Constructor.
         * @param setStartIndexInclusive establece el índice inicial para comenzar
         *        el recorrido trasversal.
         * @param setEndIndexExclusive establece el índice final para el recorrido trasversal.
         * @param setInput Valores de entrada
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive,
                final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Adquiere el valor calculado por esta tarea.
         * @return El valor calculado por esta tarea
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            // Optimización 1: Variables locales para evitar acceso a campos
            final double[] localInput = this.input;
            final int start = this.startIndexInclusive;
            final int end = this.endIndexExclusive;
            
            double localSum = 0.0;
            
            // Optimización 2: Loop unrolling para reducir overhead del loop
            int i = start;
            int limit = end - 3; // Procesar de 4 en 4
            
            // Procesar 4 elementos por iteración
            for (; i < limit; i += 4) {
                localSum += (1.0 / localInput[i]) + 
                           (1.0 / localInput[i + 1]) + 
                           (1.0 / localInput[i + 2]) + 
                           (1.0 / localInput[i + 3]);
            }
            
            // Procesar elementos restantes
            for (; i < end; i++) {
                localSum += 1.0 / localInput[i];
            }
            
            this.value = localSum;
        }
    }

    /**
     * Para hacer: Modificar este método para calcular la misma suma de recíprocos como le realizada en
     * seqArraySum, pero utilizando dos tareas ejecutándose en paralelo dentro del framework ForkJoin de Java
     * Se puede asumir que el largo del arreglo de entrada 
     * es igualmente divisible por 2.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        // Dividir el arreglo en dos mitades
        int mid = input.length / 2;
        
        // Crear dos tareas para procesar cada mitad del arreglo
        ReciprocalArraySumTask leftTask = new ReciprocalArraySumTask(0, mid, input);
        ReciprocalArraySumTask rightTask = new ReciprocalArraySumTask(mid, input.length, input);
        
        // Ejecutar la tarea izquierda en un hilo separado
        leftTask.fork();
        
        // Ejecutar la tarea derecha en el hilo actual (más eficiente)
        rightTask.compute();
        
        // Esperar a que la tarea izquierda termine y obtener su resultado
        leftTask.join();
        
        // Combinar los resultados
        return leftTask.getValue() + rightTask.getValue();
    }

    /**
     * Para hacer: extender el trabajo hecho para implementar parArraySum que permita utilizar un número establecido
     * de tareas para calcular la suma del arreglo recíproco. 
     * getChunkStartInclusive y getChunkEndExclusive pueden ser útiles para cacular 
     * el rango de elementos índice que pertenecen a cada sección/trozo (chunk).
     *
     * @param input Arreglo de entrada
     * @param numTasks El número de tareas para crear
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parManyTaskArraySum(final double[] input,
            final int numTasks) {
        
        // Crear un arreglo para almacenar las tareas
        ReciprocalArraySumTask[] tasks = new ReciprocalArraySumTask[numTasks];
        
        // Crear las tareas usando los métodos helper para calcular los rangos
        for (int i = 0; i < numTasks; i++) {
            int startIndex = getChunkStartInclusive(i, numTasks, input.length);
            int endIndex = getChunkEndExclusive(i, numTasks, input.length);
            tasks[i] = new ReciprocalArraySumTask(startIndex, endIndex, input);
        }
        
        // Ejecutar todas las tareas excepto la última usando fork()
        for (int i = 0; i < numTasks - 1; i++) {
            tasks[i].fork();
        }
        
        // Ejecutar la última tarea en el hilo actual
        tasks[numTasks - 1].compute();
        
        // Esperar a que todas las tareas forked terminen y combinar resultados
        double sum = tasks[numTasks - 1].getValue();
        for (int i = 0; i < numTasks - 1; i++) {
            tasks[i].join();
            sum += tasks[i].getValue();
        }
        
        return sum;
    }
}
