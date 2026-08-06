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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "HeartSounds";
    private static final int PURPLE = Color.rgb(139, 69, 128);
    private static final int PURPLE_DARK = Color.rgb(103, 51, 95);
    private static final int SURFACE = Color.rgb(255, 249, 253);
    private static final int TEXT = Color.rgb(37, 27, 36);
    private static final int MUTED = Color.rgb(108, 91, 105);
    private static final int GREEN = Color.rgb(33, 120, 75);

    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private Button activeButton;
    private TextView nowPlaying;
    private TextView bluetoothStatus;
    private TextView routeStatus;
    private SeekBar volumeSeek;
    private String currentTitle;
    private boolean pausedByFocus;

    private final SoundItem[] sounds = new SoundItem[]{
        new SoundItem("Нормальные тоны сердца", "S1–S2, синусовый ритм", "Нормальный сердечный цикл", "Все стандартные точки аускультации", "Базовая запись первого и второго тонов сердца.", R.raw.normal_s1_s2),
        new SoundItem("Аортальный стеноз", "Систолический шум изгнания", "Систола: crescendo–decrescendo", "II межреберье справа у края грудины; проведение на сонные артерии", "Грубый ромбовидный систолический шум.", R.raw.aortic_stenosis),
        new SoundItem("Аортальная недостаточность", "Ранний диастолический шум", "Диастола: decrescendo", "III–IV межреберье слева у края грудины", "Высокочастотный убывающий шум сразу после второго тона.", R.raw.aortic_regurgitation),
        new SoundItem("Митральный стеноз", "Щелчок открытия и диастолический рокот", "Диастола", "Верхушка сердца, положение на левом боку", "Щелчок открытия и низкочастотный диастолический рокот.", R.raw.mitral_stenosis),
        new SoundItem("Митральная недостаточность", "Голосистолический шум", "Вся систола", "Верхушка сердца; проведение в левую подмышечную область", "Равномерный пансистолический шум между первым и вторым тонами.", R.raw.mitral_regurgitation),
        new SoundItem("Пролапс митрального клапана", "Среднесистолический щелчок и поздний шум", "Поздняя систола", "Верхушка сердца", "Щелчок в середине систолы с последующим нарастающим шумом.", R.raw.mitral_valve_prolapse),
        new SoundItem("Стеноз лёгочной артерии", "Систолический шум изгнания", "Систола", "II межреберье слева у края грудины", "Систолический шум с щелчком изгнания.", R.raw.pulmonary_stenosis),
        new SoundItem("Недостаточность клапана лёгочной артерии", "Ранний диастолический шум", "Диастола", "II–III межреберье слева у края грудины", "Убывающий раннедиастолический шум в лёгочной точке.", R.raw.pulmonary_regurgitation),
        new SoundItem("Трикуспидальная недостаточность", "Голосистолический шум", "Вся систола", "Нижний левый край грудины; усиливается на вдохе", "Пансистолический шум в зоне трикуспидального клапана.", R.raw.tricuspid_regurgitation),
        new SoundItem("Дефект межжелудочковой перегородки", "Грубый голосистолический шум", "Вся систола", "III–IV межреберье слева у края грудины", "Интенсивный высокочастотный пансистолический шум.", R.raw.ventricular_septal_defect),
        new SoundItem("Дефект межпредсердной перегородки", "Фиксированное расщепление S2", "Систола и второй тон", "II межреберье слева у края грудины", "Систолический потоковый шум и устойчивое расщепление второго тона.", R.raw.atrial_septal_defect),
        new SoundItem("Открытый артериальный проток", "Непрерывный «машинный» шум", "Систола и диастола", "Левая подключичная область / II межреберье слева", "Непрерывный шум, проходящий через второй тон.", R.raw.patent_ductus_arteriosus),
        new SoundItem("Тетрада Фалло", "Грубый систолический шум", "Систола", "Левый край грудины", "Систолический шум обструкции выходного тракта правого желудочка.", R.raw.tetralogy_of_fallot),
        new SoundItem("Третий тон сердца — S3", "Протодиастолический дополнительный тон", "Ранняя диастола", "Верхушка сердца", "Дополнительный третий тон вскоре после S2.", R.raw.s3_gallop),
        new SoundItem("Четвёртый тон сердца — S4", "Пресистолический дополнительный тон", "Конец диастолы", "Верхушка сердца", "Дополнительный четвёртый тон непосредственно перед S1.", R.raw.s4_gallop)
    };

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        if (player == null) {
            return;
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            Log.i(TAG, "AUDIO_FOCUS_LOSS");
            runOnUiThread(this::stopPlayback);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (player.isPlaying()) {
                pausedByFocus = true;
                player.pause();
                runOnUiThread(() -> nowPlaying.setText("Пауза из-за другого звука телефона"));
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && pausedByFocus) {
            pausedByFocus = false;
            player.setVolume(1.0f, 1.0f);
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
        syncVolumeSlider();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            updateBluetoothStatus();
            syncVolumeSlider();
        }
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

        TextView title = text("Тренажёр аускультации сердца", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, margins(-1, -2, 0, 0, 0, 8));

        TextView subtitle = text("Версия 1.1 • 15 усиленных учебных звуков • Bluetooth", 14, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, margins(-1, -2, 0, 0, 0, 18));

        LinearLayout bluetoothCard = new LinearLayout(this);
        bluetoothCard.setOrientation(LinearLayout.VERTICAL);
        bluetoothCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        bluetoothCard.setBackground(rounded(Color.WHITE, dp(16), Color.rgb(231, 214, 226), dp(1)));
        root.addView(bluetoothCard, margins(-1, -2, 0, 0, 0, 16));

        bluetoothStatus = text("Проверка аудиовыхода…", 15, TEXT, true);
        bluetoothCard.addView(bluetoothStatus);

        TextView bluetoothHint = text("Подключите колонку в настройках телефона, затем обязательно нажмите «ПРОВЕРКА ЗВУКА». Должны прозвучать четыре чётких сигнала.", 13, MUTED, false);
        bluetoothHint.setPadding(0, dp(5), 0, dp(12));
        bluetoothCard.addView(bluetoothHint);

        Button settingsButton = secondaryButton("Открыть настройки Bluetooth");
        settingsButton.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            } catch (Exception exception) {
                Toast.makeText(this, "Откройте Bluetooth в настройках телефона", Toast.LENGTH_LONG).show();
            }
        });
        bluetoothCard.addView(settingsButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        Button testButton = primaryButton("ПРОВЕРКА ЗВУКА");
        testButton.setContentDescription("Проверка звука Bluetooth");
        testButton.setOnClickListener(view -> playTestSignal());
        bluetoothCard.addView(testButton, margins(-1, dp(52), 0, 10, 0, 0));

        LinearLayout playerCard = new LinearLayout(this);
        playerCard.setOrientation(LinearLayout.VERTICAL);
        playerCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        playerCard.setBackground(rounded(Color.rgb(246, 235, 244), dp(16), Color.TRANSPARENT, 0));
        root.addView(playerCard, margins(-1, -2, 0, 0, 0, 18));

        nowPlaying = text("Сначала выполните проверку звука", 16, PURPLE_DARK, true);
        playerCard.addView(nowPlaying);
        routeStatus = text("Аудиовыход: определяется системой", 13, MUTED, false);
        routeStatus.setPadding(0, dp(4), 0, 0);
        playerCard.addView(routeStatus);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(0, dp(12), 0, 0);

        Button pauseButton = secondaryButton("Пауза / продолжить");
        pauseButton.setOnClickListener(view -> togglePause());
        Button stopButton = secondaryButton("Стоп");
        stopButton.setOnClickListener(view -> stopPlayback());
        controls.addView(pauseButton, new LinearLayout.LayoutParams(0, dp(44), 1.0f));
        LinearLayout.LayoutParams stopLayout = new LinearLayout.LayoutParams(0, dp(44), 0.55f);
        stopLayout.setMargins(dp(8), 0, 0, 0);
        controls.addView(stopButton, stopLayout);
        playerCard.addView(controls);

        TextView volumeLabel = text("Громкость мультимедиа", 13, MUTED, false);
        volumeLabel.setPadding(0, dp(10), 0, 0);
        playerCard.addView(volumeLabel);
        volumeSeek = new SeekBar(this);
        playerCard.addView(volumeSeek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        TextView listTitle = text("Звуки и пороки сердца", 20, TEXT, true);
        root.addView(listTitle, margins(-1, -2, 0, 0, 0, 10));

        for (SoundItem sound : sounds) {
            root.addView(createSoundCard(sound), margins(-1, -2, 0, 0, 0, 12));
        }

        TextView note = text("Учебная синтетическая модель. Не использовать для клинической диагностики.", 12, MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(8), dp(8), 0);
        root.addView(note);
        return scroll;
    }

    private View createSoundCard(SoundItem sound) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(Color.WHITE, dp(16), Color.rgb(236, 222, 232), dp(1)));

        card.addView(text(sound.title, 17, TEXT, true));
        TextView subtitle = text(sound.subtitle, 14, PURPLE_DARK, true);
        subtitle.setPadding(0, dp(3), 0, dp(7));
        card.addView(subtitle);
        card.addView(text("Фаза: " + sound.phase, 13, MUTED, false));
        TextView point = text("Точка: " + sound.point, 13, MUTED, false);
        point.setPadding(0, dp(3), 0, dp(5));
        card.addView(point);
        card.addView(text(sound.description, 13, TEXT, false));

        Button playButton = primaryButton("Воспроизвести с повтором");
        playButton.setContentDescription("Воспроизвести: " + sound.title);
        playButton.setOnClickListener(view -> playSound(sound, playButton));
        card.addView(playButton, margins(-1, dp(48), 0, 12, 0, 0));
        return card;
    }

    private void playTestSignal() {
        ensureAudibleTestVolume();
        updateBluetoothStatus();
        playResource(
            R.raw.test_output,
            false,
            null,
            "Проверка звука: должны быть слышны четыре сигнала",
            true
        );
    }

    private void playSound(SoundItem sound, Button button) {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (currentVolume == 0) {
            Toast.makeText(this, "Громкость мультимедиа равна нулю. Сначала нажмите «ПРОВЕРКА ЗВУКА».", Toast.LENGTH_LONG).show();
            return;
        }
        playResource(sound.rawResource, true, button, sound.title, false);
    }

    private void playResource(int rawResource, boolean looping, Button button, String title, boolean testSignal) {
        releasePlayer();
        requestAudioFocusWithoutBlocking();
        currentTitle = title;

        try {
            MediaPlayer newPlayer = new MediaPlayer();
            newPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());

            try (AssetFileDescriptor descriptor = getResources().openRawResourceFd(rawResource)) {
                if (descriptor == null) {
                    throw new IllegalStateException("Raw audio resource is compressed or missing: " + rawResource);
                }
                newPlayer.setDataSource(
                    descriptor.getFileDescriptor(),
                    descriptor.getStartOffset(),
                    descriptor.getLength()
                );
            }

            AudioDeviceInfo bluetoothOutput = findBluetoothOutput();
            if (bluetoothOutput != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean preferred = newPlayer.setPreferredDevice(bluetoothOutput);
                Log.i(TAG, "PREFERRED_DEVICE|" + bluetoothOutput.getProductName() + "|accepted=" + preferred);
            }

            newPlayer.setLooping(looping);
            newPlayer.setVolume(1.0f, 1.0f);
            newPlayer.setOnErrorListener((mediaPlayer, what, extra) -> {
                Log.e(TAG, "MEDIA_ERROR|title=" + title + "|what=" + what + "|extra=" + extra);
                runOnUiThread(() -> Toast.makeText(this, "Ошибка воспроизведения звука", Toast.LENGTH_LONG).show());
                return false;
            });
            newPlayer.setOnCompletionListener(mediaPlayer -> {
                if (testSignal) {
                    Log.i(TAG, "TEST_COMPLETE");
                    runOnUiThread(() -> nowPlaying.setText("Проверка завершена. Теперь выберите патологию."));
                }
                releasePlayer();
            });
            newPlayer.prepare();
            player = newPlayer;
            activeButton = button;
            if (activeButton != null) {
                activeButton.setText("Сейчас воспроизводится");
            }
            player.start();

            String route = bluetoothOutput == null ? "системный аудиовыход" : bluetoothOutput.getProductName().toString();
            nowPlaying.setText(testSignal ? title : "Сейчас звучит: " + title);
            routeStatus.setText("Аудиовыход: " + route);
            Log.i(TAG, "PLAY_START|title=" + title + "|resource=" + rawResource + "|loop=" + looping + "|route=" + route);
        } catch (Exception exception) {
            Log.e(TAG, "PLAYBACK_EXCEPTION|title=" + title, exception);
            releasePlayer();
            nowPlaying.setText("Не удалось воспроизвести: " + title);
            Toast.makeText(this, "Не удалось открыть аудиофайл: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void togglePause() {
        if (player == null) {
            Toast.makeText(this, "Сначала выберите звук", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            nowPlaying.setText("Пауза: " + currentTitle);
            Log.i(TAG, "PLAY_PAUSE|title=" + currentTitle);
        } else {
            player.start();
            nowPlaying.setText("Сейчас звучит: " + currentTitle);
            Log.i(TAG, "PLAY_RESUME|title=" + currentTitle);
        }
    }

    private void stopPlayback() {
        String stoppedTitle = currentTitle;
        releasePlayer();
        nowPlaying.setText("Воспроизведение остановлено");
        if (stoppedTitle != null) {
            Log.i(TAG, "PLAY_STOP|title=" + stoppedTitle);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            try {
                player.reset();
            } catch (Exception ignored) {
            }
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

    private void requestAudioFocusWithoutBlocking() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build();
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            );
        }
        Log.i(TAG, "AUDIO_FOCUS_REQUEST|result=" + result);
    }

    private void abandonAudioFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
            focusRequest = null;
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
    }

    private void ensureAudibleTestVolume() {
        int maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int minimumForTest = Math.max(1, Math.round(maximum * 0.45f));
        if (current < minimumForTest) {
            int target = Math.max(minimumForTest, Math.round(maximum * 0.60f));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            if (volumeSeek != null) {
                volumeSeek.setProgress(target);
            }
            Toast.makeText(this, "Громкость мультимедиа установлена на 60% для проверки", Toast.LENGTH_LONG).show();
        }
    }

    private void setupVolume() {
        int maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeek.setMax(maximum);
        volumeSeek.setProgress(current);
        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    Log.i(TAG, "VOLUME_CHANGE|" + progress + "/" + maximum);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void syncVolumeSlider() {
        if (volumeSeek != null && audioManager != null) {
            volumeSeek.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }

    private AudioDeviceInfo findBluetoothOutput() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || audioManager == null) {
            return null;
        }

        AudioDeviceInfo fallback = null;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                return device;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && (type == AudioDeviceInfo.TYPE_BLE_SPEAKER || type == AudioDeviceInfo.TYPE_BLE_HEADSET)) {
                return device;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && type == AudioDeviceInfo.TYPE_HEARING_AID) {
                fallback = device;
            } else if (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                fallback = device;
            }
        }
        return fallback;
    }

    private void updateBluetoothStatus() {
        if (bluetoothStatus == null) {
            return;
        }
        AudioDeviceInfo bluetoothOutput = findBluetoothOutput();
        if (bluetoothOutput == null) {
            bluetoothStatus.setText("Bluetooth-колонка не обнаружена");
            bluetoothStatus.setTextColor(PURPLE_DARK);
            if (routeStatus != null && player == null) {
                routeStatus.setText("Аудиовыход: динамик телефона");
            }
        } else {
            String productName = bluetoothOutput.getProductName().toString();
            bluetoothStatus.setText("Bluetooth подключён: " + productName);
            bluetoothStatus.setTextColor(GREEN);
            if (routeStatus != null && player == null) {
                routeStatus.setText("Аудиовыход: " + productName);
            }
        }
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setLineSpacing(0, 1.08f);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), 0, dp(12), 0);
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
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(rounded(Color.WHITE, dp(11), Color.rgb(214, 187, 207), dp(1)));
        return button;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, stroke);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        int resolvedWidth = width == -1 ? ViewGroup.LayoutParams.MATCH_PARENT : width;
        int resolvedHeight = height == -2 ? ViewGroup.LayoutParams.WRAP_CONTENT : height;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(resolvedWidth, resolvedHeight);
        layoutParams.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return layoutParams;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SoundItem {
        final String title;
        final String subtitle;
        final String phase;
        final String point;
        final String description;
        final int rawResource;

        SoundItem(String title, String subtitle, String phase, String point, String description, int rawResource) {
            this.title = title;
            this.subtitle = subtitle;
            this.phase = phase;
            this.point = point;
            this.description = description;
            this.rawResource = rawResource;
        }
    }
}
