import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;

public class JsonRpcServletEngine {
	public static final int PORT = 54321;
	Server server;

	public void startup() throws Exception {
		server = new Server(PORT);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(JsonRpcServlet.class, "/servlet");
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}
	
	public static void main(String args[]) {
		JsonRpcServletEngine engine = new JsonRpcServletEngine();
		try {
			engine.startup();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
