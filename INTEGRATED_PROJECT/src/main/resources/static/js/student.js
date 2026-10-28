// ==========================================
// PANDEMIC ESCAPE - STUDENT GAME
// EASY → MEDIUM → HARD
// 3 LIVES
// ==========================================

let lives = 3;

let solvedQuestions = 0;

let lastPuzzleTitle = "";
let lastPuzzleContent = "";

// ==========================================
// START GAME
// ==========================================

document.addEventListener("DOMContentLoaded", () => {

    updateLives();

    const buttons =
        document.querySelectorAll(".door-card .secondary-button");

    buttons.forEach((button, index) => {

        button.addEventListener("click", () => {

            if (index === 0) {

                // EASY
                showPuzzle(
                    "LABORATORY - EASY",
                    `
                    <h2>🔬 QUESTION 1 — EASY</h2>

                    <p>
                        Which equipment is used to see
                        very small microorganisms?
                    </p>

                    <div class="mcq-options">

                        <button class="mcq-option"
                            onclick="submitPuzzle('laboratory','A')">
                            A. Microscope
                        </button>

                        <button class="mcq-option"
                            onclick="submitPuzzle('laboratory','B')">
                            B. Keyboard
                        </button>

                        <button class="mcq-option"
                            onclick="submitPuzzle('laboratory','C')">
                            C. Printer
                        </button>

                        <button class="mcq-option"
                            onclick="submitPuzzle('laboratory','D')">
                            D. Calculator
                        </button>

                    </div>

                    <p id="puzzleFeedback"></p>
                    `
                );

            } else if (index === 1) {

                // MEDIUM
                showPuzzle(
                    "COMPUTER ROOM - MEDIUM",
                    `
                    <h2>💻 QUESTION 2 — MEDIUM</h2>

                    <p>
                        The laboratory recorded these values:
                    </p>

                    <div class="puzzle-note">
                        <p><strong>Sample A → 3</strong></p>
                        <p><strong>Sample B → 7</strong></p>
                        <p><strong>Sample C → 2</strong></p>
                    </div>

                    <p>
                        Which sample has the highest recorded value?
                    </p>

                    <div class="mcq-options">

                        <button class="mcq-option"
                            onclick="submitPuzzle('computer','A')">
                            A. Sample A
                        </button>

                        <button class="mcq-option"
                            onclick="submitPuzzle('computer','B')">
                            B. Sample B
                        </button>

                        <button class="mcq-option"
                            onclick="submitPuzzle('computer','C')">
                            C. Sample C
                        </button>

                        <button class="mcq-option"
                            onclick="submitPuzzle('computer','D')">
                            D. All are equal
                        </button>

                    </div>

                    <p id="puzzleFeedback"></p>
                    `
                );

            } else {
                // ==================================
// QUESTION 3 - HARD
// ==================================

showPuzzle(
    "SECURITY EXIT - HARD",
    `
    <h2>🔐 QUESTION 3 — HARD</h2>

    <p>
        The laboratory security system shows four
        possible access codes.
    </p>

    <div class="puzzle-note">
        <p><strong>Clue 1:</strong> The code has 3 digits.</p>
        <p><strong>Clue 2:</strong> The first digit is 4.</p>
        <p><strong>Clue 3:</strong> The second digit is 7.</p>
        <p><strong>Clue 4:</strong> The final digit is 2.</p>
    </div>

    <p>
        Which code should you enter to unlock
        the emergency exit?
    </p>

    <div class="mcq-options">

        <button class="mcq-option"
            onclick="submitPuzzle('security','A')">
            A. 472
        </button>

        <button class="mcq-option"
            onclick="submitPuzzle('security','B')">
            B. 427
        </button>

        <button class="mcq-option"
            onclick="submitPuzzle('security','C')">
            C. 742
        </button>

        <button class="mcq-option"
            onclick="submitPuzzle('security','D')">
            D. 247
        </button>

    </div>

    <p id="puzzleFeedback"></p>
    `
);
               
            }

        });

    });

});


// ==========================================
// LIVES
// ==========================================

function updateLives() {

    const livesElement =
        document.getElementById("lives");

    if (!livesElement) {
        return;
    }

    let hearts = "";

    for (let i = 0; i < 3; i++) {

        if (i < lives) {
            hearts += "❤️ ";
        } else {
            hearts += "🖤 ";
        }

    }

    livesElement.innerHTML =
        hearts + "<br>LIVES: " + lives;
}


