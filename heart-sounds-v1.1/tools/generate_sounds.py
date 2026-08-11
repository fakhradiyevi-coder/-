#!/usr/bin/env python3
"""Generate speaker-compatible educational heart sounds.

The first prototype concentrated much of its energy below the useful range of
small Bluetooth speakers. This version keeps the characteristic timing but
adds audible harmonics, normalises every recording independently and creates a
separate output-test signal.
"""

from __future__ import annotations

import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44_100
BPM = 75
PERIOD = 60.0 / BPM
DURATION = 16.0  # exactly 20 cardiac cycles, so loop boundaries are clean
SAMPLE_COUNT = int(SAMPLE_RATE * DURATION)
OUTPUT_DIR = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw"

SOUND_NAMES = [
    "normal_s1_s2",
    "aortic_stenosis",
    "aortic_regurgitation",
    "mitral_stenosis",
    "mitral_regurgitation",
    "mitral_valve_prolapse",
    "pulmonary_stenosis",
    "pulmonary_regurgitation",
    "tricuspid_regurgitation",
    "ventricular_septal_defect",
    "atrial_septal_defect",
    "patent_ductus_arteriosus",
    "tetralogy_of_fallot",
    "s3_gallop",
    "s4_gallop",
]


def gaussian_pulse(cycle_time: float, center: float, width: float, amplitude: float, partials: list[tuple[float, float]]) -> float:
    delta = cycle_time - center
    envelope = math.exp(-(delta * delta) / (2.0 * width * width))
    value = 0.0
    for frequency, weight in partials:
        value += weight * math.sin(2.0 * math.pi * frequency * delta)
    return amplitude * envelope * value


def raised_envelope(cycle_time: float, start: float, end: float, power: float = 0.7) -> float:
    if cycle_time < start or cycle_time > end:
        return 0.0
    x = (cycle_time - start) / (end - start)
    return max(0.0, math.sin(math.pi * x)) ** power


def decrescendo_envelope(cycle_time: float, start: float, end: float, power: float = 1.4) -> float:
    if cycle_time < start or cycle_time > end:
        return 0.0
    x = (cycle_time - start) / (end - start)
    opening = 0.3 + 0.7 * max(0.0, math.sin(math.pi * min(1.0, x * 1.4))) ** 0.4
    return ((1.0 - x) ** power) * opening


