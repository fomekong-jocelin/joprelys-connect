import os, sys, io, asyncio, subprocess, math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import edge_tts
import imageio_ffmpeg

if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
WORKSPACE = Path("C:/MES-APPLICATIONS/joprelys-connect")
OUT_DIR = WORKSPACE / "video_build"
OUT_DIR.mkdir(parents=True, exist_ok=True)
SLIDES_DIR = WORKSPACE / "docs/teaser/clean_1080_no_header"

FINAL_VIDEO = WORKSPACE / "joprelys_connect_teaser.mp4"

WIDTH = 1920
HEIGHT = 1080
FPS = 25
CROSSFADE_FRAMES = 10  # 0.4s smooth dissolve

SCENES = [
    {
        "id": "intro",
        "image_file": SLIDES_DIR / "00_intro.png",
        "text": "Joprelys Connect. Parce qu’elle est précieuse, nous innovons pour la protéger.",
        "type": "intro"
    },
    {
        "id": "hero",
        "image_file": SLIDES_DIR / "01_hero.png",
        "text": "Découvrez la suite hospitalière unifiée conçue pour fluidifier chaque étape du parcours de soins dans nos cliniques et hôpitaux, du premier contact au suivi complet.",
        "type": "capture"
    },
    {
        "id": "challenges",
        "image_file": SLIDES_DIR / "02_challenges.png",
        "text": "Finis les dossiers cartonnés égarés et les pertes de temps en consultation. Avec Joprelys Connect, l’ensemble des informations médicales est unifié, sécurisé et accessible en un clic.",
        "type": "capture"
    },
    {
        "id": "features",
        "image_file": SLIDES_DIR / "03_features.png",
        "text": "Grâce à notre Assistant IA souverain et à la dictée vocale clinique, le praticien consacre son attention au patient. Le compte-rendu est structuré en temps réel, réduisant de quarante pour cent le temps de saisie.",
        "type": "capture"
    },
    {
        "id": "ecosystem",
        "image_file": SLIDES_DIR / "04_ecosystem.png",
        "text": "Une synergie inédite avec l’écosystème AllôPharma, permettant de localiser en direct les pharmacies de garde et la disponibilité immédiate des médicaments prescrits.",
        "type": "capture"
    },
    {
        "id": "security",
        "image_file": SLIDES_DIR / "05_security.png",
        "text": "La confidentialité est absolue : chiffrement de grade bancaire, piste d’audit inviolable et hébergement conforme aux standards les plus stricts de protection des données de santé.",
        "type": "capture"
    },
    {
        "id": "demo",
        "image_file": SLIDES_DIR / "06_demo.png",
        "text": "Rejoignez le programme pilote deux mille vingt-six. Équipez votre clinique en quarante-huit heures et transformez durablement la qualité de vos soins.",
        "type": "capture"
    },
    {
        "id": "outro",
        "image_file": SLIDES_DIR / "07_outro.png",
        "text": "Merci de nous avoir suivis ! Abonnez-vous à notre chaîne pour ne rien rater de nos prochaines vidéos et innovations. Rendez-vous sur joprelys point com pour demander votre démonstration. Joprelys Connect : la santé connectée en toute confiance.",
        "type": "outro"
    }
]

def get_audio_duration(file_path):
    cmd = [FFMPEG, "-i", str(file_path)]
    r = subprocess.run(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE, text=True, errors="replace")
    for line in r.stderr.splitlines():
        if "Duration:" in line:
            parts = line.split("Duration:")[1].split(",")[0].strip().split(":")
            dur = float(parts[0]) * 3600 + float(parts[1]) * 60 + float(parts[2])
            return dur
    return 8.0

async def prepare_voiceovers():
    print("=== Préparation des voix-off (fr-FR-HenriNeural)... ===")
    for scene in SCENES:
        out_mp3 = OUT_DIR / f"{scene['id']}.mp3"
        # Always re-generate outro to reflect new text
        if scene["id"] == "outro" and out_mp3.exists():
            out_mp3.unlink()
            
        if not out_mp3.exists() or out_mp3.stat().st_size == 0:
            print(f"  Génération TTS {scene['id']}...")
            comm = edge_tts.Communicate(scene["text"], "fr-FR-HenriNeural", rate="+3%", pitch="+0Hz")
            await comm.save(str(out_mp3))
            
        dur = get_audio_duration(out_mp3)
        scene["audio_file"] = out_mp3
        scene["audio_duration"] = dur
        # Add 1.2s breathing space after voice ends for clean cadence
        scene["duration"] = max(dur + 1.2, 5.0)
        print(f"  ✓ {scene['id']} : audio {dur:.2f}s -> durée plan {scene['duration']:.2f}s")

