import os, sys, time
from playwright.sync_api import sync_playwright
from pathlib import Path

OUT_DIR = Path("C:/MES-APPLICATIONS/joprelys-connect/docs/teaser/clean_1080")
OUT_DIR.mkdir(parents=True, exist_ok=True)

sections_to_capture = [
    {
        "name": "01_hero",
        "selector": ".landing-hero-section",
        "description": "Hero section"
    },
    {
        "name": "02_challenges",
        "selector": "main > section:nth-of-type(2)",
        "description": "Challenges and comparison"
    },
    {
        "name": "03_features",
        "selector": "#features",
        "description": "Key features"
    },
    {
        "name": "04_ecosystem",
        "selector": "#ecosystem",
        "description": "HealthTech ecosystem"
    },
    {
        "name": "05_security",
        "selector": "#security",
        "description": "Security and HDS"
    },
    {
        "name": "06_demo",
        "selector": "#contact-demo",
        "description": "Pilot & Demo form"
    }
]

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    for sec in sections_to_capture:
        page = browser.new_page(viewport={"width": 1920, "height": 1080}, device_scale_factor=1)
        page.goto("https://joprelys.com", wait_until="networkidle")
        page.wait_for_timeout(1000)
        
        # Isolate the target section and center it
        page.evaluate("""(targetSel) => {
            const header = document.querySelector('header');
            const target = document.querySelector(targetSel);
            if (!target) return;
            
            // Hide all children of main except target
            const main = document.querySelector('main');
            Array.from(main.children).forEach(child => {
                if (child !== target) {
                    child.style.display = 'none';
                }
            });
            
            // Hide footer
            const footer = document.querySelector('footer');
            if (footer) footer.style.display = 'none';
            
            // Center target in the remaining 1019px
            target.style.minHeight = 'calc(1080px - 61px)';
            target.style.display = 'flex';
            target.style.flexDirection = 'column';
            target.style.justifyContent = 'center';
            target.style.paddingTop = '1rem';
            target.style.paddingBottom = '1rem';
            window.scrollTo(0, 0);
        }""", sec["selector"])
        
        page.wait_for_timeout(500)
        out_path = OUT_DIR / f"{sec['name']}.png"
        page.screenshot(path=str(out_path))
        print(f"Captured {sec['name']} -> {out_path.name}")
        page.close()
        
    browser.close()
print("Done capturing isolated sections.")
