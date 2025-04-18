package androidx.media3.datasource;

import android.annotation.SuppressLint;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;

@SuppressLint("UserHandleName")
public class ConnectionMigrationOptions {
  private final @MigrationOptionState int mEnableDefaultNetworkMigration;
  private final @MigrationOptionState int mEnablePathDegradationMigration;
  @Nullable
  private final Boolean mAllowServerMigration;
  @Nullable
  private final Boolean mMigrateIdleConnections;
  @Nullable
  private final Duration mIdleMigrationPeriod;
  private final @MigrationOptionState int mAllowNonDefaultNetworkUsage;
  @Nullable
  private final Duration mMaxTimeOnNonDefaultNetwork;
  @Nullable
  private final Integer mMaxWriteErrorNonDefaultNetworkMigrationsCount;
  @Nullable
  private final Integer mMaxPathDegradingNonDefaultMigrationsCount;

  /**
   * Option is unspecified, platform default value will be used.
   */
  public static final int MIGRATION_OPTION_UNSPECIFIED = 0;

  /**
   * Option is enabled.
   */
  public static final int MIGRATION_OPTION_ENABLED = 1;

  /**
   * Option is disabled.
   */
  public static final int MIGRATION_OPTION_DISABLED = 2;

  /** @hide */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(flag = false, value = {
      MIGRATION_OPTION_UNSPECIFIED,
      MIGRATION_OPTION_ENABLED,
      MIGRATION_OPTION_DISABLED,
  })
  public @interface MigrationOptionState {}

  /**
   * See {@link Builder#setDefaultNetworkMigration(int)}
   */
  public @MigrationOptionState int getDefaultNetworkMigration() {
    return mEnableDefaultNetworkMigration;
  }

  /**
   * See {@link Builder#setPathDegradationMigration(int)}
   */
  public @MigrationOptionState int getPathDegradationMigration() {
    return mEnablePathDegradationMigration;
  }

  /**
   * See {@link Builder#setAllowNonDefaultNetworkUsage(int)}
   */
  public @MigrationOptionState int getAllowNonDefaultNetworkUsage() {
    return mAllowNonDefaultNetworkUsage;
  }

  ConnectionMigrationOptions(@NonNull Builder builder) {
    this.mEnableDefaultNetworkMigration = builder.mEnableDefaultNetworkMigration;
    this.mEnablePathDegradationMigration = builder.mEnablePathDegradationMigration;
    this.mAllowServerMigration = builder.mAllowServerMigration;
    this.mMigrateIdleConnections = builder.mMigrateIdleConnections;
    this.mIdleMigrationPeriod = builder.mIdleConnectionMigrationPeriod;
    this.mAllowNonDefaultNetworkUsage = builder.mAllowNonDefaultNetworkUsage;
    this.mMaxTimeOnNonDefaultNetwork = builder.mMaxTimeOnNonDefaultNetwork;
    this.mMaxWriteErrorNonDefaultNetworkMigrationsCount = builder.mMaxWriteErrorNonDefaultNetworkMigrationsCount;
    this.mMaxPathDegradingNonDefaultMigrationsCount = builder.mMaxPathDegradingNonDefaultMigrationsCount;
  }

  /**
   * Builder for {@link ConnectionMigrationOptions}.
   */
  public static final class Builder {
    private @MigrationOptionState int mEnableDefaultNetworkMigration;
    private @MigrationOptionState int mEnablePathDegradationMigration;
    @Nullable
    private Boolean mAllowServerMigration;
    @Nullable
    private Boolean mMigrateIdleConnections;
    @Nullable
    private Duration mIdleConnectionMigrationPeriod;
    private @MigrationOptionState int mAllowNonDefaultNetworkUsage;
    @Nullable
    private Duration mMaxTimeOnNonDefaultNetwork;
    @Nullable
    private Integer mMaxWriteErrorNonDefaultNetworkMigrationsCount;
    @Nullable
    private Integer mMaxPathDegradingNonDefaultMigrationsCount;

    public Builder() {}

    /**
     * Sets whether to enable the possibility of migrating connections on default network
     * change. If enabled, active QUIC connections will be migrated onto the new network when
     * the platform indicates that the default network is changing.
     *
     * @see <a href="https://developer.android.com/training/basics/network-ops/reading-network-state#listening-events">Android
     *     Network State</a>
     *
     * @param state one of the MIGRATION_OPTION_* values
     * @return this builder for chaining
     */
    @NonNull
    public Builder setDefaultNetworkMigration(@MigrationOptionState int state) {
      this.mEnableDefaultNetworkMigration = state;
      return this;
    }

    /**
     * Sets whether to enable the possibility of migrating connections if the current path is
     * performing poorly.
     *
     * <p>Depending on other configuration, this can result to migrating the connections within
     * the same default network, or to a non-default network.
     *
     * @param state one of the MIGRATION_OPTION_* values
     * @return this builder for chaining
     */
    @NonNull
    public Builder setPathDegradationMigration(@MigrationOptionState int state) {
      this.mEnablePathDegradationMigration = state;
      return this;
    }

    /**
     * Sets whether connections can be migrated to an alternate network when Cronet detects
     * a degradation of the path currently in use. Requires setting
     * {@link #setPathDegradationMigration(int)} to {@link #MIGRATION_OPTION_ENABLED} to
     * have any effect.
     *
     * <p>Note: This setting can result in requests being sent on non-default metered networks,
     * eating into the users' data budgets and incurring extra costs. Make sure you're using
     * metered networks sparingly.
     *
     * @param state one of the MIGRATION_OPTION_* values
     * @return this builder for chaining
     */
    @Experimental
    @NonNull
    public Builder setAllowNonDefaultNetworkUsage(@MigrationOptionState int state) {
      this.mAllowNonDefaultNetworkUsage = state;
      return this;
    }

    /**
     * Creates and returns the final {@link ConnectionMigrationOptions} instance, based on the
     * values in this builder.
     */
    @NonNull
    public ConnectionMigrationOptions build() {
      return new ConnectionMigrationOptions(this);
    }
  }

  /**
   * Creates a new builder for {@link ConnectionMigrationOptions}.
   *
   * {@hide}
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * An annotation for APIs which are not considered stable yet.
   *
   * <p>Applications using experimental APIs must acknowledge that they're aware of using APIs
   * that are not considered stable. The APIs might change functionality, break or cease to exist
   * without notice.
   *
   * <p>It's highly recommended to reach out to Cronet maintainers ({@code net-dev@chromium.org})
   * before using one of the APIs annotated as experimental outside of debugging
   * and proof-of-concept code. Be ready to help to help polishing the API, or for a "sorry,
   * really not production ready yet".
   *
   * <p>If you still want to use an experimental API in production, you're doing so at your
   * own risk. You have been warned.
   *
   * {@hide}
   */
  public @interface Experimental {}
}
