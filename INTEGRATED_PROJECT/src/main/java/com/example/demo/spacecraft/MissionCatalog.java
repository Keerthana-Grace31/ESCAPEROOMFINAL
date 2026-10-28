package com.example.demo.spacecraft;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/** All challenges for both missions: four per mission. */
@Component
public class MissionCatalog {

    private final Map<MissionType, List<Challenge>> challenges = new EnumMap<>(MissionType.class);

    public MissionCatalog() {
        challenges.put(MissionType.MARS, List.of(
            new Challenge(
                "Habitat Power Failure",
                List.of("A dust storm has buried the solar panels. Battery: 12%.",
                        "The reactor needs 40 minutes to restart.",
                        "Active systems: life support, heating, communications, science lab."),
                "Which system should you shut down first to save power?",
                List.of("Life support", "Science lab", "Heating", "Communications"),
                1,
                "Which system is not needed to keep the crew alive or reachable?",
                "The science lab is the only non-essential load. Life support, heating and comms keep you alive and reachable."),
            new Challenge(
                "Airlock Protocol",
                List.of("The airlock must never have both doors open at the same time.",
                        "You are inside the habitat and must go outside.",
                        "Steps: depressurize chamber, seal inner door, open outer door."),
                "What is the correct order?",
                List.of("Open outer, seal inner, depressurize",
                        "Depressurize, seal inner, open outer",
                        "Seal inner, depressurize, open outer",
                        "Seal inner, open outer, depressurize"),
                2,
                "Isolate the habitat first, then change the pressure.",
                "Seal the inner door first to protect the habitat, then depressurize, then open the outer door."),
            new Challenge(
                "Rover Range",
                List.of("The beacon is 12 km away. The rover moves at 3 km/h.",
                        "Oxygen supply: 5 hours.",
                        "Safety rule: keep at least 30 minutes of oxygen in reserve."),
                "What is your decision?",
                List.of("Go: arrive in 4 hours with 1 hour spare",
                        "Do not go: the trip takes 5 hours",
                        "Go: arrive in 3 hours with 2 hours spare",
                        "Do not go: it breaks the 30-minute reserve rule"),
                0,
                "Distance divided by speed gives travel time.",
                "12 km at 3 km/h takes 4 hours. With 5 hours of oxygen you keep 1 hour, above the 30-minute reserve."),
            new Challenge(
                "Launch Code",
                List.of("The ascent vehicle needs the next number in the sequence.",
                        "Sequence: 2, 6, 18, 54, ?"),
                "Enter the correct code.",
                List.of("108", "126", "162", "216"),
                2,
                "Look at how each number relates to the one before it.",
                "Each number is multiplied by 3, so 54 x 3 = 162.")
        ));

        challenges.put(MissionType.LUNAR, List.of(
            new Challenge(
                "Cargo Loader",
                List.of("A crate is labelled 120 kg of mass.",
                        "Lunar gravity is about one sixth of Earth's gravity.",
                        "The loader arm is rated for lifting weight, not mass."),
                "Which statement is correct on the Moon?",
                List.of("Mass is 20 kg and weight is unchanged",
                        "Mass is 120 kg and weight is about one sixth",
                        "Mass and weight are both one sixth",
                        "Mass and weight are both unchanged"),
                1,
                "Mass is the amount of matter. Gravity does not change it.",
                "Mass stays 120 kg anywhere. Weight depends on gravity, so on the Moon it is about one sixth."),
            new Challenge(
                "Earth Link",
                List.of("The Moon is about 384,000 km from Earth.",
                        "Radio waves travel at about 300,000 km/s.",
                        "You send a question and wait for mission control's reply."),
                "Roughly how long is the round-trip delay?",
                List.of("About 0.13 seconds", "About 1.3 seconds", "About 2.6 seconds", "About 13 seconds"),
                2,
                "Work out the one-way time, then double it.",
                "One way is about 1.28 s. There and back is about 2.6 s."),
            new Challenge(
                "Suit Oxygen",
                List.of("Your rover is wrecked 6 km from the base.",
                        "You walk at 3 km/h. Your suit has 150 minutes of oxygen.",
                        "No rescue is expected for at least 3 hours."),
                "What should you do?",
                List.of("Wait here for rescue",
                        "Start walking now: arrive with about 30 min spare",
                        "Rest for an hour, then walk",
                        "Walk at half speed to save energy"),
                1,
                "Compare the walking time with your oxygen time.",
                "6 km at 3 km/h is 120 minutes, leaving 30 minutes of oxygen. Waiting or slowing down runs you out."),
            new Challenge(
                "Crater Beacon",
                List.of("The base beacon unlocks with the next number in the sequence.",
                        "Sequence: 1, 1, 2, 3, 5, 8, ?"),
                "Enter the correct code.",
                List.of("11", "12", "13", "15"),
                2,
                "Each number is built from the two before it.",
                "This is the Fibonacci sequence: 5 + 8 = 13.")
        ));
    }

    public List<Challenge> forMission(MissionType type) {
        return challenges.get(type);
    }
}
