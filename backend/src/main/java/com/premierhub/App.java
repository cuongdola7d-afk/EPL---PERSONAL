package com.premierhub;

import com.premierhub.csv.ClubCsvReader;
import com.premierhub.csv.MatchCsvReader;
import com.premierhub.csv.PlayerCsvReader;
import com.premierhub.model.Club;
import com.premierhub.model.Player;
import com.premierhub.model.Standing;
import com.premierhub.service.PremierHubService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Path dataDirectory = args.length == 0 ? Path.of("data") : Path.of(args[0]);
        try {
            runDemo(dataDirectory, System.out);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Could not load PremierHub data: " + exception.getMessage());
        }
    }

    static void runDemo(Path dataDirectory, PrintStream out) throws IOException {
        var clubs = new ClubCsvReader().read(dataDirectory.resolve("clubs.csv"));
        var players = new PlayerCsvReader().read(dataDirectory.resolve("players.csv"));
        var matches = new MatchCsvReader().read(dataDirectory.resolve("matches.csv"));
        PremierHubService service = new PremierHubService(clubs, players, matches);

        out.println("=== Clubs ===");
        service.getClubs().forEach(club -> out.println(formatClub(club)));

        out.println();
        out.println("=== Search clubs: manchester ===");
        service.findClubsByName("manchester")
                .forEach(club -> out.println(formatClub(club)));

        out.println();
        out.println("=== Top scorers ===");
        service.getTopScorers(5).forEach(player -> out.println(formatPlayer(player)));

        out.println();
        out.println("=== League table ===");
        out.printf("%-3s %-22s %3s %3s %3s %3s %3s %3s %3s %3s%n",
                "#", "Club", "P", "W", "D", "L", "GF", "GA", "GD", "Pts");
        List<Standing> table = service.getLeagueTable();
        for (int index = 0; index < table.size(); index++) {
            out.println(formatStanding(index + 1, table.get(index)));
        }
    }

    public static String formatClub(Club club) {
        return club.getId() + " | " + club.getName() + " | " + club.getCity();
    }

    public static String formatPlayer(Player player) {
        return player.getName() + " | goals: " + player.getGoals()
                + " | assists: " + player.getAssists();
    }

    public static String formatStanding(int position, Standing standing) {
        return String.format("%-3d %-22s %3d %3d %3d %3d %3d %3d %+3d %3d",
                position,
                standing.getClub().getName(),
                standing.getPlayed(),
                standing.getWins(),
                standing.getDraws(),
                standing.getLosses(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDifference(),
                standing.getPoints());
    }
}
