import java.util.Locale;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class GUI extends Application {
	private Pane splashLayout;
	private ProgressBar loadProgress;
	private Label progressText;
	private Stage mainStage;
	private static final int SPLASH_WIDTH = 200;
	private static final int SPLASH_HEIGHT = 200;

	public static void main(String[] args) throws Exception { launch(args); }

	@Override public void init() {
		Locale.setDefault(new Locale("en", "US"));
		ImageView splash = new ImageView(new Image("file:./resources/static/images/logo.png"));
		loadProgress = new ProgressBar();
		loadProgress.setPrefWidth(SPLASH_WIDTH);
		progressText = new Label("");
		splashLayout = new VBox();
		splashLayout.getChildren().addAll(splash, loadProgress, progressText);
		progressText.setAlignment(Pos.CENTER);
		splashLayout.setStyle("-fx-padding: 5; -fx-background-color: linear-gradient(to bottom, #ffffff, #ffffff); -fx-border-width:1; -fx-border-color: black;");
		splashLayout.setEffect(new DropShadow());
	}

	@Override public void start(final Stage initStage) throws Exception {
		final Task preloaderTask = new Task() {
			@Override protected Object call() throws InterruptedException {
				updateMessage("Loading Chancecoin");
				
				/*
				// start Blocks
				final Blocks blocks = Blocks.getInstanceFresh();
				
				Thread progressUpdateThread = new Thread(blocks) { 
					public void run() {
						while(blocks.chancecoinBlock == 0 || blocks.working || blocks.parsing) {
							if (blocks.chancecoinBlock > 0) {
								updateMessage("Block " + blocks.chancecoinBlock + "/" + blocks.bitcoinBlock);
							} else {
								updateMessage(blocks.statusMessage);		
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
					}
				};
				progressUpdateThread.start();
				blocks.init();
				blocks.versionCheck();
				blocks.follow();
				*/
				
				// start Server
				Server server = new Server();
				Thread serverThread = new Thread(server);
				serverThread.setDaemon(true);
				serverThread.start(); 
				
				Thread.sleep(8000);
				
				return null;
			}
		};

		showSplash(initStage, preloaderTask);
		new Thread(preloaderTask).start();
		showMainStage();
	}

	private void showMainStage() {
		// create the scene
		mainStage = new Stage(StageStyle.DECORATED);
		mainStage.setTitle("Chancecoin");
		mainStage.getIcons().add(new Image("file:./resources/static/images/logo.png"));
		mainStage.setIconified(true);
		Scene scene = new Scene(new Browser(),1000,690, Color.web("#EEEEEE"));
		mainStage.setResizable(false);
		mainStage.setScene(scene);
		mainStage.show();

		mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				System.exit(0);
			}
		});
	}

	private void showSplash(final Stage initStage, Task task) {
		progressText.textProperty().bind(task.messageProperty());
		loadProgress.progressProperty().bind(task.progressProperty());
		task.stateProperty().addListener(new ChangeListener<Worker.State>() {
			@Override public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
				if (newState == Worker.State.SUCCEEDED) {
					loadProgress.progressProperty().unbind();
					loadProgress.setProgress(1);
					mainStage.setIconified(false);
					initStage.toFront();
					FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.2), splashLayout);
					fadeSplash.setFromValue(1.0);
					fadeSplash.setToValue(0.0);
					fadeSplash.setOnFinished(new EventHandler<ActionEvent>() {
						@Override public void handle(ActionEvent actionEvent) {
							initStage.hide();
						}
					});
					fadeSplash.play();
				} 
			}
		});

		Scene splashScene = new Scene(splashLayout);
		initStage.initStyle(StageStyle.UNDECORATED);
		final Rectangle2D bounds = Screen.getPrimary().getBounds();
		initStage.setScene(splashScene);
		initStage.setX(bounds.getMinX() + bounds.getWidth() / 2 - SPLASH_WIDTH / 2);
		initStage.setY(bounds.getMinY() + bounds.getHeight() / 2 - SPLASH_HEIGHT / 2);
		initStage.show();
	}
}

class Browser extends Region {
	final WebView browser = new WebView();
	final Button buttonHome = createHomeButton();
	final WebEngine webEngine = browser.getEngine();
	final String address = "http://0.0.0.0:8080/";

	public Browser() {
		webEngine.load(address);

		VBox vbox1 = new VBox(0);
		vbox1.getChildren().add(browser);
		vbox1.getChildren().add(buttonHome);
		browser.setPrefSize(1000, 654);
		vbox1.setAlignment(Pos.TOP_CENTER);
		getChildren().add(vbox1);

	}
	private Button createHomeButton() {
		Button buttonHome = new Button("Chancecoin wallet");    

		buttonHome.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				webEngine.load(address);
			}
		});
		return buttonHome;
	}
}