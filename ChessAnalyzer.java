import java.io.*;
import java.util.*;

public class ChessAnalyzer {

    private Process stockfishProcess;
    private BufferedReader stockfishReader;
    private BufferedWriter stockfishWriter;

    public ChessAnalyzer(String stockfishPath) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(stockfishPath);
        builder.redirectErrorStream(true);
        stockfishProcess = builder.start();

        stockfishWriter = new BufferedWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()));
        stockfishReader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
    }

    // Send command to Stockfish and retrieve the response
    public String sendCommand(String command) throws IOException {
        stockfishWriter.write(command + "\n");
        stockfishWriter.flush();

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = stockfishReader.readLine()) != null) {
            response.append(line).append("\n");
            // Stop reading when we receive "readyok" or "bestmove"
            if (line.equals("readyok") || line.startsWith("bestmove")) {
                break;
            }
        }
        return response.toString();
    }

    public void stopStockfish() throws IOException {
        sendCommand("quit");
        stockfishWriter.close();
        stockfishReader.close();
        stockfishProcess.destroy();
    }

    // Load PGN and analyze the game move by move
    public void analyzePGNGame(String pgn) throws IOException {
        List<String> moves = extractMovesFromPGN(pgn);
        String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"; // Starting position

        // Set initial position
        sendCommand("position fen " + startFEN);

        for (String move : moves) {
            sendCommand("position fen " + startFEN + " moves " + move);
            String analysis = sendCommand("go depth 15");
            System.out.println("Move: " + move);
            categorizeMove(analysis);
            System.out.println("Analysis: " + analysis);
        }
    }

    // Helper method to extract the moves from a PGN string
    public List<String> extractMovesFromPGN(String pgn) {
        List<String> moves = new ArrayList<>();
        String[] tokens = pgn.split("\\s+");
        for (String token : tokens) {
            // A rough check to identify valid moves
            if (token.matches("^[a-h][1-8][a-h][1-8].*")) {
                moves.add(token);
            }
        }
        return moves;
    }

    // Categorize the move based on the Stockfish evaluation
    private void categorizeMove(String analysis) {
        String[] lines = analysis.split("\n");
        for (String line : lines) {
            if (line.startsWith("bestmove")) {
                // Extract the evaluation score
                String[] parts = line.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("score")) {
                        int score = Integer.parseInt(parts[i + 1]); // Get the score
                        System.out.println("Evaluation score: " + score);
                        System.out.println("Move categorization: " + categorizeBasedOnScore(score));
                    }
                }
            }
        }
    }

    // Categorize the score
    private String categorizeBasedOnScore(int score) {
        if (score >= 300) {
            return "Blunder";
        } else if (score >= 100) {
            return "Mistake";
        } else if (score >= 50) {
            return "Inaccuracy";
        } else {
            return "Good move";
        }
    }

    public static void main(String[] args) {
        try {
            // Update the path with your Stockfish binary location
            String stockfishPath = "Engines/stockfish.exe"; // e.g., "engines/stockfish" for local
            ChessAnalyzer analyzer = new ChessAnalyzer(stockfishPath);

            // Example PGN of a simple game
            String pgn = "[Event \"Casual Game\"]\n" +
                    "[White \"WhitePlayer\"]\n" +
                    "[Black \"BlackPlayer\"]\n" +
                    "1. e4 d6 2. d4 Nf6 3. Nc3 g6 4. Be3 Bg7 5. Qd2 c6 6. f3 b5 7. Nge2 Nbd7 8. Bh6\n" +
                    "Bxh6 9. Qxh6 Bb7 10. a3 e5 11. O-O-O Qe7 12. Kb1 a6 13. Nc1 O-O-O 14. Nb3 exd4\n" +
                    "15. Rxd4 c5 16. Rd1 Nb6 17. g3 Kb8 18. Na5 Ba8 19. Bh3 d5 20. Qf4+ Ka7 21. Rhe1\n" +
                    "d4 22. Nd5 Nbxd5 23. exd5 Qd6 24. Rxd4 cxd4 25. Re7+ Kb6 26. Qxd4+ Kxa5 27. b4+\n" +
                    "Ka4 28. Qc3 Qxd5 29. Ra7 Bb7 30. Rxb7 Qc4 31. Qxf6 Kxa3 32. Qxa6+ Kxb4 33. c3+\n" +
                    "Kxc3 34. Qa1+ Kd2 35. Qb2+ Kd1 36. Bf1 Rd2 37. Rd7 Rxd7 38. Bxc4 bxc4 39. Qxh8\n" +
                    "Rd3 40. Qa8 c3 4";

            analyzer.analyzePGNGame(pgn);
            analyzer.stopStockfish();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
