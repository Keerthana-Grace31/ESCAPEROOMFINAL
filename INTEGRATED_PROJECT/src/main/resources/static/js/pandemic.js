let timeLeft = 600;
let timerInterval = null;


function updateTimer() {

    const timer =
        document.getElementById("timer");

    if (!timer) {
        return;
    }

    const minutes =
        Math.floor(timeLeft / 60);

    const seconds =
        timeLeft % 60;

    timer.textContent =
        String(minutes).padStart(2, "0")
        + ":"
        + String(seconds).padStart(2, "0");


    timer.classList.remove(
        "warning",
        "critical"
    );


    if (timeLeft <= 120 &&
        timeLeft > 30) {

        timer.classList.add("warning");
    }


    if (timeLeft <= 30) {

        timer.classList.add("critical");
    }
}


function startTimer() {

    updateTimer();

    timerInterval =
        setInterval(() => {

            timeLeft--;

            updateTimer();

            if (timeLeft <= 0) {

                clearInterval(timerInterval);

                missionFailed();
            }

        }, 1000);
}


function missionFailed() {
    playFailureSound();

    const result =
        document.getElementById("missionResult");

    if (result) {

        result.classList.add("active");

        result.innerHTML = `

            <div class="result-box result-failure">

                <h1>MISSION FAILED</h1>

                <p>
                    The quarantine lockdown has become permanent.
                </p>

                <br>

                <button
                    class="primary-button"
                    onclick="location.reload()">

                    TRY AGAIN

                </button>

            </div>

        `;
    }
}


function missionCompleted() {
    playSuccessSound();

    clearInterval(timerInterval);

    const result =
        document.getElementById("missionResult");

    if (result) {

        result.classList.add("active");

        result.innerHTML = `

            <div class="result-box result-success">

                <h1>MISSION COMPLETE</h1>

                <p>
                    You successfully escaped the facility.
                </p>

                <br>

                <button
                    class="primary-button"
                    onclick="goToMainMenu()">

                    MAIN MENU

                </button>

            </div>

        `;
    }
}


document.addEventListener(
    "DOMContentLoaded",
    () => {

        if (document.getElementById("timer")) {
            startTimer();
        }

    }
);