def generate_ambient_music(total_duration, output_wav):
    print(f"=== Génération de la piste musicale ({total_duration:.1f}s)... ===")
    sr = 44100
    t = np.linspace(0, total_duration, int(sr * total_duration), endpoint=False)
    
    # Warm chords progression: Am - F - C - G (6s per chord loop)
    chord_duration = 6.0
    chords = [
        [220.0, 261.63, 329.63, 440.0],  # Am
        [174.61, 220.0, 261.63, 349.23], # F
        [130.81, 196.0, 261.63, 329.63], # C
        [196.0, 246.94, 293.66, 392.0],  # G
    ]
    
    audio = np.zeros_like(t)
    num_chords = int(math.ceil(total_duration / chord_duration))
    
    for i in range(num_chords):
        chord_start = i * chord_duration
        idx_start = int(chord_start * sr)
        idx_end = min(int((chord_start + chord_duration) * sr), len(t))
        if idx_start >= len(t):
            break
        
        chord_t = t[idx_start:idx_end] - chord_start
        chord_len = (idx_end - idx_start) / sr
        
        env = np.ones_like(chord_t)
        attack_len = min(1.5, chord_len * 0.3)
        decay_len = min(1.5, chord_len * 0.3)
        attack_samples = int(attack_len * sr)
        decay_samples = int(decay_len * sr)
        
        if attack_samples > 0 and attack_samples < len(env):
            env[:attack_samples] = np.sin(np.linspace(0, np.pi/2, attack_samples))
        if decay_samples > 0 and decay_samples < len(env):
            env[-decay_samples:] = np.cos(np.linspace(0, np.pi/2, decay_samples))
            
        freqs = chords[i % len(chords)]
        chord_signal = np.zeros_like(chord_t)
        for f in freqs:
            chord_signal += 0.22 * np.sin(2 * np.pi * f * chord_t)
            chord_signal += 0.08 * np.sin(2 * np.pi * (f * 2) * chord_t)
            
        audio[idx_start:idx_end] += chord_signal * env
        
    # Chime ping every 3.5 seconds
    for ping_time in np.arange(1.0, total_duration - 1.0, 3.5):
        idx = int(ping_time * sr)
        ping_len = int(1.8 * sr)
        if idx + ping_len < len(t):
            pt = np.linspace(0, 1.8, ping_len, endpoint=False)
            penv = np.exp(-3.5 * pt)
            ping = 0.10 * np.sin(2 * np.pi * 1046.5 * pt) * penv # C6 chime
            audio[idx:idx + ping_len] += ping

    max_val = np.max(np.abs(audio))
    if max_val > 0:
        audio = (audio / max_val) * 0.15  # Subtle background volume
        
    fade_samples = int(2.0 * sr)
    if len(audio) > fade_samples * 2:
        audio[:fade_samples] *= np.linspace(0, 1, fade_samples)
        audio[-fade_samples:] *= np.linspace(1, 0, fade_samples)
        
    int16_audio = (audio * 32767).astype(np.int16)
    
    import wave
    with wave.open(str(output_wav), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sr)
        wf.writeframes(int16_audio.tobytes())
    print("  ✓ Musique générée.")