// ==========================================
// WRONG ANSWER
// ==========================================

function loseLife() {

    lives--;

    updateLives();

    if (lives <= 0) {

        closePuzzle();

        setTimeout(() => {

            missionFailed();

        }, 300);

    }

}


// ==========================================
// SHOW PUZZLE
// ==========================================
function showPuzzle(title, content) {

    // Remember the current question
    // so it can be shown again with RETRY
    lastPuzzleTitle = title;
    lastPuzzleContent = content;

    let modal =
        document.getElementById("puzzleModal");

    if (!modal) {

        modal = document.createElement("div");

        modal.id = "puzzleModal";

        document.body.appendChild(modal);
    }

    modal.innerHTML = `
        <div class="puzzle-modal-box">

            <button
                class="close-puzzle"
                onclick="closePuzzle()">
                ✕
            </button>

            <div class="puzzle-content">

                <div class="system-status">
                    PUZZLE ACCESS: ${title}
                </div>

                ${content}

            </div>

        </div>
    `;

    modal.classList.add("active");
}



// ==========================================
// CLOSE PUZZLE
// ==========================================

function closePuzzle() {

    const modal =
        document.getElementById("puzzleModal");

    if (modal) {
        modal.classList.remove("active");
    }

}


// ==========================================
// SUBMIT ANSWER
// ==========================================

function submitPuzzle(puzzleType, answer) {

    const feedback =
        document.getElementById("puzzleFeedback");

    let endpoint = "";

    if (puzzleType === "laboratory") {

        endpoint =
            "/api/student/laboratory";

    } else if (puzzleType === "computer") {

        endpoint =
            "/api/student/computer";

    } else if (puzzleType === "security") {

        endpoint =
            "/api/student/security";
    }


    fetch(endpoint, {

        method: "POST",

        headers: {
            "Content-Type":
                "application/x-www-form-urlencoded"
        },

        body:
            "answer=" +
            encodeURIComponent(answer)

    })

    .then(response => {

        if (!response.ok) {
            throw new Error("Server error");
        }

        return response.json();

    })

    .then(data => {

        if (data.correct) {
            playCorrectSound();
            feedback.innerHTML = `
                ✓ ${data.message}
            `;

            feedback.className =
                "correct-feedback";


            // Disable all options
            const options =
                document.querySelectorAll(".mcq-option");

            options.forEach(option => {
                option.disabled = true;
            });


            solvedQuestions++;


            // Question 3 completed
            if (puzzleType === "security") {

                setTimeout(() => {

                    closePuzzle();

                    missionCompleted();

                }, 1200);

            } else {

                setTimeout(() => {

                    closePuzzle();

                }, 1200);

            }

        } else {

            playWrongSound();
            // ==============================
            // WRONG ANSWER
            // ==============================

            lives--;

            updateLives();


            // Disable options
            const options =
                document.querySelectorAll(".mcq-option");

            options.forEach(option => {
                option.disabled = true;
            });


            // NO LIVES LEFT
            if (lives <= 0) {

                feedback.innerHTML = `
                    <div class="wrong-feedback">
                        ❌ WRONG ANSWER
                        <br><br>
                        You have lost all your lives.
                        <br><br>
                        <strong>MISSION FAILED</strong>
                    </div>
                `;

                setTimeout(() => {

                    closePuzzle();

                    missionFailed();

                }, 1500);

                return;
            }


            // STILL HAVE LIVES
            feedback.innerHTML = `
                <div class="wrong-feedback">

                    ❌ WRONG ANSWER

                    <br><br>

                    You lost 1 life.

                    <br>

                    ❤️ ${lives} LIFE${lives === 1 ? "" : "S"}

                    <br><br>

                    <button
                        class="primary-button retry-button"
                        onclick="retryStudentPuzzle()">
                        ↻ RETRY
                    </button>

                </div>
            `;

            feedback.className = "";
        }

    })

    .catch(error => {

        console.error(error);

        feedback.textContent =
            "Unable to connect to the Java backend.";

        feedback.className =
            "wrong-feedback";

    });
}
function retryStudentPuzzle() {

    showPuzzle(
        lastPuzzleTitle,
        lastPuzzleContent
    );

}