def generate_heart_sound(kind: str, seed: int) -> list[float]:
    rng = random.Random(seed)
    samples: list[float] = []
    low_pass_fast = 0.0
    low_pass_slow = 0.0

    for index in range(SAMPLE_COUNT):
        time_seconds = index / SAMPLE_RATE
        cycle_time = time_seconds % PERIOD

        white_noise = rng.uniform(-1.0, 1.0)
        low_pass_fast = 0.72 * low_pass_fast + 0.28 * white_noise
        low_pass_slow = 0.96 * low_pass_slow + 0.04 * white_noise
        band_noise = low_pass_fast - low_pass_slow

        # Heart tones include higher harmonics so they remain audible on small
        # Bluetooth speakers that strongly attenuate very low frequencies.
        s1 = gaussian_pulse(
            cycle_time,
            0.075,
            0.021,
            0.82,
            [(75.0, 0.65), (120.0, 0.28), (185.0, 0.14)],
        )
        s2 = gaussian_pulse(
            cycle_time,
            0.385,
            0.017,
            0.72,
            [(95.0, 0.58), (155.0, 0.28), (230.0, 0.14)],
        )
        value = s1 + s2

        if kind == "normal_s1_s2":
            value += 0.01 * math.sin(2.0 * math.pi * 120.0 * time_seconds)

        elif kind == "aortic_stenosis":
            envelope = raised_envelope(cycle_time, 0.115, 0.355, 0.60)
            value += envelope * (
                0.48 * band_noise
                + 0.26 * math.sin(2.0 * math.pi * 210.0 * time_seconds)
                + 0.18 * math.sin(2.0 * math.pi * 330.0 * time_seconds)
            )

        elif kind == "aortic_regurgitation":
            envelope = decrescendo_envelope(cycle_time, 0.420, 0.700, 1.60)
            value += envelope * (
                0.40 * band_noise
                + 0.28 * math.sin(2.0 * math.pi * 300.0 * time_seconds)
                + 0.16 * math.sin(2.0 * math.pi * 480.0 * time_seconds)
            )

        elif kind == "mitral_stenosis":
            value += gaussian_pulse(cycle_time, 0.470, 0.007, 0.58, [(260.0, 0.65), (430.0, 0.35)])
            envelope = raised_envelope(cycle_time, 0.500, 0.760, 0.55)
            envelope *= 0.72 + 0.28 * math.cos(2.0 * math.pi * (cycle_time - 0.500) / 0.260)
            value += envelope * (
                0.30 * band_noise
                + 0.25 * math.sin(2.0 * math.pi * 125.0 * time_seconds)
                + 0.14 * math.sin(2.0 * math.pi * 190.0 * time_seconds)
            )
            value += gaussian_pulse(cycle_time, 0.730, 0.028, 0.28, [(90.0, 0.70), (145.0, 0.30)])

        elif kind == "mitral_regurgitation":
            envelope = raised_envelope(cycle_time, 0.100, 0.385, 0.25)
            value += envelope * (
                0.42 * band_noise
                + 0.28 * math.sin(2.0 * math.pi * 230.0 * time_seconds)
                + 0.16 * math.sin(2.0 * math.pi * 390.0 * time_seconds)
            )

        elif kind == "mitral_valve_prolapse":
            value += gaussian_pulse(cycle_time, 0.235, 0.006, 0.65, [(390.0, 0.60), (620.0, 0.40)])
            envelope = raised_envelope(cycle_time, 0.255, 0.385, 0.55)
            value += envelope * (
                0.36 * band_noise
                + 0.24 * math.sin(2.0 * math.pi * 280.0 * time_seconds)
                + 0.14 * math.sin(2.0 * math.pi * 460.0 * time_seconds)
            )

        elif kind == "pulmonary_stenosis":
            value += gaussian_pulse(cycle_time, 0.105, 0.006, 0.38, [(320.0, 0.65), (520.0, 0.35)])
            envelope = raised_envelope(cycle_time, 0.125, 0.350, 0.62)
            value += envelope * (
                0.40 * band_noise
                + 0.25 * math.sin(2.0 * math.pi * 190.0 * time_seconds)
                + 0.16 * math.sin(2.0 * math.pi * 310.0 * time_seconds)
            )

        elif kind == "pulmonary_regurgitation":
            envelope = decrescendo_envelope(cycle_time, 0.430, 0.690, 1.45)
            value += envelope * (
                0.36 * band_noise
                + 0.25 * math.sin(2.0 * math.pi * 235.0 * time_seconds)
                + 0.14 * math.sin(2.0 * math.pi * 370.0 * time_seconds)
            )

        elif kind == "tricuspid_regurgitation":
            envelope = raised_envelope(cycle_time, 0.100, 0.390, 0.28)
            envelope *= 0.78 + 0.22 * math.sin(2.0 * math.pi * 0.35 * time_seconds)
            value += envelope * (
                0.40 * band_noise
                + 0.24 * math.sin(2.0 * math.pi * 200.0 * time_seconds)
                + 0.13 * math.sin(2.0 * math.pi * 340.0 * time_seconds)
            )

        elif kind == "ventricular_septal_defect":
            envelope = raised_envelope(cycle_time, 0.085, 0.405, 0.20)
            value += envelope * (
                0.52 * band_noise
                + 0.30 * math.sin(2.0 * math.pi * 310.0 * time_seconds)
                + 0.20 * math.sin(2.0 * math.pi * 520.0 * time_seconds)
            )

        elif kind == "atrial_septal_defect":
            envelope = raised_envelope(cycle_time, 0.130, 0.300, 0.75)
            value += envelope * (
                0.25 * band_noise
                + 0.17 * math.sin(2.0 * math.pi * 190.0 * time_seconds)
                + 0.10 * math.sin(2.0 * math.pi * 300.0 * time_seconds)
            )
            value -= 0.55 * s2
            value += gaussian_pulse(cycle_time, 0.372, 0.013, 0.48, [(100.0, 0.62), (160.0, 0.28), (230.0, 0.10)])
            value += gaussian_pulse(cycle_time, 0.430, 0.013, 0.46, [(90.0, 0.62), (150.0, 0.28), (220.0, 0.10)])

        elif kind == "patent_ductus_arteriosus":
            envelope = 0.35 + 0.65 * raised_envelope(cycle_time, 0.120, 0.700, 0.50)
            value += envelope * (
                0.32 * band_noise
                + 0.22 * math.sin(2.0 * math.pi * 220.0 * time_seconds)
                + 0.14 * math.sin(2.0 * math.pi * 360.0 * time_seconds)
            )

        elif kind == "tetralogy_of_fallot":
            envelope = raised_envelope(cycle_time, 0.105, 0.325, 0.45)
            value += envelope * (
                0.47 * band_noise
                + 0.27 * math.sin(2.0 * math.pi * 230.0 * time_seconds)
                + 0.15 * math.sin(2.0 * math.pi * 390.0 * time_seconds)
            )
            value -= 0.42 * s2

        elif kind == "s3_gallop":
            value += gaussian_pulse(cycle_time, 0.520, 0.025, 0.56, [(72.0, 0.58), (112.0, 0.29), (168.0, 0.13)])

        elif kind == "s4_gallop":
            value += gaussian_pulse(cycle_time, PERIOD - 0.065, 0.025, 0.56, [(70.0, 0.58), (110.0, 0.29), (165.0, 0.13)])

        else:
            raise ValueError(f"Unknown sound type: {kind}")

        samples.append(value)

    peak = max(abs(value) for value in samples)
    gain = 0.92 / max(peak, 1e-9)
    denominator = math.tanh(1.15)
    normalised = []
    for value in samples:
        value = math.tanh(1.15 * value * gain) / denominator
        normalised.append(max(-0.98, min(0.98, value)))
    return normalised


