package dk.dtu.compute.se.pisd.roborally.online.controller;

import dk.dtu.compute.se.pisd.roborally.controller.AppController;
import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.Board;
import dk.dtu.compute.se.pisd.roborally.online.model.*;
import dk.dtu.compute.se.pisd.roborally.online.view.AppDialogs;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OnlineController {

    private static final int MIN_USERNAME_LENGTH = 4;

    public final AppController appController;

    public final OnlineState onlineState;

    private AppDialogs appDialogs;

    /**
     * The root URL of the backend for all the REST services.
     */
    public final String ROBORALLY_BACKEND_URL = "http://localhost:8080/roborally/";

    /**
     * The RestClient that can be used throughout all functions of this OnlineController
     * to communicate with the RoboRally backend.
     */
    private RestClient restClient;

    /**
     * Constructs an OnlineController using the given AppController.
     * Initializes a new OnlineState, builds a RestClient configured with
     * ROBORALLY_BACKEND_URL, and creates the AppDialogs helper.
     *
     * @param appController the application controller used by this OnlineController
     */
    public OnlineController(AppController appController) {
        this.appController = appController;
        this.onlineState = new OnlineState();
        restClient = RestClient.builder().
                baseUrl(ROBORALLY_BACKEND_URL).
                build();
        this.appDialogs = new AppDialogs(this);
    }

    /**
     * Sign in a user by name. If name length >= MIN_USERNAME_LENGTH the backend
     * is queried. The first returned user, if any, is set via {@link #setOnlineUser(User)}.
     *
     * @param name the username to sign in
     * @throws RuntimeException if the REST call fails
     */
    public void signIn(String name) {
        // DONE the 4 below is a bit arbitray and should be a constant defines
        //       somewhere in the code or a configuration file!!
        if (name.length() >= MIN_USERNAME_LENGTH) {
            try {
                List<User> users = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                        .path("/user/search")
                        .queryParam ("name", name)
                        .build())
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                if (!users.isEmpty()) {
                    setOnlineUser(users.get(0));
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException("exception happened in signIn(String name) in OnlineController.java");
            }

            // DONE Assignment 7b: make sure that the user with the given name
            //      exist in the backend; and make sure that you set the user
            //      returened by the backend (with the correct uid) is added
            //      as onLineUser in this controller! (NOT the once created
            //      in the code below!)
        }
    }

    /**
     * Opens the sign-in dialog unless a game is running or game selection is active.
     */
    public void signIn() {
        if (appController.isGameRunning()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game running");
            alert.setHeaderText("You cannot sign in while a game is running!");
            alert.showAndWait();
        } else if (gameSelectionOn) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game selection is active");
            alert.setHeaderText(
                    "You cannot sign in while a game selection " +
                    "for a signed in user is active!");
            alert.showAndWait();
        } else {
            appDialogs.signIn();
        }
    }

    /**
     * Sign out the current online user.
     * If a user is signed in a confirmation dialog is shown. On OK calls {@link #setOnlineUser(User)} with null.
     */
    public void signOut() {
        if (onlineState.getSignedInUser() != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Sign out?");
            alert.setContentText(
                    "Are you sure you want to sign out, " + onlineState.getSignedInUser().getName() + "?");
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // If the user confirms, sign the user out
                setOnlineUser(null);
            }
        }
    }

    // DONE Assignment 7c: you might want to implement a method of signing up
    //      (registering) a new user here!

    /**
     * Register a new user by Posting to {@code /user}. Validates that {@code name}
     * is at least 4 characters, sends the request and on success calls
     * {@link #setOnlineUser(User)} with the created user. Shows an error alert if
     * registration fails or a warning if the name is too short.
     *
     * @param name the chosen username (must be 4+ characters)
     */
    public void signUp(String name) {
        if (name.length() >= MIN_USERNAME_LENGTH) {
            try {
                User newUser = new User();
                newUser.setName(name);

                User createdUser = restClient.post()
                        .uri("/user") // Make sure this matches your UserController!
                        .body(newUser)
                        .retrieve()
                        .body(User.class);

                if (createdUser != null) {
                    setOnlineUser(createdUser);
                }

            } catch (Exception e) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("SignUp Failed");
                alert.setHeaderText("Could not create user");
                alert.setContentText("That username is likely already taken. Please try another one.");
                alert.showAndWait();
            }
        } else {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Invalid Name");
            alert.setHeaderText("Name too short");
            alert.setContentText("Your username must be at least " + MIN_USERNAME_LENGTH + " characters long.");
            alert.showAndWait();
        }
    }

    /**
     * Open the sign-up dialog unless a game is running or game selection is active.
     */
    public void signUp() {
        if (appController.isGameRunning()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game running");
            alert.setHeaderText("You cannot sign up while a game is running!");
            alert.showAndWait();
        } else if (gameSelectionOn) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game selection is active");
            alert.setHeaderText("You cannot sign up while game selection is active!");
            alert.showAndWait();
        } else {
            appDialogs.signUp(); // Opens the dialog you just created
        }
    }

    /**
     * Set the current online user and show an info dialog (welcome or logged out),
     * unless a game is running or game selection is active.
     *
     * @param user the user to set as signed in
     */
    public void setOnlineUser(User user) {
        if (!appController.isGameRunning() && !gameSelectionOn) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (user != null) {
                alert.setTitle("User logged in!");
                alert.setHeaderText("Welcome " + user.getName());
            } else {
                alert.setTitle("Logged out!");
                alert.setHeaderText("You are logged out!");
            }
            onlineState.setSignedInUser(user);
            alert.showAndWait();
        }
    }

    /**
     * Load the list of open games from the backend and store it in OnlineState.
     */
    public void refreshGames() {
        try {
            // DONE Assignment 7b: Obtain the list of all games from the backend!
            // DONE Assignment 7c/7e: And at some later point, this should only
            //      return the games open for registration (not started yet).
            List<Game> games = restClient.get().uri("/game/open").retrieve().body(new ParameterizedTypeReference<>() {});
            onlineState.setOpenGames(games);
        } catch (Exception e) {
            onlineState.setOpenGames(null);
        }
    }

    private boolean gameSelectionOn = false;

    /**
     * Show the game selection view if the user is signed in and no game is running.
     */
    public void selectGame() {
        if (appController.isGameRunning()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game running");
            alert.setHeaderText("You cannot select a game while a game is running!");
            alert.showAndWait();
        } else if (onlineState.getSignedInUser() == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Not signed in");
            alert.setHeaderText(
                    "You cannot select a game when not signed in!\n" +
                    "Sign in first!");
            alert.showAndWait();
        } else {
            refreshGames();
            gameSelectionOn = true;
            appController.roboRally.createGameSelectionView(this);
        }
    }

    /**
     * Close the game selection view and start the selected game if one was chosen.
     */
    public void gameSelected(Game game) {
        if (!appController.isGameRunning() /* && onlineState.getSignedInUser() != null && gameSelectionOn */) {
            appController.roboRally.createGameSelectionView(null);
            gameSelectionOn = false;

            Game result = null;
            if (game != null) {

                // DONE Assignment 7e: make sure the game is set to the active state
                //      here and in the backend, so that no new players can sign up..
                try {
                    Game stubGame = new Game();
                    stubGame.setUid(game.getUid());
                    stubGame.setState(GameState.ACTIVE);

                    restClient.patch()
                            .uri("/game/{id}", game.getUid())
                            .body(stubGame)
                            .retrieve()
                            .toBodilessEntity();

                } catch (Exception e) {
                    System.out.println("Failed to start the game on the server: " + e.getMessage());
                    return;
                }

                // Then show the game board and the game (with uid from backend) is then started
                startGame(game);
            }
        }
    }

    /**
     * Create a new game in the backend and then refresh the game list.
     */
    public void createGame(Game game) {
        if (!appController.isGameRunning() && onlineState.getSignedInUser() != null && gameSelectionOn) {


            try {
                // DONE Assignment 7b: Create the game (in the backend) with the config information
                //      provided in the game configuration
                // DONE Assignment 7c: Extend the game creation so that the currently signed in user
                //      is the owner of the game, which should also be registered as the first
                //      player of the game

                // Use a stub for the owner to prevent JSON circular reference errors
                User stubOwner = new User();
                stubOwner.setUid(onlineState.getSignedInUser().getUid());
                game.setOwner(stubOwner);
                restClient.post().uri("/game").body(game).retrieve().body(Game.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // update the game select view (which should get the new game from the backend)
            selectGame();
        }
    }

    /**
     * Open the dialog for creating a new game.
     */
    public void createGame() {
        appDialogs.createNewGame();
    }

    /**
     * Add the current user to the given game if there is room and the user is not already in it.
     */
    public void joinGame(Game game) {
        try {

            // DONE Assignment 7c: add the currently active user as a Player for
            //      the given game if this user is not a player yet and if there
            //      is still room for a player. If so post his to the backend,
            //      and check whether this was successfull

            User activeUser = onlineState.getSignedInUser();

            // check how many players are currently in the game
            int currentPlayersCount = game.getPlayers() != null ? game.getPlayers().size() : 0;

            // Check the join conditions
            if (activeUser != null && !userInGame(game) && currentPlayersCount < game.getMaxPlayers()) {

                // Create lightweight "stubs" to avoid sending massive circular JSON to the backend, this can cause an,
                // error so a user cant join a game
                Game stubGame = new Game();
                stubGame.setUid(game.getUid());

                User stubUser = new User();
                stubUser.setUid(activeUser.getUid());


                // create the player
                Player newPlayer = new Player();
                newPlayer.setGame(stubGame);
                newPlayer.setUser(stubUser);
                newPlayer.setName(activeUser.getName());

                restClient.post()
                        .uri("/player") // Ensure this matches your backend controller
                        .body(newPlayer)
                        .retrieve()
                        .toBodilessEntity();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

    /**
     * Remove the current user from the given game.
     */
    public void leaveGame(Game game) {
        try {
            // DONE Assignment 7d: delete the currently active user as a player
            //      for the given game (in the backend)
            User activeUser = onlineState.getSignedInUser();
            long uid;
            for (Player cPlayer: game.getPlayers()) {
                if (cPlayer.getUser().getUid() == activeUser.getUid()){
                    uid = cPlayer.getUid();
                    restClient.delete().uri("/player/{id}",uid).retrieve().toBodilessEntity();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

    /**
     * Delete the given game from the backend.
     */
    public void deleteGame(Game game) {
        try {

            // DONE Assignment 7d: delete the given game from the games
            //      in the backend
            long uid = game.getUid();
            restClient.delete().uri("/game/{id}",uid).retrieve().toBodilessEntity();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

    /**
     * Check whether the current user is a player in the given game.
     */
    public boolean userInGame(Game game) {

        // DONE Assignment 7c: this method should return true if the
        //      currently active user is a player of the game
        User currentUser = onlineState.getSignedInUser();

        // safety checks
        if (currentUser == null || game.getPlayers() == null) {
            return false;
        }

        for (Player currentPlayer: game.getPlayers()) {
            if (currentPlayer.getUser().getUid() == currentUser.getUid()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check whether the current user owns the given game.
     */
    public boolean userOwnsGame(Game game) {

        // DONE Assignment 7c: this method should return true
        //      if the currently active user the owner of the given game
        User currentUser = onlineState.getSignedInUser();

        // safety checks
        if (currentUser == null || game.getOwner() == null) {
            return false;
        }

        if (game.getOwner().getUid() == currentUser.getUid()) {
            return true;
        }

        return false;
    }

    /**
     * Create the local game board and start the programming phase for the selected game.
     */
    private void startGame(Game game) {
        // OPTIONAL(didnt do it) Assignment 7e: creation of the board should eventually depend
        //      on the board provided by the Game information.
        //      And every user who had joined the game should be able to start
        //      it in their client (individually -- no interactive gameplay
        //      required for Assignment 7)!
        Board board = new Board(8,8);
        GameController gameController = new GameController(board);
        int i = 0;
        for (Player player: game.getPlayers()) {
            String name = player.getName();
            if (name == null) {
                name = "Player " + (i + 1);
            }
            dk.dtu.compute.se.pisd.roborally.model.Player p =
                    new dk.dtu.compute.se.pisd.roborally.model.Player(board, appController.PLAYER_COLOURS.get(i), name);
            board.addPlayer(p);
            p.setSpace(board.getSpace(i % board.width, i));
            i++;
        }

        gameSelectionOn = false;

        gameController.startProgrammingPhase();
        appController.roboRally.createBoardView(gameController);
    }


}
