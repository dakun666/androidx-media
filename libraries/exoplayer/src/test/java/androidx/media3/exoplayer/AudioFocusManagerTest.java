/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.exoplayer;

import static androidx.media3.exoplayer.AudioFocusManager.PLAYER_COMMAND_DO_NOT_PLAY;
import static androidx.media3.exoplayer.AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY;
import static androidx.media3.exoplayer.AudioFocusManager.PLAYER_COMMAND_WAIT_FOR_CALLBACK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAudioManager;

/** Unit tests for {@link AudioFocusManager}. */
@RunWith(AndroidJUnit4.class)
public class AudioFocusManagerTest {
  private static final int NO_COMMAND_RECEIVED = ~PLAYER_COMMAND_WAIT_FOR_CALLBACK;

  private AudioFocusManager audioFocusManager;
  private TestPlayerControl testPlayerControl;

  private AudioManager audioManager;

  @Before
  public void setUp() {
    audioManager =
        (AudioManager)
            ApplicationProvider.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

    testPlayerControl = new TestPlayerControl();
    audioFocusManager =
        new AudioFocusManager(
            ApplicationProvider.getApplicationContext(),
            new Handler(Looper.myLooper()),
            testPlayerControl);
  }

  @Test
  public void setAudioAttributes_withNullUsage_doesNotManageAudioFocus() {
    audioFocusManager.setAudioAttributes(/* audioAttributes= */ null);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_IDLE))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
