package com.nthexperience.desktop;

import android.content.Context;
import android.view.TextureView;
import androidx.media3.common.*;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.audio.TeeAudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
final class MediaPlayback {
    final ExoPlayer player;
    final Signal signal = new Signal();
    private List<MediaLibrary.Entry> queue = new ArrayList<>();
    private TextureView surface;
    boolean released;

    MediaPlayback(Context context) {
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(context) {
            @Override protected AudioSink buildAudioSink(Context context, boolean floatOutput, boolean playbackParams) {
                return new DefaultAudioSink.Builder(context)
                        .setAudioProcessors(new AudioProcessor[]{new TeeAudioProcessor(signal)}).build();
            }
        };
        player = new ExoPlayer.Builder(context, renderers)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setHandleAudioBecomingNoisy(true)
                .build();
    }
    void play(List<MediaLibrary.Entry> entries, int index) {
        if (index < 0 || index >= entries.size()) return;
        List<MediaItem> items = new ArrayList<>(entries.size());
        for (MediaLibrary.Entry item : entries) {
            items.add(new MediaItem.Builder().setUri(item.uri).setMediaId(item.uri.toString())
                    .setMimeType(item.mime)
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle(item.name).build()).build());
        }
        queue = new ArrayList<>(entries);
        player.setMediaItems(items, index, 0);
        player.prepare();
        player.play();
    }
    List<MediaLibrary.Entry> queue() { return new ArrayList<>(queue); }
    MediaLibrary.Entry current() {
        int index = player.getCurrentMediaItemIndex();
        return index >= 0 && index < queue.size() ? queue.get(index) : null;
    }
    void playPause() {
        if (current() == null) return;
        if (player.getPlayWhenReady() && player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlayerError() == null) {
            player.pause();
            return;
        }
        if (player.getPlayerError() != null) player.prepare();
        if (player.getPlaybackState() == Player.STATE_ENDED) player.seekToDefaultPosition();
        player.play();
    }
    void select(int index) {
        if (index < 0 || index >= player.getMediaItemCount()) return;
        player.seekToDefaultPosition(index);
        if (player.getPlaybackState() == Player.STATE_IDLE) player.prepare();
        player.play();
    }
    void surface(TextureView next) {
        if (surface == next) return;
        if (next != null) player.setVideoTextureView(next);
        else if (surface != null) player.clearVideoTextureView(surface);
        surface = next;
    }
    void clearSurface(TextureView view) {
        if (surface == view) surface(null);
    }
    void release() { released = true; player.release(); }

    /** PCM envelope from the player itself. The microphone and device audio are never used. */
    static final class Signal implements TeeAudioProcessor.AudioBufferSink {
        private static final int SAMPLES = 96;
        private volatile float[] envelope = new float[SAMPLES];
        private int stride = 2, encoding = C.ENCODING_PCM_16BIT;
        @Override public void flush(int sampleRate, int channels, int encoding) {
            this.encoding = encoding;
            stride = Math.max(1, channels) * 2;
            envelope = new float[SAMPLES];
        }
        @Override public void handleBuffer(ByteBuffer buffer) {
            if (encoding != C.ENCODING_PCM_16BIT) return;
            int frames = buffer.remaining() / stride;
            if (frames == 0) return;
            ByteBuffer pcm = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            float[] next = new float[SAMPLES];
            for (int i = 0; i < next.length; i++) {
                int at = pcm.position() + (int)((long)i * frames / next.length) * stride;
                next[i] = pcm.getShort(at) / 32768f;
            }
            envelope = next;
        }
        float[] samples() { return envelope; }
    }
}
