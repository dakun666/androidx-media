package androidx.media3.datasource;

import androidx.annotation.NonNull;

public abstract class UploadDataSink {
  public UploadDataSink() {
    throw new RuntimeException("Stub!");
  }

  public abstract void onReadSucceeded(boolean var1);

  public abstract void onReadError(@NonNull Exception var1);

  public abstract void onRewindSucceeded();

  public abstract void onRewindError(@NonNull Exception var1);
}

