package kz.asfendiyarov.heartsimulator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final int PURPLE = Color.rgb(82, 38, 105);
    private static final int PURPLE_DARK = Color.rgb(52, 23, 68);
    private static final int GOLD = Color.rgb(201, 167, 91);
    private static final int BG = Color.rgb(247, 245, 249);
    private static final int CARD = Color.WHITE;
    private static final int INK = Color.rgb(34, 28, 38);
    private static final int MUTED = Color.rgb(106, 95, 111);
    private static final int GREEN = Color.rgb(44, 139, 76);
    private static final int AMBER = Color.rgb(204, 149, 35);
    private static final int RED = Color.rgb(188, 63, 63);
    private static final int LINE = Color.rgb(229, 222, 233);

    private static final String[] NAMES = {
            "Норма, синусовый ритм",
            "Аортальный стеноз",
            "Аортальная регургитация",
            "Митральный стеноз",
            "Митральная регургитация",
            "Пролапс митрального клапана",
            "Стеноз клапана лёгочной артерии",
            "Трикуспидальная регургитация",
            "Дефект межжелудочковой перегородки",
            "Дефект межпредсердной перегородки",
            "Открытый артериальный проток",
            "Гипертрофическая обструктивная кардиомиопатия",
            "Тетрада Фалло",
            "III тон / желудочковый галоп",
            "IV тон / предсердный галоп",
            "Шум трения перикарда",
            "Фибрилляция предсердий",
            "Желудочковая экстрасистолия",
            "Выраженная синусовая тахикардия",
            "Полная атриовентрикулярная блокада"
    };

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicBoolean pollBusy = new AtomicBoolean(false);
    private final AtomicBoolean commandBusy = new AtomicBoolean(false);
    private final Map<Integer, Button> trackButtons = new LinkedHashMap<>();

    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback wifiCallback;
    private Network selectedWifiNetwork;
    private boolean running = true;
    private boolean connected = false;
    private int failedPolls = 0;
    private long lastLatencyMs = 0;

    private DeviceState state = new DeviceState();
    private TextView connectionPill;
    private TextView nowLabel;
    private TextView nowName;
    private TextView nowDetail;
    private TextView fiveChannels;
    private Button pauseButton;
    private Button stopButton;
    private Button restartButton;
    private SeekBar volumeSeek;
    private TextView volumeValue;
    private boolean volumeDragging = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(PURPLE_DARK);
            getWindow().setNavigationBarColor(Color.WHITE);
        }
        buildUi();
        bindWifi();
        startPolling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (running) poll();
    }

    @Override
    protected void onDestroy() {
        running = false;
        ApiClient.setNetwork(null);
        if (connectivity != null && wifiCallback != null) {
            try { connectivity.unregisterNetworkCallback(wifiCallback); } catch (Exception ignored) {}
        }
        io.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(-1, dp(82)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content.addView(buildNowCard(), marginTop(0));
        content.addView(buildControlsCard(), marginTop(10));
        content.addView(buildVolumeCard(), marginTop(10));
        content.addView(buildTracksCard(), marginTop(10));
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(16), dp(10));
        header.setBackgroundColor(PURPLE_DARK);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("HEART AUSCULTATION", 18, Typeface.BOLD, Color.WHITE);
        TextView sub = text("5 точек · 20 состояний", 12, Typeface.NORMAL, Color.rgb(231, 218, 235));
        titles.addView(title);
        titles.addView(sub);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        connectionPill = text("Нет связи", 12, Typeface.BOLD, Color.WHITE);
        connectionPill.setGravity(Gravity.CENTER);
        connectionPill.setPadding(dp(12), dp(9), dp(12), dp(9));
        setBg(connectionPill, RED, 999, 0, 0);
        connectionPill.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        header.addView(connectionPill, new LinearLayout.LayoutParams(-2, -2));
        return header;
    }

    private LinearLayout buildNowCard() {
        LinearLayout card = card();
        TextView cap = text("СЕЙЧАС ЗВУЧИТ", 12, Typeface.BOLD, PURPLE);
        card.addView(cap);

        nowLabel = text("STOP", 30, Typeface.BOLD, INK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        card.addView(nowLabel, lp);

        nowName = text("Выберите норму или патологию ниже", 18, Typeface.BOLD, INK);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(4);
        card.addView(nowName, np);

        nowDetail = text("Телефон должен быть подключён к Wi‑Fi Heart-Simulator", 13, Typeface.NORMAL, MUTED);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
        dpv.topMargin = dp(6);
        card.addView(nowDetail, dpv);

        fiveChannels = text("5 точек: аорта · лёгочная · Эрб · трикуспидальная · митральная", 12, Typeface.BOLD, MUTED);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(12);
        card.addView(fiveChannels, fp);
        return card;
    }

    private LinearLayout buildControlsCard() {
        LinearLayout card = card();
        card.addView(text("Управление", 16, Typeface.BOLD, INK));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(10);
        card.addView(row, rp);

        pauseButton = bigButton("ПАУЗА", AMBER, Color.WHITE);
        stopButton = bigButton("STOP", RED, Color.WHITE);
        row.addView(pauseButton, new LinearLayout.LayoutParams(0, dp(56), 1));
        row.addView(space(dp(8), 1));
        row.addView(stopButton, new LinearLayout.LayoutParams(0, dp(56), 1));

        restartButton = bigButton("ПЕРЕЗАПУСТИТЬ ТЕКУЩИЙ ЗВУК", PURPLE, Color.WHITE);
        LinearLayout.LayoutParams rr = new LinearLayout.LayoutParams(-1, dp(52));
        rr.topMargin = dp(8);
        card.addView(restartButton, rr);

        pauseButton.setOnClickListener(v -> {
            if (state.isPaused()) command("/api/resume", "STARTING");
            else command("/api/pause", "PAUSING");
        });
        stopButton.setOnClickListener(v -> command("/api/stop", "STOPPING"));
        restartButton.setOnClickListener(v -> command("/api/restart", "STARTING"));
        return card;
    }

    private LinearLayout buildVolumeCard() {
        LinearLayout card = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text("Громкость", 16, Typeface.BOLD, INK), new LinearLayout.LayoutParams(0, -2, 1));
        volumeValue = text("15 / 30", 16, Typeface.BOLD, PURPLE);
        top.addView(volumeValue);
        card.addView(top);

        volumeSeek = new SeekBar(this);
        volumeSeek.setMax(30);
        volumeSeek.setProgress(15);
        card.addView(volumeSeek, new LinearLayout.LayoutParams(-1, dp(48)));
        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeValue.setText(progress + " / 30");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { volumeDragging = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                volumeDragging = false;
                command("/api/volume?v=" + seekBar.getProgress(), null);
            }
        });
        return card;
    }

    private LinearLayout buildTracksCard() {
        LinearLayout card = card();
        card.addView(text("Выберите состояние", 18, Typeface.BOLD, INK));
        TextView hint = text("Нажатие сразу запускает выбранный звук на всех 5 MP3-модулях. Звук повторяется по кругу.", 12, Typeface.NORMAL, MUTED);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(4);
        card.addView(hint, hp);

        TextView normalHeader = text("НОРМА", 12, Typeface.BOLD, GREEN);
        LinearLayout.LayoutParams nh = new LinearLayout.LayoutParams(-1, -2);
        nh.topMargin = dp(14);
        card.addView(normalHeader, nh);
        card.addView(trackButton(1), new LinearLayout.LayoutParams(-1, dp(58)));

        TextView pathHeader = text("ПАТОЛОГИИ", 12, Typeface.BOLD, PURPLE);
        LinearLayout.LayoutParams ph = new LinearLayout.LayoutParams(-1, -2);
        ph.topMargin = dp(18);
        card.addView(pathHeader, ph);

        for (int track = 2; track <= 20; track++) {
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(58));
            bp.topMargin = dp(6);
            card.addView(trackButton(track), bp);
        }
        return card;
    }

    private Button trackButton(int track) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setTextSize(14);
        b.setPadding(dp(14), 0, dp(12), 0);
        b.setText(String.format(Locale.ROOT, "%02d. %s", track, NAMES[track - 1]));
        b.setTag(track);
        b.setOnClickListener(v -> play((Integer) v.getTag()));
        styleTrackButton(b, false, false);
        trackButtons.put(track, b);
        return b;
    }

    private void play(int track) {
        if (!connected) {
            Toast.makeText(this, "Нет связи с ESP32. Подключитесь к Wi‑Fi Heart-Simulator.", Toast.LENGTH_LONG).show();
            return;
        }
        state.track = track;
        state.trackName = NAMES[track - 1];
        state.playbackState = "STARTING";
        renderState();
        command("/api/play?track=" + track, "STARTING");
    }

    private void bindWifi() {
        connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) return;
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        wifiCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                selectedWifiNetwork = network;
                ApiClient.setNetwork(network);
                runOnUiThread(() -> {
                    connectionPill.setText("Wi‑Fi · проверка…");
                    setBg(connectionPill, AMBER, 999, 0, 0);
                });
                poll();
            }
            @Override public void onLost(Network network) {
                if (selectedWifiNetwork != null && selectedWifiNetwork.equals(network)) {
                    selectedWifiNetwork = null;
                    ApiClient.setNetwork(null);
                    runOnUiThread(this::lost);
                }
            }
            private void lost() { setDisconnected(); }
        };
        try { connectivity.registerNetworkCallback(request, wifiCallback); }
        catch (Exception e) { setDisconnected(); }
    }

    private void startPolling() {
        ui.post(new Runnable() {
            @Override public void run() {
                if (!running) return;
                if (!commandBusy.get()) poll();
                ui.postDelayed(this, 800);
            }
        });
    }

    private void poll() {
        if (!pollBusy.compareAndSet(false, true)) return;
        io.execute(() -> {
            long start = SystemClock.elapsedRealtime();
            try {
                JSONObject json = ApiClient.getJson("/api/status");
                DeviceState next = DeviceState.from(json);
                lastLatencyMs = SystemClock.elapsedRealtime() - start;
                failedPolls = 0;
                runOnUiThread(() -> applyState(next));
            } catch (Exception e) {
                failedPolls++;
                if (failedPolls >= 2) runOnUiThread(this::setDisconnected);
            } finally {
                pollBusy.set(false);
            }
        });
    }

    private void command(String path, String optimisticState) {
        if (!connected) {
            Toast.makeText(this, "Нет связи с ESP32.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!commandBusy.compareAndSet(false, true)) return;

        if (optimisticState != null) {
            state.playbackState = optimisticState;
            renderState();
        }
        updateButtonsEnabled();
        try { getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
        catch (Exception ignored) {}

        io.execute(() -> {
            try {
                ApiClient.get(path, path.startsWith("/api/restart") ? 2500 : 1800);
                JSONObject json = ApiClient.getJson("/api/status");
                DeviceState next = DeviceState.from(json);
                runOnUiThread(() -> applyState(next));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Команда не подтверждена ESP32: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                commandBusy.set(false);
                runOnUiThread(this::updateButtonsEnabled);
            }
        });
    }

    private void applyState(DeviceState next) {
        state = next;
        connected = true;
        connectionPill.setText("ESP32 ✓ · " + lastLatencyMs + " ms");
        setBg(connectionPill, GREEN, 999, 0, 0);
        if (!volumeDragging) {
            volumeSeek.setProgress(state.volume);
            volumeValue.setText(state.volume + " / 30");
        }
        renderState();
        updateButtonsEnabled();
    }

    private void renderState() {
        boolean stopped = state.track <= 0 || "STOPPED".equals(state.playbackState);
        boolean paused = state.isPaused();

        if ("STOPPING".equals(state.playbackState)) {
            nowLabel.setText("■ ОСТАНОВКА…");
            nowName.setText("Команда STOP отправлена");
            nowDetail.setText("Ожидание подтверждения ESP32");
            fiveChannels.setTextColor(MUTED);
        } else if (stopped) {
            nowLabel.setText("■ STOP");
            nowName.setText("Выберите норму или патологию ниже");
            nowDetail.setText(connected ? "ESP32 готов к воспроизведению" : "Подключитесь к Wi‑Fi Heart-Simulator");
            fiveChannels.setTextColor(MUTED);
        } else {
            String name = state.track >= 1 && state.track <= 20 ? NAMES[state.track - 1] : state.trackName;
            nowLabel.setText(paused ? "Ⅱ ПАУЗА" : "▶ ИГРАЕТ ПО КРУГУ");
            nowName.setText(name == null || name.isEmpty() ? ("Трек " + state.track) : name);
            nowDetail.setText(String.format(Locale.ROOT, "%04d.mp3 · одновременно на 5 точках", state.track));
            fiveChannels.setText("5 точек воспроизведения: аорта · лёгочная · Эрб · трикуспидальная · митральная");
            fiveChannels.setTextColor(paused ? AMBER : GREEN);
        }

        for (Map.Entry<Integer, Button> entry : trackButtons.entrySet()) {
            boolean active = !stopped && entry.getKey() == state.track;
            styleTrackButton(entry.getValue(), active, paused);
        }
    }

    private void updateButtonsEnabled() {
        boolean busy = commandBusy.get();
        boolean stopped = state.track <= 0 || state.isStopped();
        boolean transientState = state.isTransient();
        for (Button b : trackButtons.values()) b.setEnabled(connected && !busy);
        volumeSeek.setEnabled(connected && !busy);
        pauseButton.setText(state.isPaused() ? "ПРОДОЛЖИТЬ" : "ПАУЗА");
        pauseButton.setEnabled(connected && !busy && !stopped && !transientState);
        stopButton.setEnabled(connected && !busy && !stopped);
        restartButton.setEnabled(connected && !busy && !stopped && !transientState);
    }

    private void setDisconnected() {
        connected = false;
        connectionPill.setText("Нет связи");
        setBg(connectionPill, RED, 999, 0, 0);
        nowLabel.setText("НЕТ СВЯЗИ");
        nowName.setText("Подключитесь к Heart-Simulator");
        nowDetail.setText("Нажмите красную кнопку сверху, чтобы открыть настройки Wi‑Fi");
        for (Button b : trackButtons.values()) b.setEnabled(false);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        restartButton.setEnabled(false);
        volumeSeek.setEnabled(false);
    }

    private void styleTrackButton(Button button, boolean active, boolean paused) {
        int bg;
        int stroke;
        if (active) {
            bg = paused ? Color.rgb(255, 246, 219) : Color.rgb(232, 248, 238);
            stroke = paused ? AMBER : GREEN;
        } else if (((Integer) button.getTag()) == 1) {
            bg = Color.rgb(238, 249, 241);
            stroke = Color.rgb(194, 228, 202);
        } else {
            bg = Color.rgb(244, 241, 246);
            stroke = LINE;
        }
        setBg(button, bg, 13, active ? 2 : 1, stroke);
        button.setTextColor(INK);
        button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(14), dp(16), dp(14));
        setBg(layout, CARD, 18, 1, LINE);
        return layout;
    }

    private TextView text(String value, int sp, int style, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private Button bigButton(String value, int bg, int fg) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(value);
        button.setTextSize(14);
        button.setTextColor(fg);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        setBg(button, bg, 14, 0, 0);
        return button;
    }

    private View space(int width, int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return view;
    }

    private LinearLayout.LayoutParams marginTop(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private void setBg(View view, int color, float radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        view.setBackground(drawable);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
