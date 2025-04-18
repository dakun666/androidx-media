package androidx.media3.datasource;

import androidx.annotation.NonNull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class UploadDataProvider implements Closeable {
  public UploadDataProvider() {
    throw new RuntimeException("Stub!");
  }

  public abstract long getLength() throws IOException;

  public abstract void read(@NonNull UploadDataSink var1, @NonNull ByteBuffer var2) throws IOException;

  public abstract void rewind(@NonNull UploadDataSink var1) throws IOException;

  public void close() throws IOException {
    throw new RuntimeException("Stub!");
  }
}

