import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


public class GUI extends Application {
	private Scene scene;
	@Override public void start(Stage stage) {
		// start Blocks
		//Blocks blocks = Blocks.getInstance();

		// start Server
		Server server = new Server();
		Thread serverThread = new Thread(server);
		serverThread.setDaemon(true); // important, otherwise JVM does not exit at end of main()
		serverThread.start(); 

		// create the scene
		stage.setTitle("Chancecoin");
		stage.getIcons().add(new Image("file:./static/images/logo.png"));
		scene = new Scene(new Browser(),1000,690, Color.web("#EEEEEE"));
		stage.setResizable(false);
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args){
		launch(args);
	}
}
class Browser extends Region {

	final WebView browser = new WebView();
	final Button buttonHome = createHomeButton();
	final WebEngine webEngine = browser.getEngine();
	final String address = "http://0.0.0.0:8080/";

	public Browser() {
		//getStyleClass().add("browser");
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

/*
import java.io.IOException;

public class GUI {
    public static void main(String[] args){
    	// start Blocks
		//Blocks blocks = Blocks.getInstance();

		// start Server
		Server server = Server.getInstance();

		try {
			java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://0.0.0.0:8080/"));
		} catch (Exception e) {
		}
    }
}
 */