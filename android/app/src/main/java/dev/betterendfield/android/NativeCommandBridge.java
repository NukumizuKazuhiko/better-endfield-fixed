package dev.betterendfield.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Panel-to-runtime channel. This used to be a JNI bridge, but the runtime
 * library registers under the game's classloader while these classes belong to
 * the LSPosed module classloader — and Android scopes JNI symbol lookup (and
 * .so openings) per classloader, so every call threw UnsatisfiedLinkError while
 * the native modules themselves ran fine. Plain files in the game's own files
 * directory have no such boundary: the native side (input_relay.cpp) tails the
 * input file and publishes the status file.
 *
 * Protocol, one event per line:
 *   "&lt;vk&gt; &lt;action&gt;\n"  latch a virtual key
 *   "c &lt;payload&gt;\n"       submit a runtime command
 *   "r\n"               release every latched key
 *   "m &lt;dx&gt; &lt;dy&gt;\n"     accumulate a mouse-look delta
 *   "p &lt;dx&gt; &lt;dy&gt;\n"     accumulate a PC-layout relative mouse delta
 *   "P &lt;0|1&gt;\n"         report platform pointer capture for the PC layout
 */
final class NativeCommandBridge {
    /** Matches betterendfield::VirtualKeyAction in native/shared/android_compat. */
    static final int KEY_RELEASE = 0;
    static final int KEY_PRESS = 1;
    static final int KEY_PULSE = 2;

    /**
     * Stands in for a newline inside a command payload.
     *
     * <p>The relay frames the input stream by newlines - one event per line -
     * but a runtime command is itself multi-line ({@code BE_COMMAND_V1} on its
     * own line, then the generation, then the verb, then the value). Writing
     * those newlines raw ends the event after its first field, and the relay
     * hands the pump the 13-byte fragment {@code "BE_COMMAND_V1"}, which it
     * rejects outright. Folding them into U+001F keeps the whole command on one
     * line; the native relay unfolds it. A control character, not a printable
     * one, so no verb or value can collide with it.
     */
    private static final char LINE_SEPARATOR = '\u001f';

    private static final Object LOCK = new Object();
    private static volatile File inputFile;
    private static volatile File statusFile;
    private static volatile File nativeLogFile;
    /** Sibling of the status file the relay rewrites for the PC layout. */
    private static volatile File captureFile;

    private NativeCommandBridge() {}

    /** Called from the game process before the native runtime loads. */
    static void configure(File input, File status, File nativeLog) {
        inputFile = input;
        statusFile = status;
        nativeLogFile = nativeLog;
        captureFile = new File(status.getPath() + ".pcmouse");
        // Start every session with an empty stream so the native tail cannot
        // replay events from a previous run of the game.
        try (FileOutputStream fresh = new FileOutputStream(input)) {
            fresh.getChannel().force(false);
        } catch (Throwable ignored) {
            // The panel reports "unable to send" until the directory exists.
        }
        try (FileOutputStream fresh = new FileOutputStream(nativeLog)) {
            fresh.getChannel().force(false);
        } catch (Throwable ignored) {
            // The journal simply stays empty until the runtime creates it.
        }
        try (FileOutputStream fresh = new FileOutputStream(captureFile)) {
            fresh.getChannel().force(false);
        } catch (Throwable ignored) {
            // An absent capture file reads as "not requested".
        }
    }

    /**
     * Submits a runtime command. The payload is multi-line; it travels folded
     * into a single relay line and is unfolded by the native relay. Note the
     * absence of a trim: the payload's own trailing newline is part of the
     * shape the pump validates, and folding it preserves that terminator.
     */
    static boolean submit(String payload) {
        if (payload == null || payload.isEmpty()) return false;
        String flat = payload.replace('\r', ' ').replace('\n', LINE_SEPARATOR);
        return append("c " + flat + "\n");
    }

    /**
     * Presses a Windows virtual-key code in the latch the ported desktop
     * modules poll through GetAsyncKeyState. Returns false while the channel
     * is not configured (no native runtime expected yet).
     */
    static boolean key(int virtualKey, int action) {
        if (action < KEY_RELEASE || action > KEY_PULSE) return false;
        return append(virtualKey + " " + action + "\n");
    }

    static boolean releaseKeys() {
        return append("r\n");
    }

    /**
     * Adds a mouse-look delta, in the screen pixels and the axis directions the
     * free camera's mouse term already expects (x right, y down). Deliberately
     * not a "press and release" pair like {@link #key}: looking is a stream, not
     * a state, and the native side sums whatever arrives instead of queueing it,
     * so a dropped line costs a few pixels rather than a movement.
     */
    static boolean look(int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        return append("m " + dx + " " + dy + "\n");
    }

    /**
     * Adds a PC-layout relative mouse delta. Deliberately not folded into
     * {@link #look}: the free camera's look delta and the game's own
     * Mouse X / Mouse Y axes are different consumers, and the layout only
     * routes motion to the second one.
     */
    static boolean pcMouseMotion(float dx, float dy) {
        if (dx == 0.0f && dy == 0.0f) return false;
        // Float.toString round-trips exactly through the relay's strtof,
        // so the delta the panel captured is the delta the game receives.
        return append("p " + Float.toString(dx) + " " + Float.toString(dy) + "\n");
    }

    /**
     * Reports whether the platform granted the pointer capture the native
     * state machine asked for. The panel must not forward relative
     * coordinates the game did not request.
     */
    static boolean pcMouseCaptured(boolean captured) {
        return append(captured ? "P 1\n" : "P 0\n");
    }

    /**
     * The PC layout's capture request, republished by the native relay.
     * Absent or unreadable reads as "not requested": the panel then leaves
     * the game's own pointer handling alone.
     */
    static boolean pcMouseCaptureRequested() {
        File file = captureFile;
        if (file == null) return false;
        try (FileInputStream stream = new FileInputStream(file)) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return text.contains("pc_capture=1");
        } catch (IOException notYet) {
            return false;
        }
    }

    /**
     * The runtime command status the native relay publishes. Empty until the
     * native runtime has loaded — the same "not ready yet" signal the JNI
     * version used to give through exceptions.
     */
    static String status() {
        File file = statusFile;
        if (file == null) return "";
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] bytes = stream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (IOException notYet) {
            return "";
        }
    }

    /**
     * Appends native log lines written since the previous call to {@code into}
     * (complete lines only; a trailing partial line waits for its newline).
     * {@code offset[0]} is the read cursor, owned by the caller. Empty when the
     * runtime has not produced anything (or has not loaded).
     */
    static void tailNativeLog(StringBuilder into, long[] offset) {
        File file = nativeLogFile;
        if (file == null) return;
        try (FileInputStream stream = new FileInputStream(file)) {
            long size = stream.getChannel().size();
            if (size < offset[0]) offset[0] = 0;  // the relay truncated the file
            if (size == offset[0]) return;
            stream.getChannel().position(offset[0]);
            byte[] chunk = new byte[(int) Math.min(size - offset[0], 65536)];
            int got = stream.read(chunk);
            if (got <= 0) return;
            offset[0] += got;
            into.append(new String(chunk, 0, got, StandardCharsets.UTF_8));
        } catch (IOException notYet) {
            // The relay creates the file when the runtime loads.
        }
    }

    private static boolean append(String line) {
        File file = inputFile;
        if (file == null) return false;
        synchronized (LOCK) {
            try (FileOutputStream stream = new FileOutputStream(file, true)) {
                stream.write(line.getBytes(StandardCharsets.UTF_8));
                stream.flush();
                return true;
            } catch (IOException broken) {
                return false;
            }
        }
    }
}
