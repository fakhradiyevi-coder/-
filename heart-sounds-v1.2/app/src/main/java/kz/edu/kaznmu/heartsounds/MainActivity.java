package kz.edu.kaznmu.heartsounds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "HeartSounds";
    private static final int PURPLE = Color.rgb(139, 69, 128);
    private static final int PURPLE_DARK = Color.rgb(103, 51, 95);
    private static final int SURFACE = Color.rgb(255, 249, 253);
    private static final int TEXT = Color.rgb(37, 27, 36);
    private static final int MUTED = Color.rgb(108, 91, 105);
    private static final int GREEN = Color.rgb(33, 120, 75);

    private MediaPlayer player;
    private LoudnessEnhancer loudnessEnhancer;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private TextView nowPlaying;
    private TextView bluetoothStatus;
    private TextView routeStatus;
    private SeekBar volumeSeek;
    private Switch boostSwitch;
    private Button activeButton;
    private String currentTitle;
    private boolean pausedByFocus;

    private final SoundItem[] sounds = new SoundItem[]{
        new SoundItem("Взрослые записи", "Adult Case 1", "Нормальные тоны сердца", "Живая запись нормального сердечного цикла.", R.raw.adult_case_1_normal),
        new SoundItem("Взрослые записи", "Adult Case 2", "Невинный или функциональный шум", "Живая запись функционального сердечного шума.", R.raw.adult_case_2_innocent),
        new SoundItem("Взрослые записи", "Adult Case 3", "Митральный стеноз", "Основная запись митрального стеноза.", R.raw.adult_case_3_mitral_stenosis),
        new SoundItem("Взрослые записи", "Adult Case 3a", "Митральный стеноз — вариант A", "Дополнительная запись того же диагноза.", R.raw.adult_case_3a_mitral_stenosis),
        new SoundItem("Взрослые записи", "Adult Case 3b", "Митральный стеноз — вариант B", "Дополнительная запись того же диагноза.", R.raw.adult_case_3b_mitral_stenosis),
        new SoundItem("Взрослые записи", "Adult Case 3c", "Митральный стеноз — вариант C", "Дополнительная запись того же диагноза.", R.raw.adult_case_3c_mitral_stenosis),
        new SoundItem("Взрослые записи", "Adult Case 3d", "Митральный стеноз — вариант D", "Дополнительная запись того же диагноза.", R.raw.adult_case_3d_mitral_stenosis),
        new SoundItem("Взрослые записи", "Adult Case 4", "Двустворчатый аортальный клапан: стеноз и недостаточность", "Основная комбинированная запись аортального порока.", R.raw.adult_case_4_bicuspid_aortic_stenosis_insufficiency),
        new SoundItem("Взрослые записи", "Adult Case 4a", "Аортальный стеноз и недостаточность — вариант A", "Дополнительная запись комбинированного аортального порока.", R.raw.adult_case_4a_bicuspid_aortic_stenosis_insufficiency),
        new SoundItem("Взрослые записи", "Adult Case 4b", "Аортальный стеноз и недостаточность — вариант B", "Дополнительная запись комбинированного аортального порока.", R.raw.adult_case_4b_bicuspid_aortic_stenosis_insufficiency),
        new SoundItem("Взрослые записи", "Adult Case 4c", "Аортальный стеноз и недостаточность — вариант C", "Дополнительная запись комбинированного аортального порока.", R.raw.adult_case_4c_bicuspid_aortic_stenosis_insufficiency),
        new SoundItem("Взрослые записи", "Adult Case 5", "Дефект межжелудочковой перегородки", "Живая запись ДМЖП.", R.raw.adult_case_5_vsd),
        new SoundItem("Взрослые записи", "Adult Case 6", "Митральная недостаточность с пролапсом митрального клапана", "Основная живая запись сочетанного митрального поражения.", R.raw.adult_case_6_mitral_insufficiency_mvp),
        new SoundItem("Взрослые записи", "Adult Case 6a", "Митральная недостаточность с пролапсом — вариант A", "Дополнительная запись того же диагноза.", R.raw.adult_case_6a_mitral_insufficiency_mvp),
        new SoundItem("Взрослые записи", "Adult Case 7", "Открытый артериальный проток", "Живая запись непрерывного шума ОАП.", R.raw.adult_case_7_pda),

        new SoundItem("Врожденные пороки", "Congenital Case 1", "Дефект межпредсердной перегородки", "Первая живая запись ДМПП.", R.raw.child_case_1_asd),
        new SoundItem("Врожденные пороки", "Congenital Case 2", "Стеноз легочной артерии", "Живая запись стеноза легочной артерии.", R.raw.child_case_2_pulmonary_stenosis),
        new SoundItem("Врожденные пороки", "Congenital Case 3", "Открытый артериальный проток", "Педиатрическая запись ОАП.", R.raw.child_case_3_pda),
        new SoundItem("Врожденные пороки", "Congenital Case 4", "Аортальный стеноз и аортальная регургитация", "Живая комбинированная запись аортального порока.", R.raw.child_case_4_aortic_stenosis_regurgitation),
        new SoundItem("Врожденные пороки", "Congenital Case 5", "Нормальные тоны сердца у ребенка", "Живая запись нормальных тонов.", R.raw.child_case_5_normal),
        new SoundItem("Врожденные пороки", "Congenital Case 6", "Двустворчатый аортальный клапан", "Живая запись двустворчатого аортального клапана.", R.raw.child_case_6_bicuspid_aortic_valve),
        new SoundItem("Врожденные пороки", "Congenital Case 7", "Дефект межпредсердной перегородки — второй пример", "Вторая живая запись ДМПП.", R.raw.child_case_7_asd),
        new SoundItem("Врожденные пороки", "Congenital Case 8", "Невинный шум и третий тон S3", "Живая запись функционального шума с S3.", R.raw.child_case_8_innocent_s3),
        new SoundItem("Врожденные пороки", "Congenital Case 9", "Дефект межжелудочковой перегородки", "Педиатрическая запись ДМЖП.", R.raw.child_case_9_vsd)
    };

    private final AudioManager.OnAudioFocusChangeListener focusListener = focusChange -> {
        if (player == null) return;
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            runOnUiThread(this::stopPlayback);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (player.isPlaying()) {
                pausedByFocus = true;
                player.pause();
                runOnUiThread(() -> nowPlaying.setText("Пауза из-за другого звука телефона"));
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && pausedByFocus) {
            pausedByFocus = false;
            player.start();
            runOnUiThread(() -> nowPlaying.setText("Сейчас звучит: " + currentTitle));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setContentView(buildContent());
        setupVolume();
        updateBluetoothStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();
        syncVolume();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SURFACE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.university_logo);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(105)));

        TextView university = text("ASFENDIYAROV UNIVERSITY 1930", 17, PURPLE_DARK, true);
        university.setGravity(Gravity.CENTER);
        root.addView(university, margins(-1, -2, 0, 4, 0, 8));

        TextView title = text("Тренажер аускультации сердца", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, margins(-1, -2, 0, 0, 0, 8));

        TextView subtitle = text("Версия 1.2 • 24 реальные записи • автономная работа", 14, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, margins(-1, -2, 0, 0, 0, 18));

        LinearLayout connectionCard = card();
        root.addView(connectionCard, margins(-1, -2, 0, 0, 0, 16));

        bluetoothStatus = text("Проверка аудиовыхода…", 15, TEXT, true);
        connectionCard.addView(bluetoothStatus);
        TextView hint = text("Подключите колонку, затем нажмите кнопку проверки. Должны прозвучать четыре четких сигнала.", 13, MUTED, false);
        hint.setPadding(0, dp(5), 0, dp(10));
        connectionCard.addView(hint);

        Button bluetoothButton = secondaryButton("Открыть настройки Bluetooth");
        bluetoothButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            } catch (Exception exception) {
                Toast.makeText(this, "Откройте Bluetooth в настройках телефона", Toast.LENGTH_LONG).show();
            }
        });
        connectionCard.addView(bluetoothButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        Button testButton = primaryButton("ПРОВЕРКА ЗВУКА");
        testButton.setContentDescription("Проверка звука Bluetooth");
        testButton.setOnClickListener(v -> playTestSignal());
        connectionCard.addView(testButton, margins(-1, dp(52), 0, 10, 0, 0));

        LinearLayout playerCard = new LinearLayout(this);
        playerCard.setOrientation(LinearLayout.VERTICAL);
        playerCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        playerCard.setBackground(rounded(Color.rgb(246, 235, 244), dp(16), Color.TRANSPARENT, 0));
        root.addView(playerCard, margins(-1, -2, 0, 0, 0, 18));

        nowPlaying = text("Сначала выполните проверку звука", 16, PURPLE_DARK, true);
        playerCard.addView(nowPlaying);
        routeStatus = text("Аудиовыход определяется системой Android", 13, MUTED, false);
        routeStatus.setPadding(0, dp(4), 0, dp(7));
        playerCard.addView(routeStatus);

        boostSwitch = new Switch(this);
        boostSwitch.setText("Усиление для Bluetooth-колонки (+8 дБ)");
        boostSwitch.setTextColor(TEXT);
        boostSwitch.setTextSize(14);
        boostSwitch.setChecked(true);
        boostSwitch.setOnCheckedChangeListener((buttonView, checked) -> applyLoudness());
        playerCard.addView(boostSwitch);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(0, dp(10), 0, 0);
        Button pauseButton = secondaryButton("Пауза / продолжить");
        pauseButton.setOnClickListener(v -> togglePause());
        Button stopButton = secondaryButton("Стоп");
        stopButton.setOnClickListener(v -> stopPlayback());
        controls.addView(pauseButton, new LinearLayout.LayoutParams(0, dp(44), 1f));
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0, dp(44), 0.55f);
        stopParams.setMargins(dp(8), 0, 0, 0);
        controls.addView(stopButton, stopParams);
        playerCard.addView(controls);

        TextView volumeLabel = text("Громкость мультимедиа", 13, MUTED, false);
        volumeLabel.setPadding(0, dp(9), 0, 0);
        playerCard.addView(volumeLabel);
        volumeSeek = new SeekBar(this);
        playerCard.addView(volumeSeek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        String previousSection = "";
        for (SoundItem item : sounds) {
            if (!item.section.equals(previousSection)) {
                root.addView(sectionHeading(item.section), margins(-1, -2, 0, previousSection.isEmpty() ? 0 : 12, 0, 10));
                previousSection = item.section;
            }
            root.addView(createSoundCard(item), margins(-1, -2, 0, 0, 0, 12));
        }

        LinearLayout sourceCard = card();
        sourceCard.addView(text("Источник записей", 16, TEXT, true));
        TextView source = text("John P. Finley. Teaching Heart Auscultation to Health Professionals. teachingheartauscultation.com. Записи воспроизведены с указанием источника. В приложении используются только диагнозы, которые реально представлены на странице загрузки.", 12, MUTED, false);
        source.setPadding(0, dp(6), 0, 0);
        sourceCard.addView(source);
        root.addView(sourceCard, margins(-1, -2, 0, 4, 0, 10));

        TextView note = text("Учебное приложение. Не использовать для клинической диагностики.", 12, MUTED, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note);
        return scroll;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(Color.WHITE, dp(16), Color.rgb(231, 214, 226), dp(1)));
        return card;
    }

    private TextView sectionHeading(String label) {
        return text(label, 20, TEXT, true);
    }

    private View createSoundCard(SoundItem item) {
        LinearLayout card = card();
        card.addView(text(item.diagnosis, 17, TEXT, true));
        TextView caseLabel = text(item.caseLabel, 13, PURPLE_DARK, true);
        caseLabel.setPadding(0, dp(3), 0, dp(6));
        card.addView(caseLabel);
        card.addView(text(item.description, 13, MUTED, false));

        Button playButton = primaryButton("Воспроизвести с повтором");
        playButton.setContentDescription("Воспроизвести: " + item.caseLabel);
        playButton.setOnClickListener(v -> playSound(item, playButton));
        card.addView(playButton, margins(-1, dp(48), 0, 12, 0, 0));
        return card;
    }

    private void playTestSignal() {
        int maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int minimum = Math.max(1, Math.round(maximum * 0.5f));
        if (current < minimum) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(maximum * 0.65f), 0);
            syncVolume();
            Toast.makeText(this, "Громкость установлена на 65% для проверки", Toast.LENGTH_LONG).show();
        }
        playResource(R.raw.test_output, false, null, "Проверка звука: четыре сигнала", true);
    }

    private void playSound(SoundItem item, Button button) {
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            Toast.makeText(this, "Громкость мультимедиа равна нулю", Toast.LENGTH_LONG).show();
            return;
        }
        playResource(item.rawResource, true, button, item.diagnosis + " — " + item.caseLabel, false);
    }

    private void playResource(int rawResource, boolean looping, Button button, String title, boolean testSignal) {
        releasePlayer();
        requestAudioFocus();
        currentTitle = title;

        try {
            MediaPlayer newPlayer = new MediaPlayer();
            newPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());

            try (AssetFileDescriptor descriptor = getResources().openRawResourceFd(rawResource)) {
                if (descriptor == null) throw new IllegalStateException("Аудиофайл не найден");
                newPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            }

            AudioDeviceInfo bluetooth = findBluetoothOutput();
            if (bluetooth != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                newPlayer.setPreferredDevice(bluetooth);
            }

            newPlayer.setLooping(looping);
            newPlayer.setVolume(1f, 1f);
            newPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MEDIA_ERROR what=" + what + " extra=" + extra + " title=" + title);
                Toast.makeText(this, "Ошибка воспроизведения аудиофайла", Toast.LENGTH_LONG).show();
                return false;
            });
            newPlayer.setOnCompletionListener(mp -> {
                if (testSignal) nowPlaying.setText("Проверка завершена. Выберите запись.");
                releasePlayer();
            });
            newPlayer.prepare();
            player = newPlayer;

            if (!testSignal) {
                loudnessEnhancer = new LoudnessEnhancer(player.getAudioSessionId());
                applyLoudness();
            }

            activeButton = button;
            if (activeButton != null) activeButton.setText("Сейчас воспроизводится");
            player.start();

            String route = bluetooth == null ? "системный аудиовыход" : bluetooth.getProductName().toString();
            routeStatus.setText("Аудиовыход: " + route);
            nowPlaying.setText(testSignal ? title : "Сейчас звучит: " + title);
            Log.i(TAG, "PLAY_START|title=" + title + "|resource=" + rawResource + "|route=" + route);
        } catch (Exception exception) {
            Log.e(TAG, "PLAYBACK_EXCEPTION title=" + title, exception);
            releasePlayer();
            nowPlaying.setText("Не удалось воспроизвести: " + title);
            Toast.makeText(this, "Не удалось открыть запись", Toast.LENGTH_LONG).show();
        }
    }

    private void applyLoudness() {
        if (loudnessEnhancer == null) return;
        try {
            loudnessEnhancer.setTargetGain(boostSwitch != null && boostSwitch.isChecked() ? 800 : 0);
            loudnessEnhancer.setEnabled(boostSwitch != null && boostSwitch.isChecked());
        } catch (Exception exception) {
            Log.w(TAG, "LOUDNESS_ERROR", exception);
        }
    }

    private void togglePause() {
        if (player == null) {
            Toast.makeText(this, "Сначала выберите запись", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            nowPlaying.setText("Пауза: " + currentTitle);
        } else {
            player.start();
            nowPlaying.setText("Сейчас звучит: " + currentTitle);
        }
    }

    private void stopPlayback() {
        releasePlayer();
        if (nowPlaying != null) nowPlaying.setText("Воспроизведение остановлено");
    }

    private void releasePlayer() {
        if (loudnessEnhancer != null) {
            try { loudnessEnhancer.release(); } catch (Exception ignored) { }
            loudnessEnhancer = null;
        }
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.reset(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
        if (activeButton != null) {
            activeButton.setText("Воспроизвести с повтором");
            activeButton = null;
        }
        pausedByFocus = false;
        abandonAudioFocus();
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setOnAudioFocusChangeListener(focusListener)
                .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
            focusRequest = null;
        } else {
            audioManager.abandonAudioFocus(focusListener);
        }
    }

    private void setupVolume() {
        int maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volumeSeek.setMax(maximum);
        syncVolume();
        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void syncVolume() {
        if (volumeSeek != null && audioManager != null) {
            volumeSeek.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }

    private AudioDeviceInfo findBluetoothOutput() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || audioManager == null) return null;
        AudioDeviceInfo fallback = null;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return device;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (type == AudioDeviceInfo.TYPE_BLE_SPEAKER || type == AudioDeviceInfo.TYPE_BLE_HEADSET)) return device;
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) fallback = device;
        }
        return fallback;
    }

    private void updateBluetoothStatus() {
        if (bluetoothStatus == null) return;
        AudioDeviceInfo bluetooth = findBluetoothOutput();
        if (bluetooth == null) {
            bluetoothStatus.setText("Bluetooth-колонка не обнаружена");
            bluetoothStatus.setTextColor(PURPLE_DARK);
            if (routeStatus != null && player == null) routeStatus.setText("Аудиовыход: динамик телефона");
        } else {
            String name = bluetooth.getProductName().toString();
            bluetoothStatus.setText("Bluetooth подключен: " + name);
            bluetoothStatus.setTextColor(GREEN);
            if (routeStatus != null && player == null) routeStatus.setText("Аудиовыход: " + name);
        }
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(PURPLE, dp(12), Color.TRANSPARENT, 0));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(PURPLE_DARK);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(Color.WHITE, dp(11), Color.rgb(214, 187, 207), dp(1)));
        return button;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        int resolvedWidth = width == -1 ? ViewGroup.LayoutParams.MATCH_PARENT : width;
        int resolvedHeight = height == -2 ? ViewGroup.LayoutParams.WRAP_CONTENT : height;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(resolvedWidth, resolvedHeight);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SoundItem {
        final String section;
        final String caseLabel;
        final String diagnosis;
        final String description;
        final int rawResource;

        SoundItem(String section, String caseLabel, String diagnosis, String description, int rawResource) {
            this.section = section;
            this.caseLabel = caseLabel;
            this.diagnosis = diagnosis;
            this.description = description;
            this.rawResource = rawResource;
        }
    }
}
