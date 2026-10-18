import os, sys, time
from playwright.sync_api import sync_playwright
from pathlib import Path

OUT_DIR = Path("C:/MES-APPLICATIONS/joprelys-connect/docs/teaser")
OUT_DIR.mkdir(parents=True, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    # 1920x1080 standard Full HD
    page = browser.new_page(viewport={"width": 1920, "height": 1080}, device_scale_factor=1)
    print("Navigating to https://joprelys.com ...")
    page.goto("https://joprelys.com", wait_until="networkidle")
    page.wait_for_timeout(2000)

    # 1. Hero: Let's slightly hide overflow or zoom to make the hero section fill the 1920x1080 screen cleanly without showing half of the next section
    # Let's see the height of the hero section
    hero_rect = page.evaluate("""() => {
        const hero = document.querySelector('.landing-hero-section') || document.querySelector('section');
        const stats = document.querySelector('.landing-stats-grid') || document.querySelectorAll('.grid')[0];
        return {
            heroHeight: hero ? hero.offsetHeight : 0,
            statsBottom: stats ? stats.getBoundingClientRect().bottom + window.pageYOffset : 0
        };
    }""")
    print("Hero rect:", hero_rect)

    # Clean capture of Hero
    page.screenshot(path=str(OUT_DIR / "1080_01_hero.png"))
    print("Captured 1080_01_hero.png")

    # 2. Challenges & Comparison section
    page.evaluate("""() => {
        const el = document.getElementById('challenges-title') || document.querySelector('section:nth-of-type(2)');
        if (el) {
            const y = el.getBoundingClientRect().top + window.pageYOffset - 90;
            window.scrollTo({top: y, behavior: 'instant'});
        }
    }""")
    page.wait_for_timeout(1000)
    page.screenshot(path=str(OUT_DIR / "1080_02_challenges.png"))
    print("Captured 1080_02_challenges.png")

    # 3. Features: Assistant IA & Lits
    page.evaluate("""() => {
        const el = document.getElementById('features') || document.getElementById('features-title');
        if (el) {
            const y = el.getBoundingClientRect().top + window.pageYOffset - 90;
            window.scrollTo({top: y, behavior: 'instant'});
        }
    }""")
    page.wait_for_timeout(1000)
    page.screenshot(path=str(OUT_DIR / "1080_03_features.png"))
    print("Captured 1080_03_features.png")

    # 4. Ecosystem & AllôPharma
    page.evaluate("""() => {
        const el = document.getElementById('ecosystem') || document.getElementById('ecosystem-title');
        if (el) {
            const y = el.getBoundingClientRect().top + window.pageYOffset - 90;
            window.scrollTo({top: y, behavior: 'instant'});
        }
    }""")
    page.wait_for_timeout(1000)
    page.screenshot(path=str(OUT_DIR / "1080_04_ecosystem.png"))
    print("Captured 1080_04_ecosystem.png")

    # 5. Security & Sovereignty
    page.evaluate("""() => {
        const el = document.getElementById('security') || document.getElementById('security-title');
        if (el) {
            const y = el.getBoundingClientRect().top + window.pageYOffset - 90;
            window.scrollTo({top: y, behavior: 'instant'});
        }
    }""")
    page.wait_for_timeout(1000)
    page.screenshot(path=str(OUT_DIR / "1080_05_security.png"))
    print("Captured 1080_05_security.png")

    # 6. Contact / Demo / Pilot CTA
    # Let's find the demo section or footer CTA
    page.evaluate("""() => {
        const cta = document.querySelector('section:last-of-type') || document.querySelector('footer');
        if (cta) {
            const y = cta.getBoundingClientRect().top + window.pageYOffset - 120;
            window.scrollTo({top: y, behavior: 'instant'});
        }
    }""")
    page.wait_for_timeout(1000)
    page.screenshot(path=str(OUT_DIR / "1080_06_cta.png"))
    print("Captured 1080_06_cta.png")

    browser.close()
    print("All captures completed.")
