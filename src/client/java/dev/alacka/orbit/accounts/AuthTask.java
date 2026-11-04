package dev.alacka.orbit.accounts;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Bounded daemon workers; closing a screen invalidates every queued UI callback. */
public final class AuthTask implements AutoCloseable {
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), runnable -> {
                Thread thread = new Thread(runnable, "Orbit-account-auth"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private Future<?> future;
    @FunctionalInterface public interface Work<T> { T run(AuthTask task) throws AuthFailure; }

    public boolean cancelled() { return cancelled.get(); }
    public void check() { AuthProtocol.check(this::cancelled); }

    public <T> void start(Minecraft minecraft, Screen owner, Work<T> work, Consumer<T> success, Consumer<AuthFailure> failure) {
        try {
            future = WORKERS.submit(() -> {
                try {
                    check();
                    T value = work.run(this);
                    check();
                    minecraft.execute(() -> {
                        if (!cancelled() && minecraft.screen == owner) success.accept(value);
                    });
                } catch (CancellationException ignored) {
                } catch (AuthFailure error) {
                    minecraft.execute(() -> {
                        if (!cancelled() && minecraft.screen == owner) failure.accept(error);
                    });
                } catch (RuntimeException error) {
                    minecraft.execute(() -> {
                        if (!cancelled() && minecraft.screen == owner) failure.accept(new AuthFailure(AuthFailure.Reason.RESPONSE));
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException error) {
            failure.accept(new AuthFailure(AuthFailure.Reason.RATE_LIMIT));
        }
    }

    public static void invalidateLater(Minecraft minecraft, AuthSession session) {
        if (session == null || session.provider() != AuthSession.Provider.ELY) return;
        try {
            WORKERS.execute(() -> {
                try { new AuthProtocol(new AuthHttp(minecraft.getProxy())).invalidateEly(session); }
                catch (AuthFailure ignored) {}
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {}
    }

    @Override public void close() {
        cancelled.set(true);
        if (future != null) future.cancel(true);
    }
}