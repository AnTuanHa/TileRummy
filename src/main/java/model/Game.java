package model;

import ai.AIPlayer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.observable.ObservableMeld;
import model.observable.ObservableTile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class Game {
	private static final Logger logger = LogManager.getLogger(Game.class.getName());

	private Stock stock;
	private Hand player1Hand;
	private Hand player2Hand;
	private Hand player3Hand;
	private Hand player4Hand;
	private ObservableList<ObservableMeld> table;

	private BooleanBinding isNPCTurn;
	private IntegerProperty playerTurn;
	private IntegerProperty winner;
	private AIPlayer ai1;
	private AIPlayer ai2;
	private AIPlayer ai3;
	private int numPlayers;

	// Note: you must call setStock since we do not do it automatically here
	// This is because we need to be able to set up the game for integration testing
	public Game() {
		// If no num of players are passed in, we default to 4 players
		this(4);
	}

	public Game(int numPlayers) {
		table = FXCollections.observableArrayList();
		winner = new SimpleIntegerProperty(-1);
		player1Hand = new Hand();
		player2Hand = new Hand();
		player3Hand = new Hand();
		player4Hand = new Hand();
		this.numPlayers = numPlayers;

		playerTurn = new SimpleIntegerProperty(0);
		isNPCTurn = Bindings.createBooleanBinding(() -> playerTurn.getValue() >= 1, playerTurn);

		ai1 = new AIPlayer(1, this, player2Hand);
		ai2 = new AIPlayer(2, this, player3Hand);
		ai3 = new AIPlayer(3, this, player4Hand);

		player1Hand.getSizeProperty().addListener((observableValue, oldVal, newVal) -> {
			if (newVal.intValue() == 0 && allMeldsValid()) {
				logger.info("Player 1 is the winner!");
				winner.setValue(1);
			}
		});
		player2Hand.getSizeProperty().addListener((observableValue, oldVal, newVal) -> {
			if (newVal.intValue() == 0 && allMeldsValid()) {
				logger.info("Player 2 is the winner!");
				winner.setValue(2);
			}
		});
		player3Hand.getSizeProperty().addListener((observableValue, oldVal, newVal) -> {
			if (newVal.intValue() == 0 && allMeldsValid()) {
				logger.info("Player 3 is the winner!");
				winner.setValue(3);
			}
		});
		player4Hand.getSizeProperty().addListener((observableValue, oldVal, newVal) -> {
			if (newVal.intValue() == 0 && allMeldsValid()) {
				logger.info("Player 4 is the winner!");
				winner.setValue(4);
			}
		});
	}

	public void setStock(Stock stock) {
		this.stock = stock;
	}

	public void resetGame() {
		table.clear();
		winner.setValue(-1);
		playerTurn.setValue(0);
		player1Hand.clear();
		player2Hand.clear();
		player3Hand.clear();
		player4Hand.clear();
	}

	public void dealInitialTiles() {
		for (int i = 0; i < 14; i++) {
			drawTile().ifPresent(t -> player1Hand.addTile(t));
		}
		for (int i = 0; i < 14; i++) {
			drawTile().ifPresent(t -> player2Hand.addTile(t));
		}
		if (numPlayers >= 3) {
			for (int i = 0; i < 14; i++) {
				drawTile().ifPresent(t -> player3Hand.addTile(t));
			}
		}
		if (numPlayers >= 4) {
			for (int i = 0; i < 14; i++) {
				drawTile().ifPresent(t -> player4Hand.addTile(t));
			}
		}
	}

	public IntegerProperty getWinnerProperty() {
		return winner;
	}

	public int getNumPlayers() {
		return numPlayers;
	}

	public Optional<ObservableTile> drawTile() {
		return stock.draw();
	}

	public Hand getCurrentPlayerhand() {
		switch (getPlayerTurn()) {
			case 0:
				return player1Hand;
			case 1:
				return player2Hand;
			case 2:
				return player3Hand;
			case 3:
				return player4Hand;
		}
		return null;
	}

	public Hand getPlayer1Hand() {
		return player1Hand;
	}

	public Hand getPlayer2Hand() {
		return player2Hand;
	}

	public Hand getPlayer3Hand() {
		return player3Hand;
	}

	public Hand getPlayer4Hand() {
		return player4Hand;
	}

	public int getStockSize() {
		return stock.getSize();
	}

	public AIPlayer getAI1() {
		return ai1;
	}

	public AIPlayer getAI2() {
		return ai2;
	}

	public AIPlayer getAI3() {
		return ai3;
	}

	public void endTurn(Hand hand) {
		if (!allMeldsValid()) {
			return;
		}

		if (noTilesAddedThisTurn()) {
			drawTile().ifPresent(hand::addTile);
		}
		playAllTiles();
		playerTurn.setValue((playerTurn.getValue() + 1) % numPlayers);
	}

	private boolean allMeldsValid() {
		for (Meld meld : table) {
			if (!meld.isValidLength()) {
				return false;
			}
		}
		return true;
	}

	private boolean noTilesAddedThisTurn() {
		for (Meld meld : table) {
			for (ObservableTile tile : meld.getMeld()) {
				//if a tile is unplayed, that means tiles have been added to the table this turn
				if (!tile.hasBeenPlayed()) {
					return false;
				}
			}
		}
		return true;
	}

	private void playAllTiles() {
		table.forEach(meld -> meld.getMeld().forEach(ObservableTile::play));
	}

	public BooleanBinding getNPCTurn() {
		return isNPCTurn;
	}

	public int getPlayerTurn() {
		return playerTurn.getValue();
	}

	public IntegerProperty getPlayerTurnProperty() {
		return playerTurn;
	}

	public ObservableList<ObservableMeld> getTable() {
		return table;
	}

	public boolean removeTileFromTable(ObservableTile tile, int row, int col) {
		for (ObservableMeld meld : table) {
			// Assume that there exists at least one tile in the Meld, as we do not allow empty melds to exist on the table
			ObservableTile firstTile = meld.getMeld().get(0);
			ObservableTile lastTile = meld.getMeld().get(meld.getSize() - 1);

			// In the same row
			if (row == meld.getRow()) {
				// Either at the beginning or end of list
				if (col == meld.getCol()) {
					if (firstTile.getRank() == tile.getRank() && firstTile.getColour().equals(tile.getColour())) {
						meld.removeFirstTile();
						// If we remove the beginning of the meld, the old starting column is out of date, so we update it
						meld.setCol(meld.getCol() + 1);
						// Remove empty melds from table
						if (meld.getSize() <= 0) {
							table.remove(meld);
						}
						return true;
					}
				} else if (col == meld.getCol() + meld.getSize() - 1) {
					if (lastTile.getRank() == tile.getRank() && lastTile.getColour().equals(tile.getColour())) {
						meld.removeLastTile();
						// Remove empty melds from table
						if (meld.getSize() <= 0) {
							table.remove(meld);
						}
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean meldExistsOnTheLeftAndRightOfCell(int row, int col) {
		boolean meldOnLeft = false;
		boolean meldOnRight = false;
		for (ObservableMeld meld : table) {
			if (meld.getRow() == row) {
				// A meld exists on the left of the cell
				if (col - 1 == meld.getCol() + meld.getSize() - 1) {
					meldOnLeft = true;
				}
				// A meld exists on the right of the cell
				if (col + 1 == meld.getCol()) {
					meldOnRight = true;
				}
			}
		}
		return meldOnLeft && meldOnRight;
	}

	public boolean addTileToTable(ObservableTile tile, int row, int col) {
		boolean success = false;

		if (meldExistsOnTheLeftAndRightOfCell(row, col)) {
			return false;
		}

		// Try to find existing meld to add tile to
		for (ObservableMeld meld : table) {
			// within bounds
			if (row == meld.getRow() && col >= meld.getCol() - 1 && col <= meld.getCol() + meld.getSize()) {
				// Only allow adding to tail ends
				if (col == meld.getCol() - 1) {
					success = meld.addFirstTile(tile);
					if (success) {
						// If we add to the beginning of the meld, the old starting column is out of date, so we update it
						meld.setCol(meld.getCol() - 1);
					}
				} else if (col == meld.getCol() + meld.getSize()) {
					success = meld.addLastTile(tile);
				}
				return success;
			}
		}

		// Otherwise, create new meld
		ObservableMeld meld = new ObservableMeld(row, col);
		if (meld.addFirstTile(tile)) {
			table.add(meld);
			return true;
		}
		return false;
	}
}
