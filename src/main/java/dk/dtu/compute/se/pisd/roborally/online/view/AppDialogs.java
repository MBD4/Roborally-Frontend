package dk.dtu.compute.se.pisd.roborally.online.view;

import dk.dtu.compute.se.pisd.roborally.online.controller.OnlineController;
import dk.dtu.compute.se.pisd.roborally.online.model.Game;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AppDialogs {

    private OnlineController onlineController;

    public AppDialogs(OnlineController onlineController) {
        this.onlineController = onlineController;
    }

    public void signIn() {
        Stage stage = new Stage();

        Text text = new Text("Register as user for Online RoboRally with a (new) user name.");
        TextField userName = new TextField();

        Button cancel = new Button("Cancel");
        cancel.setOnAction( e -> stage.close() );
        Button register= new Button("Sign in");
        register.setOnAction(
                e -> {
                    String name = userName.getText();
                    if (name.length() >= 4) {
                        stage.close();
                        onlineController.signIn(name);
                    }
                }
        );
        HBox buttons = new HBox(cancel, register);

        VBox vbox = new VBox(text, userName, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Register for Online RoboRally");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    public void signUp() {
        Stage stage = new Stage();

        Text text = new Text("Register a NEW user for Online RoboRally.");
        TextField userName = new TextField();

        Button cancel = new Button("Cancel");
        cancel.setOnAction( e -> stage.close() );
        Button register= new Button("Sign up");

        register.setOnAction(
                e -> {
                    String name = userName.getText();
                    if (name.length() >= 4) {
                        stage.close();
                        // This calls the method we drafted in the previous step!
                        onlineController.signUp(name);
                    }
                }
        );
        HBox buttons = new HBox(cancel, register);
        VBox vbox = new VBox(text, userName, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Sign Up for Online RoboRally");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    public void createNewGame() {
        Stage stage = new Stage();

        Text text = new Text("Add the needed data for your a new game.");

        Label labelGameName = new Label("Name:");
        TextField gameName = new TextField();
        HBox nameBox = new HBox(labelGameName,gameName);

        Label minLabel = new Label("min:");
        TextField min = new TextField();
        Label maxLabel = new Label("max:");
        TextField max = new TextField();
        HBox minMaxBox = new HBox(minLabel,min,maxLabel,max);


        Button cancel = new Button("Cancel");
        cancel.setOnAction( e -> stage.close() );
        Button create= new Button("Create");
        create.setOnAction(
                e -> {
                    try {
                        Game game = new Game();
                        game.setName(gameName.getText());
                        int minPlayers = Integer.parseInt(min.getText());
                        int maxPlayers = Integer.parseInt(max.getText());
                        if (minPlayers > maxPlayers) {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Invalid player limits");
                            alert.setHeaderText("Minimum players cannot exceed maximum players.");
                            alert.setContentText("Please enter a minimum that is less than or equal to the maximum.");
                            alert.showAndWait();
                            return;
                        }
                        game.setMinPlayers(minPlayers);
                        game.setMaxPlayers(maxPlayers);
                        onlineController.createGame(game);
                        stage.close();
                    } catch (NumberFormatException exception) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid input");
                        alert.setHeaderText("Player limits must be numbers.");
                        alert.setContentText("Please enter valid integer values for min and max players.");
                        alert.showAndWait();
                    } catch (Exception exception) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Could not create game");
                        alert.setHeaderText("Something went wrong while creating the game.");
                        alert.setContentText("Please try again.");
                        alert.showAndWait();
                    }
                }
        );
        HBox buttons = new HBox(cancel, create);

        VBox vbox = new VBox(text, nameBox, minMaxBox, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Create New Online Game");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

}
