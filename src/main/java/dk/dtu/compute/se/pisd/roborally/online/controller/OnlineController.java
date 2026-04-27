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

    public OnlineController(AppController appController) {
        this.appController = appController;
        this.onlineState = new OnlineState();
        restClient = RestClient.builder().
                baseUrl(ROBORALLY_BACKEND_URL).
                build();
        this.appDialogs = new AppDialogs(this);
    }

    public void signIn(String name) {
        // FIXME the 4 below is a bit arbitray and should be a constant defines
        //       somewhere in the code or a configuration file!!
        if (name.length() >= 4) {
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
//            User user = new User();
//            user.setName(name);
//            setOnlineUser(user);
        }
    }

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
    public void signUp(String name) {
        if (name.length() >= 4) {
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
            alert.setContentText("Your username must be at least 4 characters long.");
            alert.showAndWait();
        }
    }

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

    public void refreshGames() {
        try {
            // DONE Assignment 7b: Obtain the list of all games from the backend!
            // TODO Assignment 7c/7e: And at some later point, this should only
            //      return the games open for registration (not started yet).
            List<Game> games = restClient.get().uri("/game").retrieve().body(new ParameterizedTypeReference<>() {});
            onlineState.setOpenGames(games);
        } catch (Exception e) {
            onlineState.setOpenGames(null);
        }
    }

    private boolean gameSelectionOn = false;

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

    public void gameSelected(Game game) {
        if (!appController.isGameRunning() /* && onlineState.getSignedInUser() != null && gameSelectionOn */) {
            appController.roboRally.createGameSelectionView(null);
            gameSelectionOn = false;

            Game result = null;
            if (game != null) {

                // TODO Assignment 7e: make sure the game is set to the active state
                //      here and in the backend, so that no new players can sign up.

                // Then show the game board and the game (with uid from backend) is then started
                startGame(result);
            }
        }
    }

    public void createGame(Game game) {
        if (!appController.isGameRunning() && onlineState.getSignedInUser() != null && gameSelectionOn) {


            try {
                // DONE Assignment 7b: Create the game (in the backend) with the config information
                //      provided in the game configuration
                // DONE Assignment 7c: Extend the game creation so that the currently signed in user
                //      is the owner of the game, which should also be registered as the first
                //      player of the game
                game.setOwner(onlineState.getSignedInUser());
                restClient.post().uri("/game").body(game).retrieve().body(Game.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // update the game select view (which should get the new game from the backend)
            selectGame();
        }
    }

    public void createGame() {
        appDialogs.createNewGame();
    }

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
                // 1. Just create the player
                Player newPlayer = new Player();
                newPlayer.setGame(game);
                newPlayer.setUser(activeUser);
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

    public void leaveGame(Game game) {
        try {
            // TODO Assignment 7d: delete the currently active user as a player
            //      for the given game (in the backend)

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

    public void deleteGame(Game game) {
        try {

            // TODO Assignment 7d: delete the given game from the games
            //      in the backend

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

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

    private void startGame(Game game) {
        // TODO Assignment 7e: creation of the board should eventually depend
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

    // TODO still somethings to do here for the real game play.
    //      - where to save the game controller (here we just forget it); it should probably be
    //        part of the online state
    //      - who is owner (controlling game logic) and communicating that to the backend
    //      - coordinating with the backend and updating the game state accordingly
    //      - sending users choices to the backend
    //      - not showing the hidden data (hand cards and not ye played program cards)
    //        for the other players in view
    //      - ...
    //      But this is not part of the course 02324 and its assignment. This assignment is just
    //      about creating a game and different users joining it (coordinated by the backend).

}
