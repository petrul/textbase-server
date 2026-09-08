package ro.editii.scriptorium.vector;

import io.milvus.response.SearchResultsWrapper;
import org.jetbrains.annotations.NotNull;
import ro.editii.scriptorium.search.MilvusHit;
import ro.editii.scriptorium.search.content.ContentResolver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class VectorUtils {

    @NotNull
    public static List<List<Float>> arr2List(float[][] vectors) {
        return Arrays.stream(vectors)
                .map(vect -> arr2List(vect))
                .toList();
    }

    public static List<Float> arr2List(float[] vector) {
        return IntStream
                .range(0, vector.length)
                .mapToObj(it -> Float.valueOf(vector[it]))
                .toList();
    }

    public static List<MilvusHit> searchResultsWrapperToHits(SearchResultsWrapper resultsWrapper, ContentResolver contentResolver) {
        final List<SearchResultsWrapper.IDScore> scores = resultsWrapper.getIDScore(0);
        final List<MilvusHit> milvusHits = getMilvusHits(scores, contentResolver);
        return milvusHits;
    }

    @NotNull
    private static List<MilvusHit> getMilvusHits(List<SearchResultsWrapper.IDScore> scores, ContentResolver contentResolver) {
        final var resp  = scores.stream()
                .map(it -> {
//                    final var sid = (String) it.get(MilvusCollection.FIELD_SHA_256);
                    final var url = (String) it.get(MilvusCollection.FIELD_URL);
                    final var cnt = contentResolver.resolve(url);
                    return MilvusHit.from(url, it.getScore(), cnt);
                })
                .toList();
        return resp;
    }


}