def build_soundtrack(total_duration):
    music_wav = OUT_DIR / "ambient_music.wav"
    generate_ambient_music(total_duration, music_wav)
    
    audio_inputs = []
    filter_chains = []
    current_offset = 0.0
    
    for i, s in enumerate(SCENES):
        audio_inputs.extend(["-i", str(s["audio_file"])])
        # Voiceover starts at offset + 0.35s
        delay_ms = int((current_offset + 0.35) * 1000)
        filter_chains.append(f"[{i}:a]adelay={delay_ms}|{delay_ms}[a{i}];")
        current_offset += s["duration"]
        
    concat_filter = "".join(filter_chains)
    inputs_merge = "".join([f"[a{i}]" for i in range(len(SCENES))])
    concat_filter += f"{inputs_merge}amix=inputs={len(SCENES)}:normalize=0[voice];"
    
    music_idx = len(SCENES)
    audio_inputs.extend(["-i", str(music_wav)])
    concat_filter += f"[voice][{music_idx}:a]amix=inputs=2:weights=1.0 0.22:duration=first[aout]"
    
    mixed_audio_file = OUT_DIR / "final_soundtrack.m4a"
    mix_cmd = [
        FFMPEG, "-y",
        *audio_inputs,
        "-filter_complex", concat_filter,
        "-map", "[aout]",
        "-c:a", "aac", "-b:a", "192k",
        str(mixed_audio_file)
    ]
    print("  Mixage audio avec FFmpeg...")
    subprocess.run(mix_cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    print("  ✓ Piste audio finale prête.")
    return mixed_audio_file

def render_intro_frame(bg_arr, logo_arr, logo_pos, slogan_img, f_idx, total_frames):
    """
    Smooth intro animation:
    0-22 frames (0.9s): logo fades in
    22-44 frames (0.9s): slogan fades in
    44+ frames: 100% visible
    """
    frame = bg_arr.copy()
    
    logo_progress = min(max(f_idx / 22.0, 0.0), 1.0)
    slogan_progress = min(max((f_idx - 22) / 22.0, 0.0), 1.0)
    
    lx, ly, lw, lh = logo_pos
    if logo_progress > 0:
        logo_rgb = logo_arr[:, :, :3]
        logo_a = (logo_arr[:, :, 3] / 255.0) * logo_progress
        logo_a = logo_a[:, :, np.newaxis]
        
        target_roi = frame[ly:ly+lh, lx:lx+lw]
        frame[ly:ly+lh, lx:lx+lw] = (target_roi * (1.0 - logo_a) + logo_rgb * logo_a).astype(np.uint8)
        
    if slogan_progress > 0:
        slogan_arr = np.array(slogan_img)
        s_rgb = slogan_arr[:, :, :3]
        s_a = (slogan_arr[:, :, 3] / 255.0) * slogan_progress
        s_a = s_a[:, :, np.newaxis]
        
        frame[:, :] = (frame * (1.0 - s_a) + s_rgb * s_a).astype(np.uint8)
        
    return frame

def render_outro_frame(outro_arr, f_idx, total_frames):
    """
    Outro with soft fade-to-black at the end (last 20 frames = 0.8s)
    """
    fade_out_start = total_frames - 20
    if f_idx >= fade_out_start:
        fade = 1.0 - (f_idx - fade_out_start) / 20.0
        return (outro_arr * fade).astype(np.uint8)
    return outro_arr

def render_video(mixed_audio_file, total_duration):
    print("=== Encodage de la vidéo Full HD 1080p (25 FPS)... ===")
    
    bg_img = Image.new("RGB", (WIDTH, HEIGHT), (248, 250, 252))
    draw_bg = ImageDraw.Draw(bg_img)
    for y in range(HEIGHT):
        r = int(255 - (255 - 241) * (y / HEIGHT))
        g = int(255 - (255 - 245) * (y / HEIGHT))
        b = int(255 - (255 - 249) * (y / HEIGHT))
        draw_bg.line([(0, y), (WIDTH, y)], fill=(r, g, b))
    bg_arr = np.array(bg_img)
    
    logo = Image.open(WORKSPACE / "web/src/assets/branding/logo_principal.png").convert("RGBA")
    target_w = 680
    target_h = int(logo.height * (target_w / logo.width))
    logo_resized = logo.resize((target_w, target_h), Image.Resampling.LANCZOS)
    logo_arr = np.array(logo_resized)
    logo_x = (WIDTH - target_w) // 2
    logo_y = (HEIGHT // 2) - target_h - 10
    logo_pos = (logo_x, logo_y, target_w, target_h)
    
    slogan_overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw_s = ImageDraw.Draw(slogan_overlay)
    font_path_b = os.path.join(os.environ["WINDIR"], "Fonts", "segoeuib.ttf")
    font_path_r = os.path.join(os.environ["WINDIR"], "Fonts", "segoeui.ttf")
    font_slogan = ImageFont.truetype(font_path_b, 44)
    font_sub = ImageFont.truetype(font_path_r, 26)
    
    sep_y = (HEIGHT // 2) + 75
    draw_s.line([(WIDTH // 2 - 90, sep_y), (WIDTH // 2 + 90, sep_y)], fill=(0, 151, 48, 255), width=5)
    
    slogan_text = "« Parce qu’elle est précieuse, nous innovons pour la protéger. »"
    bbox_s = draw_s.textbbox((0, 0), slogan_text, font=font_slogan)
    slogan_w = bbox_s[2] - bbox_s[0]
    draw_s.text(((WIDTH - slogan_w) // 2, sep_y + 35), slogan_text, fill=(0, 29, 74, 255), font=font_slogan)
    
    sub_text = "Suite Hospitalière Unifiée & Souveraine  •  Cameroun & Afrique"
    bbox_sub = draw_s.textbbox((0, 0), sub_text, font=font_sub)
    sub_w = bbox_sub[2] - bbox_sub[0]
    draw_s.text(((WIDTH - sub_w) // 2, sep_y + 105), sub_text, fill=(71, 85, 105, 255), font=font_sub)
    
    scene_arrays = []
    for scene in SCENES:
        img = Image.open(scene["image_file"]).convert("RGB")
        if img.size != (WIDTH, HEIGHT):
            img = img.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
        scene_arrays.append(np.array(img))
        
    video_cmd = [
        FFMPEG, "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{WIDTH}x{HEIGHT}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-",
        "-i", str(mixed_audio_file),
        "-c:v", "libx264",
        "-preset", "fast",
        "-crf", "18",
        "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        str(FINAL_VIDEO)
    ]
    
    proc = subprocess.Popen(video_cmd, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    total_rendered_frames = 0
    total_target_frames = sum(int(s["duration"] * FPS) for s in SCENES)
    
    for s_idx, scene in enumerate(SCENES):
        scene_frames = int(scene["duration"] * FPS)
        print(f"  Rendu plan {s_idx + 1}/{len(SCENES)} : {scene['id']} ({scene_frames} frames)...")
        current_arr = scene_arrays[s_idx]
        next_arr = scene_arrays[s_idx + 1] if s_idx + 1 < len(SCENES) else None
        
        for f_idx in range(scene_frames):
            if scene["type"] == "intro":
                frame_arr = render_intro_frame(bg_arr, logo_arr, logo_pos, slogan_overlay, f_idx, scene_frames)
            elif scene["type"] == "outro":
                frame_arr = render_outro_frame(current_arr, f_idx, scene_frames)
            else:
                frame_arr = current_arr
                
            if next_arr is not None and f_idx >= scene_frames - CROSSFADE_FRAMES:
                blend_t = (f_idx - (scene_frames - CROSSFADE_FRAMES)) / float(CROSSFADE_FRAMES)
                smooth_t = blend_t * blend_t * (3.0 - 2.0 * blend_t)
                frame_arr = (frame_arr.astype(np.float32) * (1.0 - smooth_t) + next_arr.astype(np.float32) * smooth_t).astype(np.uint8)
                
            proc.stdin.write(frame_arr.tobytes())
            total_rendered_frames += 1
            if total_rendered_frames % 50 == 0:
                pct = (total_rendered_frames / total_target_frames) * 100
                sys.stdout.write(f"\r  Progression : {pct:.1f}% ({total_rendered_frames}/{total_target_frames} frames)")
                sys.stdout.flush()
                
    proc.stdin.close()
    proc.wait()
    print(f"\n\n🎉 VIDÉO TEASER TERMINÉE AVEC SUCCÈS !")
    print(f"Fichier : {FINAL_VIDEO.resolve()}")
    print(f"Taille  : {FINAL_VIDEO.stat().st_size / (1024*1024):.2f} Mo")

async def main():
    await prepare_voiceovers()
    total_duration = sum(s["duration"] for s in SCENES)
    soundtrack_file = build_soundtrack(total_duration)
    render_video(soundtrack_file, total_duration)

if __name__ == "__main__":
    asyncio.run(main())
