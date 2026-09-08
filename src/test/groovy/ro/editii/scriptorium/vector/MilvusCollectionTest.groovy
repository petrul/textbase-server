package ro.editii.scriptorium.vector

import org.junit.jupiter.api.Test

class MilvusCollectionTest {

    @Test
    void arr2List() {
        final int nvectors = 25
        final int vectorDim = 384
        float[][] matrix = new float[nvectors][vectorDim]
        final rnd = new Random();

        for (int i = 0; i < nvectors; i++) {
            for (int j = 0; j < vectorDim; j++) {
                matrix[i][j] = rnd.nextFloat()
            }
        }

        final list = VectorUtils.arr2List(matrix);

        for (int i = 0; i < nvectors; i++) {
            for (int j = 0; j < vectorDim; j++) {
                assert matrix[i][j] == list.get(i).get((j))
            }
        }
    }
}