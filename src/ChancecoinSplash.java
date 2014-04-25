
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

//add -splash:static/images/splash_background.png to run configuration arguments
public class ChancecoinSplash extends Application implements ActionListener {
	private Scene scene;
	final SplashScreen splash = SplashScreen.getSplashScreen();
	Graphics2D graphics;
	@Override 
	public void start(Stage stage) {
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
		ChancecoinSplash chancecoinSplash = new ChancecoinSplash();
		chancecoinSplash.initiateSplash();
		chancecoinSplash.renderSplashFrame("Synchronizing with Blockchain...");
		chancecoinSplash.splash.update();
		 
		Thread thread = new Thread(Blocks.getInstance());
		thread.setDaemon(true);
		thread.start();
		
//		while (blocksRunnable.blocks.getInstance() == null) {
//			try {
//				System.out.println("sleep loop");
//                Thread.sleep(90);
//            } catch(InterruptedException e) {
//            	
//            }
//		}
//		System.out.println("blocks not null");
//        while (!blocksRunnable.blocks.getInstance().status.equals("Blockchain synchronization complete. Launching Chancecoin.")) {
//        	System.out.println("loop");
//        	chancecoinSplash.renderSplashFrame(blocksRunnable.blocks.getInstance().status);
//            chancecoinSplash.splash.update();
//            try {
//                Thread.sleep(90);
//            } catch(InterruptedException e) {
//            	
//            }
//        }
//        System.out.println("block status " + blocksRunnable.blocks.getInstance().status);
        chancecoinSplash.splash.close();
		launch(args);
	}
	
    public void renderSplashFrame(String status) {
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(120,140,200,40);
        graphics.setPaintMode();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.setFont(new Font("Arial", Font.PLAIN, 15));
        graphics.drawString(status, 120, 250);
    }
    
    public void initiateSplash() {
    	
        if (splash == null) {
            System.out.println("SplashScreen.getSplashScreen() returned null");
            return;
        }
        graphics = splash.createGraphics();
        if (graphics == null) {
            System.out.println("g is null");
            return;
        }

    }
    public void actionPerformed(ActionEvent ae) {
        System.exit(0);
    }
    
    private static WindowListener closeWindow = new WindowAdapter(){
        public void windowClosing(WindowEvent e){
            e.getWindow().dispose();
        }
    };
    
}

class ChancecoinBrowser extends Region {

	final WebView browser = new WebView();
	final Button buttonHome = createHomeButton();
	final WebEngine webEngine = browser.getEngine();
	final String address = "http://0.0.0.0:8080/";

	public ChancecoinBrowser() {
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