def generate_test_signal() -> list[float]:
    duration = 5.2
    total = int(SAMPLE_RATE * duration)
    result = []
    beep_windows = [
        (0.35, 1.05, 660.0),
        (1.35, 2.05, 880.0),
        (2.35, 3.05, 660.0),
        (3.35, 4.35, 880.0),
    ]
    for index in range(total):
        time_seconds = index / SAMPLE_RATE
        value = 0.0
        for start, end, frequency in beep_windows:
            if start <= time_seconds <= end:
                x = (time_seconds - start) / (end - start)
                envelope = min(1.0, x / 0.05, (1.0 - x) / 0.05)
                envelope = max(0.0, envelope)
                value = 0.72 * envelope * (
                    0.78 * math.sin(2.0 * math.pi * frequency * time_seconds)
                    + 0.22 * math.sin(2.0 * math.pi * frequency * 1.5 * time_seconds)
                )
                break
        result.append(value)
    return result


def write_wav(path: Path, samples: list[float]) -> tuple[float, float]:
    pcm = [int(max(-1.0, min(1.0, value)) * 32767.0) for value in samples]
    peak = max(abs(value) for value in samples)
    rms = math.sqrt(sum(value * value for value in samples) / len(samples))
    with wave.open(str(path), "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        wav_file.writeframes(struct.pack(f"<{len(pcm)}h", *pcm))
    return peak, rms


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    for index, name in enumerate(SOUND_NAMES):
        path = OUTPUT_DIR / f"{name}.wav"
        peak, rms = write_wav(path, generate_heart_sound(name, 1930 + index))
        if peak < 0.80 or rms < 0.12:
            raise RuntimeError(f"Audio validation failed for {name}: peak={peak:.3f}, rms={rms:.3f}")
        print(f"{name}: peak={peak:.3f}, rms={rms:.3f}, bytes={path.stat().st_size}")

    test_path = OUTPUT_DIR / "test_output.wav"
    peak, rms = write_wav(test_path, generate_test_signal())
    if peak < 0.60 or rms < 0.20:
        raise RuntimeError(f"Test signal validation failed: peak={peak:.3f}, rms={rms:.3f}")
    print(f"test_output: peak={peak:.3f}, rms={rms:.3f}, bytes={test_path.stat().st_size}")

    generated = list(OUTPUT_DIR.glob("*.wav"))
    if len(generated) != 16:
        raise RuntimeError(f"Expected 16 WAV files, found {len(generated)}")


if __name__ == "__main__":
    main()
