// ==========================================
// DIGITAL ESCAPE ROOM - SOUND SYSTEM
// ==========================================

let soundEnabled = true;

// ==========================================
// AMBIENT SOUND
// ==========================================

// No pandemic ambient track has been supplied yet. Put a file at
// static/sounds/pandemic/ambient.mp3 and point AMBIENT_SRC at it (URL path /sounds/pandemic/ambient.mp3).
const AMBIENT_SRC = null;

const backgroundSound = AMBIENT_SRC ? new Audio(AMBIENT_SRC) : null;

if (backgroundSound) {
    backgroundSound.loop = true;
    backgroundSound.volume = 0.20;
}


// ==========================================
// PLAY SOUND FUNCTION
// ==========================================

function playSound(file, volume = 0.6) {

    if (!soundEnabled) {
        return;
    }

    // Levels + the SOUND toggle come from bgm.js; the per-call volumes below are only a fallback.
    if (window.BGM && window.BGM.sfxAllowed) {
        if (!window.BGM.sfxAllowed()) {
            return;
        }
        volume = window.BGM.sfxVolume;
    }

    const sound = new Audio(file);

    sound.volume = volume;

    sound.play().catch(error => {
        console.log("Sound waiting for user interaction.");
    });
}


// ==========================================
// CLICK SOUND
// ==========================================

function playClickSound() {

    playSound(
        "/sounds/common/click.mp3",
        0.6
    );
}


// ==========================================
// CORRECT SOUND
// ==========================================

function playCorrectSound() {

    playSound(
        "/sounds/common/correct.mp3",
        0.7
    );
}


// ==========================================
// WRONG SOUND
// ==========================================

function playWrongSound() {

    playSound(
        "/sounds/common/wrong.mp3",
        0.7
    );
}


// ==========================================
// SUCCESS SOUND
// ==========================================

function playSuccessSound() {

    playSound(
        "/sounds/common/success.mp3",
        0.8
    );
}


// ==========================================
// FAILURE SOUND
// ==========================================

function playFailureSound() {

    playSound(
        "/sounds/common/failure.mp3",
        0.8
    );
}


// ==========================================
// START AMBIENT SOUND
// ==========================================

function startBackgroundSound() {

    if (!soundEnabled || !backgroundSound) {
        return;
    }

    backgroundSound.play().catch(() => {
        console.log("Waiting for user interaction...");
    });
}


// ==========================================
// SOUND ON / OFF
// ==========================================
// The header SOUND button is a [data-bgm-toggle] handled by bgm.js (shared background music,
// localStorage "escaperoom.bgm.enabled"). Gameplay sounds (click / correct / wrong / success /
// failure) always play, as on every other scenario. toggleSound() is kept for old callers.

function toggleSound() {

    if (window.BGM) {
        window.BGM.toggle();
    }
}


// ==========================================
// HOME
// ==========================================

function goToMainMenu() {

    playClickSound();

    if (backgroundSound) {
        backgroundSound.pause();
    }

    window.location.href = "/";
}


// ==========================================
// BUTTON CLICK SOUND
// ==========================================

document.addEventListener("DOMContentLoaded", () => {

    const buttons =
        document.querySelectorAll("button");

    buttons.forEach(button => {

        if (
            !button.classList.contains("sound-button")
        ) {

            button.addEventListener("click", () => {

                playClickSound();

                // Start ambient after user interaction
                startBackgroundSound();

            });

        }

    });

});