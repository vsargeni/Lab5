package pkgPoker.app.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;

import netgame.common.Hub;
import pkgPokerBLL.Action;
import pkgPokerBLL.CardDraw;
import pkgPokerBLL.Deck;
import pkgPokerBLL.GamePlay;
import pkgPokerBLL.Player;
import pkgPokerBLL.Rule;
import pkgPokerBLL.Table;
import pkgPokerEnum.eCardDestination;
import pkgPokerEnum.eDrawCount;

public class PokerHub extends Hub {

	private Table HubPokerTable = new Table();
	private GamePlay HubGamePlay;
	private int iDealNbr = 0;

	public PokerHub(int port) throws IOException {
		super(port);
	}

	protected void playerConnected(int playerID) {

		if (playerID == 2) {
			shutdownServerSocket();
		}
	}

	protected void playerDisconnected(int playerID) {
		shutDownHub();
	}

	protected void messageReceived(int ClientID, Object message) {

		if (message instanceof Action) {
			Player actPlayer = (Player) ((Action) message).getPlayer();
			Action act = (Action) message;
			switch (act.getAction()) {
			case Sit:
				HubPokerTable.AddPlayerToTable(actPlayer);
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case Leave:
				HubPokerTable.RemovePlayerFromTable(actPlayer);
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case TableState:
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case StartGame:

				Rule rle = new Rule(act.geteGame());

				if (HubGamePlay == null) {
					HubGamePlay = new GamePlay(rle, actPlayer.getPlayerID());

				}

				HubGamePlay.setGameDeck(new Deck(rle.GetNumberOfJokers(), rle.GetWildCards()));

				ArrayList<Player> players = new ArrayList<Player>(HubPokerTable.getHmPlayer().values());

				HashMap<UUID, Player> playersMap = new HashMap<UUID, Player>();

				for (Player p : players) {
					playersMap.put(p.getPlayerID(), p);
				}

				HubGamePlay.setGamePlayers(playersMap);

				for (Player p : players) {
					HubGamePlay.addPlayerToGame(p);

					HubGamePlay.setiActOrder(HubGamePlay.GetOrder(p.getiPlayerPosition()));
				}

			case Draw:

				HubGamePlay.getRule().GetDrawCard(eDrawCount.geteDrawCount(iDealNbr));

				TreeMap<Integer, CardDraw> tree = HubGamePlay.getRule().getHmCardDraw();
				

				CardDraw cd = tree.get( (Integer) this.iDealNbr);

				eCardDestination destin = cd.getCardDestination();

				if (destin == eCardDestination.Player) {
					players = (ArrayList) HubGamePlay.getGamePlayers().values();

					for (Player p : players) {
						for (int i = 0; i <= cd.getCardCount().ordinal(); i++) {
							HubGamePlay.getPlayerHand(p).AddCardToHand(HubGamePlay.getGameDeck().Draw());
						}

					}
				} else if (destin == eCardDestination.Community) {
					for (int i = 0; i <= cd.getCardCount().ordinal(); i++) {
						HubGamePlay.getGameCommonHand().AddCardToHand(HubGamePlay.getGameDeck().Draw());
					}
				}

				this.iDealNbr++;

				HubGamePlay.isGameOver();

				resetOutput();
				sendToAll(HubGamePlay);
				break;
			case ScoreGame:

				resetOutput();
				sendToAll(HubGamePlay);
				break;
			}

		}

	}

}