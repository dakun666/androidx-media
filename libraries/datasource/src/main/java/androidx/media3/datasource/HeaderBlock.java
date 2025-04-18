package androidx.media3.datasource;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Unmodifiable container of headers or trailers.
 */
public abstract class HeaderBlock {
  /**
   * Returns an unmodifiable list of the header field and value pairs.
   * For response, the headers are in the same order they are received over the wire.
   * For request, the headers are in the same order they are added.
   *
   * @return an unmodifiable list of header field and value pairs
   */
  @NonNull
  public abstract List<Map.Entry<String, String>> getAsList();

  /**
   * Returns an unmodifiable map from header field names to lists of values.
   * Order of each list of values for a single header field is:
   * For response, the same order they were received over the wire.
   * For request, the same order they were added.
   * The iteration order of keys is unspecified.
   *
   * @return an unmodifiable map from header field names to lists of values
   */
  @NonNull
  public abstract Map<String, List<String>> getAsMap();
}
