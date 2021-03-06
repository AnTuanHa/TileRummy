package controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Game;
import model.OptionChoices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MainController {

	private static final Logger logger = LogManager.getLogger(MainController.class.getName());

	@FXML
	private BorderPane bpPlayScreen;

	@FXML
	private HBox hboxMenuBar;

	@FXML
	private HBox hboxCenter;

	@FXML
	private JFXButton btnEndTurn;

	@FXML
	private StackPane root;

	private Game game;
	private OptionChoices options;

	private HandController player1HandController;
	private HandController player2HandController;
	private HandController player3HandController;
	private HandController player4HandController;

	public MainController(OptionChoices options) {
		this.options = options;
		game = new Game(options);
	}

	@FXML
	public void initialize() {
		logger.info("Initializing Main view");

		GridPane tableView;
		VBox player1HandView;
		VBox player2HandView;
		VBox player3HandView = null;
		VBox player4HandView = null;

		FXMLLoader loader;

		try {
			loader = new FXMLLoader(getClass().getClassLoader().getResource("view/TableView.fxml"));
			loader.setControllerFactory(c -> new TableController(game));
			tableView = loader.load();

			if (options.getPlayer1() == OptionChoices.Type.HUMAN) {
				loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
				player1HandController = new HumanHandController(game, 1, options.getTimerChecked(), options.getShowHandsChecked());
				loader.setControllerFactory(c -> player1HandController);
				player1HandView = loader.load();
			} else {
				loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
				player1HandController = new NPCHandController(game, 1, options.getShowHandsChecked());
				loader.setControllerFactory(c -> player1HandController);
				player1HandView = loader.load();
			}

			if (options.getPlayer2() == OptionChoices.Type.HUMAN) {
				loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
				player2HandController = new HumanHandController(game, 2, options.getTimerChecked(), options.getShowHandsChecked());
				loader.setControllerFactory(c -> player2HandController);
				player2HandView = loader.load();
			} else {
				loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
				player2HandController = new NPCHandController(game, 2, options.getShowHandsChecked());
				loader.setControllerFactory(c -> player2HandController);
				player2HandView = loader.load();
			}

			if (options.getNumPlayers() >= 3) {
				if (options.getPlayer3() == OptionChoices.Type.HUMAN) {
					loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
					player3HandController = new HumanHandController(game, 3, options.getTimerChecked(), options.getShowHandsChecked());
					loader.setControllerFactory(c -> player3HandController);
					player3HandView = loader.load();
				} else {
					loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
					player3HandController = new NPCHandController(game, 3, options.getShowHandsChecked());
					loader.setControllerFactory(c -> player3HandController);
					player3HandView = loader.load();
				}
			}

			if (options.getNumPlayers() >= 4) {
				if (options.getPlayer4() == OptionChoices.Type.HUMAN) {
					loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
					player4HandController = new HumanHandController(game, 4, options.getTimerChecked(), options.getShowHandsChecked());
					loader.setControllerFactory(c -> player4HandController);
					player4HandView = loader.load();
				} else {
					loader = new FXMLLoader(getClass().getClassLoader().getResource("view/HandView.fxml"));
					player4HandController = new NPCHandController(game, 4, options.getShowHandsChecked());
					loader.setControllerFactory(c -> player4HandController);
					player4HandView = loader.load();
				}
			}

		} catch (IOException e) {
			logger.error("Failed to load fxml files", e);
			return;
		}

		HBox.setHgrow(tableView, Priority.ALWAYS);
		bpPlayScreen.setBottom(player1HandView);
		bpPlayScreen.setLeft(player2HandView);
		if (options.getNumPlayers() >= 3) {
			bpPlayScreen.setTop(player3HandView);
		}
		if (options.getNumPlayers() >= 4) {
			bpPlayScreen.setRight(player4HandView);
		}
		hboxCenter.getChildren().add(0, tableView);

		btnEndTurn.setOnMouseClicked(b -> game.endTurn(game.getCurrentPlayerHand()));
		btnEndTurn.disableProperty().bind(game.getNPCTurn().or(game.getWinnerProperty().isNotEqualTo(-1)));

		game.getWinnerProperty().addListener((observableValue, oldVal, newVal) -> {
			if (newVal.intValue() != -1) {
				FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("view/WinnerDialogContent.fxml"));
				try {
					HBox content = fxmlLoader.load();
					Label lblWinner = content.getChildren().stream()
							.filter(Label.class::isInstance)
							.map(Label.class::cast)
							.filter(l -> l.getId().equals("lblWinner"))
							.findFirst()
							.orElseThrow();
					lblWinner.setText("Player " + newVal + " is the winner!");
					JFXDialog dialog = new JFXDialog(root, content, JFXDialog.DialogTransition.CENTER);
					dialog.show();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		});

		if (options.getShowStartDialog()) {
			loadPlayerFirstDialog();
		}

		AnimationTimer gameLoop = new AnimationTimer() {
			@Override
			public void handle(long l) {
				game.update();
			}
		};
		gameLoop.start();
	}

	public Game getGame() {
		return game;
	}

	public Pane getPlayer1HandPane() {
		return player1HandController.getHandPane();
	}

	public Pane getPlayer2HandPane() {
		return player2HandController.getHandPane();
	}

	public Pane getPlayer3HandPane() {
		return player3HandController.getHandPane();
	}

	public Pane getPlayer4HandPane() {
		return player4HandController.getHandPane();
	}

	private void loadPlayerFirstDialog() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("view/WinnerDialogContent.fxml"));
		try {
			HBox content = fxmlLoader.load();
			Label lblWinner = content.getChildren().stream()
					.filter(Label.class::isInstance)
					.map(Label.class::cast)
					.filter(l -> l.getId().equals("lblWinner"))
					.findFirst()
					.orElseThrow();

			String playerText = "";
			for (int i=0; i<options.getNumPlayers(); i++) {
				playerText += "Player " + (i+1) + " drew: " + game.getDrawnStartTile(i) + "\n";
			}
			lblWinner.setText(playerText + "Player " + (game.getPlayerTurn()+1) + " gets to go first!");
			JFXDialog dialog = new JFXDialog(root, content, JFXDialog.DialogTransition.CENTER);
			dialog.show();
		} catch (IOException e) {
			logger.error(e);
		}
	}
}