//        Shadows.shadowOf(audioManager).getLastAudioFocusRequest();
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(request).isNull();
  }

  @Test
  @Config(maxSdk = 25)
  public void setAudioAttributes_withNullUsage_abandonsAudioFocus() {
//    Shadows.shadowOf(audioManager)
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
//        Shadows.shadowOf(audioManager).getLastAudioFocusRequest();
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(request.durationHint).isEqualTo(AudioManager.AUDIOFOCUS_GAIN);

    // Ensure that setting null audio attributes with focus releases focus.
    audioFocusManager.setAudioAttributes(/* audioAttributes= */ null);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    AudioManager.OnAudioFocusChangeListener lastRequest =
//        Shadows.shadowOf(audioManager).getLastAbandonedAudioFocusListener();
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener();
    assertThat(lastRequest).isNotNull();
  }

  @Test
  @Config(minSdk = 26)
  public void setAudioAttributes_withNullUsage_abandonsAudioFocus_v26() {
//    Shadows.shadowOf(audioManager)
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
//        Shadows.shadowOf(audioManager).getLastAudioFocusRequest();
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(getAudioFocusGainFromRequest(request)).isEqualTo(AudioManager.AUDIOFOCUS_GAIN);

    // Ensure that setting null audio attributes with focus releases focus.
    audioFocusManager.setAudioAttributes(/* audioAttributes= */ null);
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    AudioFocusRequest lastRequest =
//        Shadows.shadowOf(audioManager).getLastAbandonedAudioFocusRequest();
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest();
    assertThat(lastRequest).isNotNull();
  }

  @Test
  public void setAudioAttributes_withUsageAlarm_throwsIllegalArgumentException() {
    // USAGE_ALARM attributes map to AUDIOFOCUS_GAIN_TRANSIENT, which should result in failure.
    AudioAttributes alarm = new AudioAttributes.Builder().setUsage(C.USAGE_ALARM).build();
    try {
      audioFocusManager.setAudioAttributes(alarm);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void setAudioAttributes_withUsageMedia_usesAudioFocusGain() {
//    Shadows.shadowOf(audioManager)
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);

    AudioAttributes mediaAudioAttributes =
        new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).build();
    audioFocusManager.setAudioAttributes(mediaAudioAttributes);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
//        Shadows.shadowOf(audioManager).getLastAudioFocusRequest();
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(getAudioFocusGainFromRequest(request)).isEqualTo(AudioManager.AUDIOFOCUS_GAIN);
  }

  @Test
  public void setAudioAttributes_inEndedState_requestsAudioFocus() {
//    Shadows.shadowOf(audioManager)
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);

    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_ENDED))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(getAudioFocusGainFromRequest(request)).isEqualTo(AudioManager.AUDIOFOCUS_GAIN);
  }

  @Test
  public void updateAudioFocus_idleToBuffering_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);

    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_IDLE))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest()).isNull();
    assertThat(
            audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_BUFFERING))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(getAudioFocusGainFromRequest(request)).isEqualTo(AudioManager.AUDIOFOCUS_GAIN);
  }

  @Test
  public void updateAudioFocus_pausedToPlaying_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    // Audio focus should not be requested yet, because playWhenReady is false.
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest()).isNull();

    // Audio focus should be requested now that playWhenReady is true.
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(getAudioFocusGainFromRequest(request)).isEqualTo(AudioManager.AUDIOFOCUS_GAIN);
  }

  @Test
  public void updateAudioFocus_pausedToPlaying_withTransientLoss_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    // Simulate transient focus loss.
    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);

    // Focus should be re-requested rather than staying in a state of transient focus loss. See
    // https://github.com/google/ExoPlayer/issues/7182 for context.
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
  }

  @Test
  public void updateAudioFocus_pausedToPlaying_withTransientDuck_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    // Simulate transient ducking.
    audioFocusManager
        .getFocusListener()
        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    ((org.robolectric.shadows.ShadowLooper)Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastVolumeMultiplier).isLessThan(1.0f);
    // Focus should be re-requested, rather than staying in a state of transient ducking. This
    // should restore the volume to 1.0. See https://github.com/google/ExoPlayer/issues/7182 for
    // context.
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(testPlayerControl.lastVolumeMultiplier).isEqualTo(1.0f);
  }

  @Test
  public void updateAudioFocus_toPausedBeforeRequestingFocus_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    @AudioFocusManager.PlayerCommand
    int playerCommand =
        audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY);

    assertThat(playerCommand).isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
  }

  @Test
  public void updateAudioFocus_toPausedWithFocus_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);
    audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY);

    @AudioFocusManager.PlayerCommand
    int playerCommand =
        audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY);

    assertThat(playerCommand).isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
  }

  @Test
  public void updateAudioFocus_toPausedWithFocusLoss_setsPlayerCommandDoNotPlay() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);
    audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY);
    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    @AudioFocusManager.PlayerCommand
    int playerCommand =
        audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY);

    assertThat(playerCommand).isEqualTo(PLAYER_COMMAND_DO_NOT_PLAY);
  }

  @Test
  public void updateAudioFocus_toPausedWithTransientFocusLoss_setsPlayerCommandWaitForCallback() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);
    audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY);
    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    @AudioFocusManager.PlayerCommand
    int playerCommand =
        audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY);

    assertThat(playerCommand).isEqualTo(PLAYER_COMMAND_WAIT_FOR_CALLBACK);
  }

  @Test
  public void
      updateAudioFocus_toPausedWithTransientFocusLossCanDuck_setsPlayerCommandPlayWhenReady() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);
    audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY);
    audioFocusManager
        .getFocusListener()
        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    @AudioFocusManager.PlayerCommand
    int playerCommand =
        audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY);

    assertThat(playerCommand).isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
  }

  @Test
  public void updateAudioFocus_abandonFocusWhenDucked_restoresFullVolume() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    // Simulate transient ducking.
    audioFocusManager
        .getFocusListener()
        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastVolumeMultiplier).isLessThan(1.0f);

    // Configure the manager to no longer handle focus.
    audioFocusManager.setAudioAttributes(null);

    // Focus should be abandoned, which should restore the volume to 1.0.
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(testPlayerControl.lastVolumeMultiplier).isEqualTo(1.0f);
  }

  @Test
  @Config(maxSdk = 25)
  public void updateAudioFocus_readyToIdle_abandonsAudioFocus() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener()).isNull();

    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_IDLE))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener())
        .isEqualTo(request.listener);
  }

  @Test
  @Config(minSdk = 26)
  public void updateAudioFocus_readyToIdle_abandonsAudioFocus_v26() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest()).isNull();

    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_IDLE))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest())
        .isEqualTo(request.audioFocusRequest);
  }

  @Test
  @Config(maxSdk = 25)
  public void updateAudioFocus_readyToIdle_withoutFocus_isNoOp() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(null);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener()).isNull();
    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(request).isNull();

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_IDLE))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener()).isNull();
  }

  @Test
  @Config(minSdk = 26)
  public void updateAudioFocus_readyToIdle_withoutFocus_isNoOp_v26() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(null);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest()).isNull();
    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    assertThat(request).isNull();

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ false, Player.STATE_IDLE))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest()).isNull();
  }

  @Test
  public void release_doesNotCallPlayerControlToRestoreVolume() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    // Simulate transient ducking.
    audioFocusManager
        .getFocusListener()
        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastVolumeMultiplier).isLessThan(1.0f);

    audioFocusManager.release();

    // PlaybackController.setVolumeMultiplier should not have been called to restore the volume.
    assertThat(testPlayerControl.lastVolumeMultiplier).isLessThan(1.0f);
  }

  @Test
  public void onAudioFocusChange_withDuckEnabled_reducesAndRestoresVolume() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    audioFocusManager
        .getFocusListener()
        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastVolumeMultiplier).isLessThan(1.0f);
    assertThat(testPlayerControl.lastPlayerCommand).isEqualTo(NO_COMMAND_RECEIVED);

    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastVolumeMultiplier).isEqualTo(1.0f);
  }

  @Test
  public void onAudioFocusChange_withPausedWhenDucked_sendsCommandWaitForCallback() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);

    AudioAttributes speechAudioAttributes =
        new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build();
    audioFocusManager.setAudioAttributes(speechAudioAttributes);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    audioFocusManager
        .getFocusListener()
        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastPlayerCommand).isEqualTo(PLAYER_COMMAND_WAIT_FOR_CALLBACK);
    assertThat(testPlayerControl.lastVolumeMultiplier).isEqualTo(1.0f);

    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastPlayerCommand).isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
  }

  @Test
  public void onAudioFocusChange_withTransientLoss_sendsCommandWaitForCallback() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);

    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastVolumeMultiplier).isEqualTo(1.0f);
    assertThat(testPlayerControl.lastPlayerCommand).isEqualTo(PLAYER_COMMAND_WAIT_FOR_CALLBACK);
  }

  @Test
  @Config(maxSdk = 25)
  public void onAudioFocusChange_withFocusLoss_sendsDoNotPlayAndAbandonsFocus() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener()).isNull();

    ShadowAudioManager.AudioFocusRequest request =
        ((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest();
    request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastPlayerCommand).isEqualTo(PLAYER_COMMAND_DO_NOT_PLAY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusListener())
        .isEqualTo(request.listener);
  }

  @Test
  @Config(minSdk = 26)
  public void onAudioFocusChange_withFocusLoss_sendsDoNotPlayAndAbandonsFocus_v26() {
    ((ShadowAudioManager) Shadow.extract(audioManager))
        .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    audioFocusManager.setAudioAttributes(AudioAttributes.DEFAULT);

    assertThat(audioFocusManager.updateAudioFocus(/* playWhenReady= */ true, Player.STATE_READY))
        .isEqualTo(PLAYER_COMMAND_PLAY_WHEN_READY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest()).isNull();

    audioFocusManager.getFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
    ((org.robolectric.shadows.ShadowLooper) Shadow.extract(Looper.getMainLooper())).idle();

    assertThat(testPlayerControl.lastPlayerCommand).isEqualTo(PLAYER_COMMAND_DO_NOT_PLAY);
    assertThat(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAbandonedAudioFocusRequest())
        .isEqualTo(((ShadowAudioManager) Shadow.extract(audioManager)).getLastAudioFocusRequest().audioFocusRequest);
  }

  private int getAudioFocusGainFromRequest(ShadowAudioManager.AudioFocusRequest audioFocusRequest) {
    return Util.SDK_INT >= 26
        ? audioFocusRequest.audioFocusRequest.getFocusGain()
        : audioFocusRequest.durationHint;
  }

  private static class TestPlayerControl implements AudioFocusManager.PlayerControl {
    private float lastVolumeMultiplier = 1.0f;
    private int lastPlayerCommand = NO_COMMAND_RECEIVED;

    @Override
    public void setVolumeMultiplier(float volumeMultiplier) {
      lastVolumeMultiplier = volumeMultiplier;
    }

    @Override
    public void executePlayerCommand(int playerCommand) {
      lastPlayerCommand = playerCommand;
    }
  }
}
